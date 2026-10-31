package com.ecommerce.ecommerce_backend.Controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class BakongTestController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/bakong-qr")
    public ResponseEntity<?> testBakongQR() {
        try {
            // 1. The KHQR.dev API URL for generating a QR
            String url = "https://api.khqr.dev/v1/generate_qr";

            // 2. Set up the headers with your rbk... token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // PASTE YOUR rbk_ TOKEN HERE
            headers.setBearerAuth("rbk_enwdt3Rnb2feGRdZzUttnbraE87i5SFD");

            // 3. Create the request body (only amount is needed)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("amount", 1.50);
            requestBody.put("currency", "USD");

            // 4. Send the request
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            // 5. Return the response (should contain the QR string)
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}