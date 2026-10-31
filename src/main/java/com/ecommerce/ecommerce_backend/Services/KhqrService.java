package com.ecommerce.ecommerce_backend.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class KhqrService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Read from application.properties
    @Value("${khqr.api-token}")
    private String khqrToken;

    public Map<String, Object> generateKHQR(Double amount, String orderId) {
        try {
            String url = "https://api.khqr.dev/v1/generate_qr";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Clean the token just in case there are hidden spaces
            String cleanToken = khqrToken.trim();
            headers.setBearerAuth(cleanToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("amount", amount);
            requestBody.put("currency", "USD");
            requestBody.put("bill_number", orderId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate KHQR: " + e.getMessage());
        }
    }
}