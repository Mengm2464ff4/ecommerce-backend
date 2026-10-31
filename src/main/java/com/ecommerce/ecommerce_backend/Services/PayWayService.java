package com.ecommerce.ecommerce_backend.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class PayWayService {

    @Value("${payway.merchant-id}")
    private String merchantId;

    @Value("${payway.api-key}")
    private String apiKey;

    @Value("${payway.sandbox-url}")
    private String sandboxUrl;

    @Value("${payway.production-url}")
    private String productionUrl;

    @Value("${payway.sandbox-mode}")
    private boolean sandboxMode;

    private static final String LIFETIME_MINUTES = "1440";
    private static final String QR_TEMPLATE = "template3_color";
    private static final String PAYMENT_OPTION = "abapay_khqr";
    private static final String PURCHASE_TYPE = "purchase";

    private static final DateTimeFormatter REQUEST_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PayWayService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private String getBaseUrl() {
        String baseUrl = sandboxMode
                ? sandboxUrl
                : productionUrl;

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "PayWay base URL is not configured"
            );
        }

        return baseUrl.trim().replaceAll("/+$", "");
    }

    @PostConstruct
    public void verifyConfig() {
        merchantId = trimToNull(merchantId);
        apiKey = trimToNull(apiKey);
        sandboxUrl = trimToNull(sandboxUrl);
        productionUrl = trimToNull(productionUrl);

        if (merchantId == null) {
            throw new IllegalStateException(
                    "payway.merchant-id is missing"
            );
        }

        if (apiKey == null) {
            throw new IllegalStateException(
                    "payway.api-key is missing"
            );
        }

        log.info(
                "PayWay configured. merchantIdConfigured={}, " +
                        "apiKeyConfigured={}, sandboxMode={}, baseUrl={}",
                merchantId != null,
                apiKey != null,
                sandboxMode,
                getBaseUrl()
        );
    }

    /*
     * Keep this six-argument method because your OrderService currently
     * calls createPaymentAndGetQR with six arguments.
     */
    public Map<String, Object> createPaymentAndGetQR(
            String tranId,
            double amount,
            String currency,
            String callbackUrl,
            String returnUrl,
            String cancelUrl
    ) {
        return createPaymentAndGetQR(
                tranId,
                amount,
                currency,
                callbackUrl,
                returnUrl,
                cancelUrl,
                "",
                "",
                ""
        );
    }

    /*
     * Java does not support default parameter values.
     * Therefore, optional values are handled through this overload.
     */
    public Map<String, Object> createPaymentAndGetQR(
            String tranId,
            double amount,
            String currency,
            String callbackUrl,
            String returnUrl,
            String cancelUrl,
            String customFields,
            String returnParams,
            String payout
    ) {
        validateRequest(
                tranId,
                amount,
                currency,
                callbackUrl,
                customFields,
                returnParams,
                payout
        );

        String safeCustomFields = customFields == null
                ? ""
                : customFields;

        String safeReturnParams = returnParams == null
                ? ""
                : returnParams;

        String safePayout = payout == null
                ? ""
                : payout;

        String reqTime = LocalDateTime.now()
                .format(REQUEST_TIME_FORMAT);

        String amountStr = String.format(
                Locale.US,
                "%.2f",
                amount
        );

        String itemsJson = String.format(
                Locale.US,
                "[{\"name\":\"Order %s\",\"quantity\":1,\"price\":%s}]",
                tranId,
                amountStr
        );

        String encodedItems = base64Encode(itemsJson);

        String encodedCallback = base64Encode(callbackUrl);

        /*
         * PayWay hash field order.
         * These values must match the JSON request values exactly.
         */
        String rawData =
                reqTime
                        + merchantId
                        + tranId
                        + amountStr
                        + encodedItems
                        + ""                    // first_name
                        + ""                    // last_name
                        + ""                    // email
                        + ""                    // phone
                        + PURCHASE_TYPE
                        + PAYMENT_OPTION
                        + encodedCallback
                        + ""                    // return_deeplink
                        + currency
                        + safeCustomFields
                        + safeReturnParams
                        + safePayout
                        + LIFETIME_MINUTES
                        + QR_TEMPLATE;

        String hash = generateHash(rawData);

        Map<String, String> requestBody = new LinkedHashMap<>();

        requestBody.put("req_time", reqTime);
        requestBody.put("merchant_id", merchantId);
        requestBody.put("tran_id", tranId);
        requestBody.put("amount", amountStr);
        requestBody.put("items", encodedItems);
        requestBody.put("first_name", "");
        requestBody.put("last_name", "");
        requestBody.put("email", "");
        requestBody.put("phone", "");
        requestBody.put("purchase_type", PURCHASE_TYPE);
        requestBody.put("payment_option", PAYMENT_OPTION);
        requestBody.put("callback_url", encodedCallback);
        requestBody.put("return_deeplink", "");
        requestBody.put("currency", currency);
        requestBody.put("custom_fields", safeCustomFields);
        requestBody.put("return_params", safeReturnParams);
        requestBody.put("payout", safePayout);
        requestBody.put("lifetime", LIFETIME_MINUTES);
        requestBody.put("qr_image_template", QR_TEMPLATE);
        requestBody.put("hash", hash);

        String jsonBody;

        try {
            jsonBody = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Could not create PayWay request JSON",
                    e
            );
        }

        String url = getBaseUrl()
                + "/api/payment-gateway/v1/payments/generate-qr";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(
                java.util.List.of(MediaType.APPLICATION_JSON)
        );

        HttpEntity<String> requestEntity =
                new HttpEntity<>(jsonBody, headers);

        log.info(
                "Sending PayWay QR request. tranIdLength={}, " +
                        "customFieldsLength={}, amount={}, sandboxMode={}",
                tranId.length(),
                safeCustomFields.length(),
                amountStr,
                sandboxMode
        );

        try {
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(
                            url,
                            requestEntity,
                            Map.class
                    );

            Map<String, Object> responseBody =
                    response.getBody();

            if (responseBody == null) {
                throw new IllegalStateException(
                        "PayWay returned an empty response"
                );
            }

            log.info(
                    "PayWay QR response received. tranId={}",
                    tranId
            );

            return responseBody;

        } catch (HttpStatusCodeException e) {
            log.error(
                    "PayWay QR request failed. status={}, response={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );

            throw new RuntimeException(
                    "PayWay QR API call failed: "
                            + e.getStatusCode()
                            + " - "
                            + e.getResponseBodyAsString(),
                    e
            );

        } catch (Exception e) {
            log.error(
                    "Unexpected PayWay QR error for tranId={}",
                    tranId,
                    e
            );

            throw new RuntimeException(
                    "PayWay QR API call failed: "
                            + e.getMessage(),
                    e
            );
        }
    }

    public boolean verifyCallbackHash(
            Map<String, Object> payload,
            String receivedHash
    ) {
        if (payload == null || receivedHash == null) {
            return false;
        }

        String tranId = value(payload, "tran_id");
        String apv = value(payload, "apv");
        String status = value(payload, "status");
        String returnParams = value(payload, "return_params");

        if (tranId == null || apv == null || status == null) {
            return false;
        }

        String rawData =
                tranId
                        + apv
                        + status
                        + (returnParams == null
                        ? ""
                        : returnParams);

        String calculatedHash = generateHash(rawData);

        return MessageDigest.isEqual(
                calculatedHash.getBytes(StandardCharsets.UTF_8),
                receivedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void validateRequest(
            String tranId,
            double amount,
            String currency,
            String callbackUrl,
            String customFields,
            String returnParams,
            String payout
    ) {
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalStateException(
                    "PayWay merchant ID is missing"
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "PayWay API key is missing"
            );
        }

        if (tranId == null || tranId.isBlank()) {
            throw new IllegalArgumentException(
                    "PayWay tran_id is required"
            );
        }

        if (tranId.length() > 20) {
            throw new IllegalArgumentException(
                    "PayWay tran_id cannot exceed 20 characters. "
                            + "Current length: "
                            + tranId.length()
            );
        }

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "PayWay amount must be greater than zero"
            );
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException(
                    "PayWay currency is required"
            );
        }

        if (callbackUrl == null || callbackUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "PayWay callback URL is required"
            );
        }

        if (customFields != null && customFields.length() > 20) {
            throw new IllegalArgumentException(
                    "PayWay custom_fields cannot exceed 20 characters"
            );
        }

        if (returnParams != null && returnParams.length() > 20) {
            throw new IllegalArgumentException(
                    "PayWay return_params cannot exceed 20 characters"
            );
        }

        if (payout != null && payout.length() > 20) {
            throw new IllegalArgumentException(
                    "PayWay payout cannot exceed 20 characters"
            );
        }
    }

    private String generateHash(String rawData) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");

            SecretKeySpec secretKey = new SecretKeySpec(
                    apiKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            );

            hmac.init(secretKey);

            byte[] hashBytes = hmac.doFinal(
                    rawData.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder()
                    .encodeToString(hashBytes);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate PayWay hash",
                    e
            );
        }
    }

    private String base64Encode(String value) {
        return Base64.getEncoder()
                .encodeToString(
                        value.getBytes(StandardCharsets.UTF_8)
                );
    }

    private String value(
            Map<String, Object> payload,
            String key
    ) {
        Object value = payload.get(key);

        return value == null
                ? null
                : String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isBlank()
                ? null
                : trimmed;
    }
}
