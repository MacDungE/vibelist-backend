package org.example.vibelist.domain.youtube.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.youtube.dto.YoutubeVideoMetaDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class YoutubeApiClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${youtube.api.key}")
    private String apiKey;

    public YoutubeVideoMetaDto getYoutubeVideo(String query) {
        WebClient webClient = webClientBuilder
                .baseUrl("https://www.googleapis.com/youtube/v3")
                .build();

        try {
            // Step 1: /search로 videoId 얻기
            String searchResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("q", query)
                            .queryParam("type", "video")
                            .queryParam("maxResults", 1)
                            .queryParam("videoEmbeddable", true)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode searchNode = mapper.readTree(searchResponse);
            String videoId = searchNode.get("items").get(0).get("id").get("videoId").asText();

            // Step 2: /videos로 duration 얻기
            String detailResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/videos")
                            .queryParam("part", "contentDetails")
                            .queryParam("id", videoId)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode detailNode = mapper.readTree(detailResponse);
            String isoDuration = detailNode.get("items").get(0).get("contentDetails").get("duration").asText();
            int durationSeconds = (int) Duration.parse(isoDuration).getSeconds();

            return YoutubeVideoMetaDto.builder()
                    .url("https://www.youtube.com/watch?v=" + videoId)
                    .duration(durationSeconds)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("유튜브 API 응답 파싱 실패", e);
        }
    }
}

