package org.example.vibelist.domain.youtube.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.youtube.dto.YoutubeVideoMetaDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeApiClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${youtube.api.key}")
    private String apiKey;

    public YoutubeVideoMetaDto getYoutubeVideo(String query) {
        WebClient webClient = webClientBuilder
                .baseUrl("https://www.googleapis.com/youtube/v3")
                .build();

        String videoId;

        try {
            // Step 1: /search로 videoId 얻기
            log.info("🔍 [YouTube API] Calling /search for query: {}", query);
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

            JsonNode searchNode = objectMapper.readTree(searchResponse);
            videoId = searchNode.get("items").get(0).get("id").get("videoId").asText();
        } catch (WebClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            String errorMessage = extractYoutubeErrorMessage(errorBody);
            log.info("❌ [YouTube API] /search 오류 응답 메시지: {}", errorMessage);
            throw new RuntimeException("유튜브 /search API 오류: " + errorMessage, e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


        try {
            log.info("🎬 [YouTube API] Calling /videos for videoId: {}", videoId);
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

            JsonNode detailNode = objectMapper.readTree(detailResponse);
            String isoDuration = detailNode.get("items").get(0).get("contentDetails").get("duration").asText();
            int durationSeconds = (int) Duration.parse(isoDuration).getSeconds();

            return YoutubeVideoMetaDto.builder()
                    .url("https://www.youtube.com/watch?v=" + videoId)
                    .duration(durationSeconds)
                    .build();
        } catch (WebClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            String errorMessage = extractYoutubeErrorMessage(errorBody);
            log.info("❌ [YouTube API] /video 오류 응답 메시지: {}", errorMessage);
            throw new RuntimeException("유튜브 /video API 오류: " + errorMessage, e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String extractYoutubeErrorMessage(String responseBody) {
        try {
            YoutubeErrorResponse error = objectMapper.readValue(responseBody, YoutubeErrorResponse.class);
            return (error != null && error.error != null && error.error.message != null)
                    ? error.error.message
                    : "알 수 없는 오류";
        } catch (Exception e) {
            log.info("❗ 유튜브 오류 메시지 파싱 실패", e);
            return "알 수 없는 오류";
        }
    }

    private static class YoutubeErrorResponse {
        public Error error;

        public static class Error {
            public String message;
        }
    }
}