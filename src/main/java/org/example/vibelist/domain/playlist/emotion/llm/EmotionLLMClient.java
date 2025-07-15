package org.example.vibelist.domain.playlist.emotion.llm;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
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

    @Value("${llm.gemini.api-key}")
    private String apiKey;

    @Value("${llm.gemini.url}")
    private String apiUrl;

    public Mono<String> requestEmotionAnalysis (String prompt){
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
                .map(json -> json.get("candidates").get(0).get("content").get("parts").get(0).get("text").asText());
    }
}