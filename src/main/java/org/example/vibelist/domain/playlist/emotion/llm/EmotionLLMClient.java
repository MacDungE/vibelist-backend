package org.example.vibelist.domain.playlist.emotion.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.vibelist.domain.playlist.emotion.profile.AudioFeatureRange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class EmotionLLMClient {

    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.gemini.api-key}")
    private String apiKey;

    @Value("${llm.gemini.url}")
    private String apiUrl;

    public Mono<AudioFeatureRange> requestEmotionAnalysis (String prompt){
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "thinkingConfig", Map.of(
                                "thinkingBudget", 0
                        )
                )
        );

        return webClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.get("candidates").get(0).get("content").get("parts").get(0).get("text").asText())
                .map(this::extractJsonFromText)
                .map(jsonStr -> {
                    try {
                        return objectMapper.readValue(jsonStr, AudioFeatureRange.class);
                    } catch (Exception e) {
                        throw new RuntimeException("JSON 파싱 실패: " + jsonStr, e);
                    }
                });
    }
    // 응답에서 JSON 부분만 추출 (정규식 사용)
    private String extractJsonFromText(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        throw new IllegalArgumentException("JSON 포맷이 아님: " + text);
    }
}