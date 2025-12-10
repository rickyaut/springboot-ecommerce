package com.ecommerce.order.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SlackNotificationService {
    
    @Value("${slack.webhook.url:}")
    private String slackWebhookUrl;
    
    private final RestTemplate restTemplate;
    
    public SlackNotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        System.out.println("SlackNotificationService created with webhook URL: " + slackWebhookUrl);
    }
    
    public void sendDLQAlert(String topic, String orderId, String error) {
        if (slackWebhookUrl.isEmpty()) {
            return;
        }
        
        String message = String.format(
            "🚨 *DLQ Alert* 🚨\n" +
            "Topic: `%s`\n" +
            "Order ID: `%s`\n" +
            "Error: %s", 
            topic, orderId, error
        );
        
        SlackMessage slackMessage = new SlackMessage(message);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        try {
            restTemplate.postForEntity(slackWebhookUrl, new HttpEntity<>(slackMessage, headers), String.class);
        } catch (Exception ex) {
            System.err.println("Failed to send Slack notification: " + ex.getMessage());
        }
    }
    
    private record SlackMessage(String text) {}
}