package edu.ucsal.fiadopay.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.domain.WebhookDelivery;
import edu.ucsal.fiadopay.repo.WebhookDeliveryRepository;
import edu.ucsal.fiadopay.service.signature.SignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class WebhookDeliveryService {

    private final WebhookDeliveryRepository webhookDeliveryRepository;
    @Qualifier("httpSender")
    private final WebhookSender webhookSender;
    @Qualifier("hmac")
    private final SignatureService signatureService;
    private final ObjectMapper objectMapper;

    @Value("${fiadopay.webhook.secret}") private String secret;
    @Value("${fiadopay.webhook.max-attempts:5}") private int maxAttempts;
    @Value("${fiadopay.webhook.retry-delay-ms:1000}") private long retryDelayMs;

    public void deliver(Merchant m, Payment p){

        if (m==null || m.getWebhookUrl()==null || m.getWebhookUrl().isBlank()) return;

        String payload = buildPayload(p);
        if (payload == null) return; // fallback mínimo: não envia webhook se falhar a serialização

        var signature = signatureService.sign(payload, secret);

        var delivery = webhookDeliveryRepository.save(WebhookDelivery.builder()
                .eventId("evt_"+UUID.randomUUID().toString().substring(0,8))
                .eventType("payment.updated")
                .paymentId(p.getId())
                .targetUrl(m.getWebhookUrl())
                .signature(signature)
                .payload(payload)
                .attempts(0)
                .delivered(false)
                .lastAttemptAt(null)
                .build());

        CompletableFuture.runAsync(() -> tryDeliver(delivery.getId()));
    }

    private String buildPayload(Payment p){
        try {
            var data = Map.of(
                    "paymentId", p.getId(),
                    "status", p.getStatus().name(),
                    "occurredAt", Instant.now().toString()
            );
            var event = Map.of(
                    "id", "evt_"+ UUID.randomUUID().toString().substring(0,8),
                    "type", "payment.updated",
                    "data", data
            );
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            return null;
        }
    }

    private void tryDeliver(Long deliveryId){
        var d = webhookDeliveryRepository.findById(deliveryId).orElse(null);
        if (d==null) return;

        try {
            boolean entregou = webhookSender.sendWebhook(d.getTargetUrl(), d.getSignature(), d.getPayload(), d.getEventType());

            d.setAttempts(d.getAttempts()+1);
            d.setLastAttemptAt(Instant.now());
            d.setDelivered(entregou);
            webhookDeliveryRepository.save(d);

            if(!d.isDelivered() && d.getAttempts() < maxAttempts){
                Thread.sleep(retryDelayMs * d.getAttempts());
                tryDeliver(deliveryId);
            }

        } catch (Exception e){
            d.setAttempts(d.getAttempts()+1);
            d.setLastAttemptAt(Instant.now());
            d.setDelivered(false);
            webhookDeliveryRepository.save(d);
            if (d.getAttempts() < maxAttempts){
                try {
                    Thread.sleep(retryDelayMs * d.getAttempts());
                } catch (InterruptedException ignored) {}
                tryDeliver(deliveryId);
            }
        }
    }

}
