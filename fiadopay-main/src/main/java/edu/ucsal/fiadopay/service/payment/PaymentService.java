package edu.ucsal.fiadopay.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsal.fiadopay.domain.Merchant;
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
import edu.ucsal.fiadopay.service.webhook.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class PaymentService {

  private final AuthService authService;
  private final PaymentRepository payments;
  private final WebhookDeliveryService webhookDeliveryService;
  private final PaymentMapper paymentMapper;

  @Value("${fiadopay.processing-delay-ms}") long delay;
  @Value("${fiadopay.failure-rate}") double failRate;

  @Transactional
  public PaymentResponse createPayment(String auth, String idemKey, PaymentRequest req){
    var merchant = authService.merchantFromAuth(auth);
    var mid = merchant.getId();

    if (idemKey != null) {
      var existing = payments.findByIdempotencyKeyAndMerchantId(idemKey, mid);
      if(existing.isPresent()) return paymentMapper.toResponse(existing.get());
    }

    Double interest = null;
    BigDecimal total = req.amount();
    if ("CARD".equalsIgnoreCase(req.method()) && req.installments()!=null && req.installments()>1){
      interest = 1.0; // 1%/mês
      var base = new BigDecimal("1.01");
      var factor = base.pow(req.installments());
      total = req.amount().multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    var payment = Payment.builder()
        .id("pay_"+UUID.randomUUID().toString().substring(0,8))
        .merchantId(mid)
        .method(req.method().toUpperCase())
        .amount(req.amount())
        .currency(req.currency())
        .installments(req.installments()==null?1:req.installments())
        .monthlyInterest(interest)
        .totalWithInterest(total)
        .status(Payment.Status.PENDING)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .idempotencyKey(idemKey)
        .metadataOrderId(req.metadataOrderId())
        .build();

    payments.save(payment);

    CompletableFuture.runAsync(() -> processAndWebhook(payment.getId(), merchant));

    return paymentMapper.toResponse(payment);
  }

  public PaymentResponse getPayment(String id){
    return paymentMapper.toResponse(payments.findById(id)
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
    webhookDeliveryService.deliver(merchant, p);
    return Map.of("id","ref_"+UUID.randomUUID(),"status","PENDING");
  }

  private void processAndWebhook(String paymentId, Merchant merchant){
    try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
    var p = payments.findById(paymentId).orElse(null);
    if (p==null || merchant==null) return;

    var approved = Math.random() > failRate;
    p.setStatus(approved ? Payment.Status.APPROVED : Payment.Status.DECLINED);
    p.setUpdatedAt(Instant.now());
    payments.save(p);

    webhookDeliveryService.deliver(merchant, p);
  }

}
