package com.ecommerce.ecommerce_backend.Services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class TelegramService {

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.chat-id}")
    private String chatId;

    @Async
    public void sendMessage(String text) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "chat_id", chatId,
                    "text", text,
                    "parse_mode", "HTML"
            );

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);

            log.info("Telegram message sent");
        } catch (Exception e) {
            log.error("Telegram send failed: {}", e.getMessage());
        }
    }
}