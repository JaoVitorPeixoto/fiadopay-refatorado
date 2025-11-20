package edu.ucsal.fiadopay.service.webhook;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component("httpSender")
public class HttpWebhookSender implements WebhookSender {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public boolean sendWebhook(String webhookUrl, String signature, String payload,  String eventType) {
        try {
            var req = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .header("X-Event-Type", eventType)
                    .header("X-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            return res.statusCode() >= 200 && res.statusCode() < 300;

        } catch (Exception e) {
            return false;
        }
    }
}
