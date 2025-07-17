package org.example.vibelist.domain.playlist.emotion.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.vibelist.domain.playlist.emotion.profile.AudioFeatureRange;
import org.example.vibelist.global.exception.CustomException;
import org.example.vibelist.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

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
                // 1. HTTP 에러/외부 API 장애 감지
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(
                                        new CustomException(ErrorCode.LLM_API_ERROR)
                                ))

                )
                .bodyToMono(JsonNode.class)
                // 2. 타임아웃 / 통신장애 처리
                .timeout(Duration.ofSeconds(5))
                .onErrorMap(TimeoutException.class, e ->
                        new CustomException(ErrorCode.LLM_TIMEOUT))
                .onErrorMap(WebClientRequestException.class, e ->
                        new CustomException(ErrorCode.LLM_API_ERROR))
                // 3. JSON 파싱
                .map(json -> json.get("candidates").get(0).get("content").get("parts").get(0).get("text").asText())
                .map(this::extractJsonFromText)
                .map(jsonStr -> {
                    try {
                        return objectMapper.readValue(jsonStr, AudioFeatureRange.class);
                    } catch (Exception e) {
                        throw new CustomException(ErrorCode.LLM_PARSE_ERROR);
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
        throw new CustomException(ErrorCode.LLM_INVALID_FORMAT);
    }
}