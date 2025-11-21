package edu.ucsal.fiadopay.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsal.fiadopay.annotation.WebhookSink;
import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.domain.WebhookDelivery;
import edu.ucsal.fiadopay.repo.WebhookDeliveryRepository;
import edu.ucsal.fiadopay.service.signature.SignatureService;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WebhookDeliveryService {

    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final WebhookSender webhookSender;
    private final SignatureService signatureService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    @Value("${fiadopay.webhook.secret}")
    private String secret;
    
    @Value("${fiadopay.webhook.max-attempts:5}")
    private int maxAttempts;
    
    @Value("${fiadopay.webhook.retry-delay-ms:1000}")
    private long retryDelayMs;

    public WebhookDeliveryService(
            WebhookDeliveryRepository webhookDeliveryRepository,
            @Qualifier("httpSender") WebhookSender webhookSender,
            @Qualifier("hmac") SignatureService signatureService,
            ObjectMapper objectMapper) {
        
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.webhookSender = webhookSender;
        this.signatureService = signatureService;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newFixedThreadPool(10, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "webhook-worker-" + counter.incrementAndGet());
            }
        });
    }

    public void deliver(Merchant m, Payment p) {
        if (m == null || m.getWebhookUrl() == null || m.getWebhookUrl().isBlank()) return;

        String payload = buildPayload(p);
        if (payload == null) return;

        var signature = signatureService.sign(payload, secret);

        var delivery = webhookDeliveryRepository.save(WebhookDelivery.builder()
            .eventId("evt_" + UUID.randomUUID().toString().substring(0, 8))
            .eventType("payment.updated")
            .paymentId(p.getId())
            .targetUrl(m.getWebhookUrl())
            .signature(signature)
            .payload(payload)
            .attempts(0)
            .delivered(false)
            .lastAttemptAt(null)
            .build());

        executorService.submit(() -> tryDeliver(delivery.getId()));
    }

    @WebhookSink(description = "Executes the HTTP delivery attempt with retries")
    private void tryDeliver(Long deliveryId) {
        var d = webhookDeliveryRepository.findById(deliveryId).orElse(null);
        if (d == null) return;

        try {
            boolean delivered = webhookSender.sendWebhook(
                d.getTargetUrl(), 
                d.getSignature(), 
                d.getPayload(), 
                d.getEventType()
            );

            d.setAttempts(d.getAttempts() + 1);
            d.setLastAttemptAt(Instant.now());
            d.setDelivered(delivered);
            webhookDeliveryRepository.save(d);

            if (!d.isDelivered() && d.getAttempts() < maxAttempts) {
                long waitTime = retryDelayMs * d.getAttempts();
                
                System.out.println("[" + Thread.currentThread().getName() + "] Webhook failure " + deliveryId + 
                    ". Attempt " + d.getAttempts() + ". Retrying in " + waitTime + "ms");
                
                Thread.sleep(waitTime);
                
                tryDeliver(deliveryId);
            } else if (d.isDelivered()) {
                System.out.println("[" + Thread.currentThread().getName() + "] Webhook " + deliveryId + " delivered successfully.");
            }

        } catch (Exception e) {
            handleDeliveryException(d, e);
        }
    }

    private void handleDeliveryException(WebhookDelivery d, Exception e) {
        System.err.println("[" + Thread.currentThread().getName() + "] Critical delivery error: " + e.getMessage());
        
        d.setAttempts(d.getAttempts() + 1);
        d.setLastAttemptAt(Instant.now());
        d.setDelivered(false);
        webhookDeliveryRepository.save(d);

        if (d.getAttempts() < maxAttempts) {
            try {
                Thread.sleep(retryDelayMs * d.getAttempts());
                tryDeliver(d.getId());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String buildPayload(Payment p) {
        try {
            var data = Map.of(
                "paymentId", p.getId(),
                "status", p.getStatus().name(),
                "occurredAt", Instant.now().toString()
            );
            var event = Map.of(
                "id", "evt_" + UUID.randomUUID().toString().substring(0, 8),
                "type", "payment.updated",
                "data", data
            );
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            return null;
        }
    }
    
    @PreDestroy
    public void shutdown() {
        System.out.println("Shutting down Webhook thread pool...");
        executorService.shutdown();
    }
}