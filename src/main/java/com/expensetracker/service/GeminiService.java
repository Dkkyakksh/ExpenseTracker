package com.expensetracker.service;

import com.expensetracker.dto.GeminiParsedExpense;
import com.expensetracker.dto.GeminiParsedItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final String EXTRACTION_PROMPT = """
            Analyze this receipt/bill image and extract the following information.
            Respond ONLY with a valid JSON object — no markdown, no explanation, no code fences.

            Required JSON structure:
            {
              "merchantName": "Name of the store/restaurant/service",
              "category": "One of: Food & Dining, Groceries, Transport, Shopping, Entertainment, Health, Utilities, Travel, Other",
              "totalAmount": 100,
              "taxAmount": 5,
              "currency": "INR",
              "expenseDate": "YYYY-MM-DD",
              "paymentMethod": "Cash/Credit Card/Debit Card/UPI/Other",
              "items": [
                {
                  "name": "Item name",
                  "quantity": 1,
                  "unitPrice": 10.00,
                  "totalPrice": 10.00
                }
              ],
              "rawText": "Full raw text you can read from the receipt"
            }

            Rules:
            - If a field is not visible or unclear, use null for that field
            - totalAmount must be the final amount paid (after tax/discounts)
            - currency should be the 3-letter ISO code (INR, USD, EUR, etc.)
            - expenseDate format must be YYYY-MM-DD; use null if unclear
            - items array should contain ALL line items from the receipt; use empty array [] if none visible
            - Do not include any text outside the JSON object
            """;

    /**
     * Sends an image to Gemini Vision API and parses the response into a GeminiParsedExpense.
     */
    public GeminiParsedExpense parseReceiptImage(String requestId, MultipartFile imageFile) throws Exception {
        log.info("[requestId: {}]: Sending image to Gemini API: {}", requestId, imageFile.getOriginalFilename());

        String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
        String mimeType = resolveMimeType(imageFile.getContentType());

        Map<String, Object> requestBody = buildGeminiRequest(base64Image, mimeType);

        WebClient client = webClientBuilder.baseUrl(geminiApiUrl).build();

        String rawResponse = client.post()
                .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.debug("requestId: {}]: Raw Gemini response: {}", requestId, rawResponse);
        return parseGeminiResponse(rawResponse);
    }

    // ─── Build Gemini API Request Body ────────────────────────────────────────

    private Map<String, Object> buildGeminiRequest(String base64Image, String mimeType) {
        Map<String, Object> imagePart = Map.of(
                "inlineData", Map.of(
                        "mimeType", mimeType,
                        "data", base64Image
                )
        );

        Map<String, Object> textPart = Map.of("text", EXTRACTION_PROMPT);

        Map<String, Object> content = Map.of(
                "parts", List.of(imagePart, textPart)
        );

        return Map.of("contents", List.of(content));
    }

    // ─── Parse Gemini Response ────────────────────────────────────────────────

    private GeminiParsedExpense parseGeminiResponse(String rawResponse) throws Exception {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new RuntimeException("Empty response from Gemini API");
        }

        JsonNode root = objectMapper.readTree(rawResponse);

        // Check for API errors
        if (root.has("error")) {
            String errorMsg = root.path("error").path("message").asText("Unknown Gemini API error");
            throw new RuntimeException("Gemini API error: " + errorMsg);
        }

        // Navigate to the text content
        String jsonText = root
                .path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text").asText();

        log.debug("Extracted JSON text from Gemini: {}", jsonText);

        // Strip any accidental markdown fences just in case
        jsonText = cleanJsonText(jsonText);

        JsonNode parsed = objectMapper.readTree(jsonText);
        return mapToGeminiParsedExpense(parsed);
    }

    private GeminiParsedExpense mapToGeminiParsedExpense(JsonNode node) {
        GeminiParsedExpense result = new GeminiParsedExpense();
        result.setMerchantName(textOrNull(node, "merchantName"));
        result.setCategory(textOrNull(node, "category"));
        result.setTotalAmount(decimalOrNull(node, "totalAmount"));
        result.setTaxAmount(decimalOrNull(node, "taxAmount"));
        result.setCurrency(textOrNull(node, "currency"));
        result.setExpenseDate(textOrNull(node, "expenseDate"));
        result.setPaymentMethod(textOrNull(node, "paymentMethod"));
        result.setRawText(textOrNull(node, "rawText"));

        List<GeminiParsedItem> items = new ArrayList<>();
        JsonNode itemsNode = node.path("items");
        if (itemsNode.isArray()) {
            for (JsonNode itemNode : itemsNode) {
                GeminiParsedItem item = new GeminiParsedItem();
                item.setName(textOrNull(itemNode, "name"));
                item.setQuantity(itemNode.path("quantity").isNull() ? null : itemNode.path("quantity").asInt());
                item.setUnitPrice(decimalOrNull(itemNode, "unitPrice"));
                item.setTotalPrice(decimalOrNull(itemNode, "totalPrice"));
                items.add(item);
            }
        }
        result.setItems(items);
        return result;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String cleanJsonText(String text) {
        if (text == null) return "";
        // Remove ```json ... ``` or ``` ... ``` fences
        text = text.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "");
        return text.trim();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return (n.isMissingNode() || n.isNull()) ? null : n.asText();
    }

    private BigDecimal decimalOrNull(JsonNode node, String field) {
        JsonNode n = node.path(field);
        if (n.isMissingNode() || n.isNull()) return null;
        try {
            return new BigDecimal(n.asText());
        } catch (NumberFormatException e) {
            log.warn("Could not parse decimal for field '{}': {}", field, n.asText());
            return null;
        }
    }

    private String resolveMimeType(String contentType) {
        return Objects.nonNull(contentType) ? contentType.toLowerCase() : "image/jpeg";
    }
}
