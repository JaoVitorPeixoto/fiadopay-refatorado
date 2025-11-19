package edu.ucsal.fiadopay.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsal.fiadopay.dto.request.PaymentRequest;
import edu.ucsal.fiadopay.dto.response.PaymentResponse;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.domain.WebhookDelivery;
import edu.ucsal.fiadopay.mapper.PaymentMapper;
import edu.ucsal.fiadopay.repo.MerchantRepository;
import edu.ucsal.fiadopay.repo.PaymentRepository;
import edu.ucsal.fiadopay.repo.WebhookDeliveryRepository;
import edu.ucsal.fiadopay.service.auth.AuthService;
import edu.ucsal.fiadopay.service.signature.SignatureService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentService {

  private final AuthService authService;
  private final PaymentCreator creator;
  private final PaymentProcessor processor;
  private final WebhookDispatcher dispatcher;
  private final PaymentMapper mapper;

  @Value("${fiadopay.webhook-secret}") String secret;
  @Value("${fiadopay.processing-delay-ms}") long delay;
  @Value("${fiadopay.failure-rate}") double failRate;

  public PaymentService(AuthService authService, PaymentCreator creator,
          PaymentProcessor processor, WebhookDispatcher dispatcher,
          PaymentMapper mapper) {
    this.authService = authService;
    this.creator = creator;
    this.processor = processor;
    this.dispatcher = dispatcher;
    this.mapper = mapper;
  }

  @Transactional
  public PaymentResponse createPayment(String auth, String idemKey, PaymentRequest req) {

      var merchant = authService.merchantFromAuth(auth);
      var payment = creator.create(idemKey, merchant.getId(), req);

      CompletableFuture.runAsync(() -> {
          var processed = processor.process(payment.getId());
          if (processed != null)
              dispatcher.dispatch(processed);
      });

      return mapper.toResponse(payment);
  }

  public PaymentResponse getPayment(String id) {
      return mapper.toResponse(creator.getPayments().findById(id)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }
  
  public Map<String,Object> refund(String auth, String paymentId){
    var merchant = authService.merchantFromAuth(auth);
    var p = payments.findById(paymentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!merchant.getId().equals(p.getMerchantId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    p.setStatus(Payment.Status.REFUNDED);
    p.setUpdatedAt(Instant.now());
    payments.save(p);
    sendWebhook(p);
    return Map.of("id","ref_"+UUID.randomUUID(),"status","PENDING");
  }

  private void processAndWebhook(String paymentId){
    try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
    var p = payments.findById(paymentId).orElse(null);
    if (p==null) return;

    var approved = Math.random() > failRate;
    p.setStatus(approved ? Payment.Status.APPROVED : Payment.Status.DECLINED);
    p.setUpdatedAt(Instant.now());
    payments.save(p);

    sendWebhook(p);
  }

  private void sendWebhook(Payment p){
    var merchant = merchants.findById(p.getMerchantId()).orElse(null);
    if (merchant==null || merchant.getWebhookUrl()==null || merchant.getWebhookUrl().isBlank()) return;

    String payload;
    try {
      var data = Map.of(
          "paymentId", p.getId(),
          "status", p.getStatus().name(),
          "occurredAt", Instant.now().toString()
      );
      var event = Map.of(
          "id", "evt_"+UUID.randomUUID().toString().substring(0,8),
          "type", "payment.updated",
          "data", data
      );
      payload = objectMapper.writeValueAsString(event);
    } catch (Exception e) {
      // fallback mínimo: não envia webhook se falhar a serialização
      return;
    }

    var signature = signatureService.sign(payload, secret);

    var delivery = deliveries.save(WebhookDelivery.builder()
        .eventId("evt_"+UUID.randomUUID().toString().substring(0,8))
        .eventType("payment.updated")
        .paymentId(p.getId())
        .targetUrl(merchant.getWebhookUrl())
        .signature(signature)
        .payload(payload)
        .attempts(0)
        .delivered(false)
        .lastAttemptAt(null)
        .build());

    CompletableFuture.runAsync(() -> tryDeliver(delivery.getId()));
  }

  private void tryDeliver(Long deliveryId){
    var d = deliveries.findById(deliveryId).orElse(null);
    if (d==null) return;
    try {
      var client = HttpClient.newHttpClient();
      var req = HttpRequest.newBuilder(URI.create(d.getTargetUrl()))
        .header("Content-Type","application/json")
        .header("X-Event-Type", d.getEventType())
        .header("X-Signature", d.getSignature())
        .POST(HttpRequest.BodyPublishers.ofString(d.getPayload()))
        .build();
      var res = client.send(req, HttpResponse.BodyHandlers.ofString());
      d.setAttempts(d.getAttempts()+1);
      d.setLastAttemptAt(Instant.now());
      d.setDelivered(res.statusCode()>=200 && res.statusCode()<300);
      deliveries.save(d);
      if(!d.isDelivered() && d.getAttempts()<5){
        Thread.sleep(1000L * d.getAttempts());
        tryDeliver(deliveryId);
      }
    } catch (Exception e){
      d.setAttempts(d.getAttempts()+1);
      d.setLastAttemptAt(Instant.now());
      d.setDelivered(false);
      deliveries.save(d);
      if (d.getAttempts()<5){
        try {
          Thread.sleep(1000L * d.getAttempts());
        } catch (InterruptedException ignored) {}
        tryDeliver(deliveryId);
      }
    }
  }

}
