package org.example.vibelist.domain.playlist.emotion.llm;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmotionLLMClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${llm.gemini.api-key}")
    private String apiKey;

    @Value("${llm.gemini.url}")
    private String apiUrl;

    public String requestEmotionAnalysis(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> contentPart = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(contentPart));
        Map<String, Object> requestBody = Map.of("contents", List.of(content));

        String fullUrl = apiUrl + "?key=" + apiKey;

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(fullUrl, request, JsonNode.class);
            return extractContent(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Gemini API 호출 실패: " + e.getMessage(), e);
        }
    }

    private String extractContent(JsonNode json) {
        return json
                .get("candidates").get(0)
                .get("content").get("parts").get(0)
                .get("text").asText();
    }
}
