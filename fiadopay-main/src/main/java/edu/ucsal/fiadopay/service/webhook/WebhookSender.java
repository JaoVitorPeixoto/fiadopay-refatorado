package edu.ucsal.fiadopay.service.webhook;

public interface WebhookSender {
    boolean sendWebhook(String webhookUrl, String signature, String payload, String eventType);
}
