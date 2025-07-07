package org.example.vibelist.domain.track.client;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.track.dto.SpotifyTrackMetaDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class SpotifyApiClient {

    private final WebClient.Builder webClientBuilder;

    //@Value("${spotify.api.token}")
    private String accessToken = "123";

    public SpotifyTrackMetaDto getTrackMeta(String trackId) {
        WebClient webClient = webClientBuilder
                .baseUrl("https://api.spotify.com/v1")
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();

        return webClient.get()
                .uri("/tracks/{id}", trackId)
                .retrieve()
                .bodyToMono(SpotifyTrackMetaDto.class)
                .block();
    }
}
