package org.example.vibelist.domain.batch.spotify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpotifyAuthService {
    @Value("${spotify.clientId}")
    private String clientId;

    @Value("${spotify.clientSecret}")
    private String clientSecret;

    //spotify dashboard에 명시한 redirect URI
    private String redirectUri = "http://127.0.0.1:8080/v1/playlist/callback";

    private final RestTemplate restTemplate= new RestTemplate();
    private String accessToken;
    private String refreshToken;
    private Instant tokenExpiry;

    /**
     * 1. 사용자가 로그인할 수 있는 Spotify URL 반환
     */
    public String getAuthorizationUrl() {
        String scope = "user-read-private user-read-email playlist-modify-private";
        return UriComponentsBuilder.fromHttpUrl("https://accounts.spotify.com/authorize")
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .build().toUriString();
    }

    /**
     * 2. Spotify에서 받은 code를 이용해 access_token과 refresh_token 교환
     */
    public String  exchangeCodeForTokens(String code) {
        String url = "https://accounts.spotify.com/api/token";

        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        try {
            JsonNode json = new ObjectMapper().readTree(response.getBody());
            accessToken = json.get("access_token").asText();
            refreshToken = json.get("refresh_token").asText();
            tokenExpiry = Instant.now().plusSeconds(json.get("expires_in").asLong());
            // 🟢 여기서 refreshToken은 DB에 저장할 것
            log.info("Access token: {}", accessToken);
            return accessToken;
        } catch (Exception e) {
            throw new RuntimeException("Token 파싱 실패", e);
        }
    }

    /**
     * 3. refresh token을 이용해 access_token 재발급
     */
    public String refreshAccessToken() {
        if (refreshToken == null) throw new IllegalStateException("Refresh token 없음");

        String url = "https://accounts.spotify.com/api/token";
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        try {
            JsonNode json = new ObjectMapper().readTree(response.getBody());
            accessToken = json.get("access_token").asText();
            if (json.has("refresh_token")) {
                refreshToken = json.get("refresh_token").asText();
            }
            tokenExpiry = Instant.now().plusSeconds(json.get("expires_in").asLong());
            return accessToken;
        } catch (Exception e) {
            throw new RuntimeException("Refresh 실패", e);
        }
    }

    public String getSpotifyUserId(String accessToken) {
        String url = "https://api.spotify.com/v1/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String.class
        );
        try {
            JsonNode root = new ObjectMapper().readTree(response.getBody());
            return root.get("id").asText();
        } catch (Exception e) {
            log.error("Failed to extract user ID from Spotify response", e);
            throw new RuntimeException("Failed to get Spotify user id", e);
        }
    }

    public synchronized String getAccessToken() {
        if (tokenExpiry != null && Instant.now().isAfter(tokenExpiry.minusSeconds(60))) {
            return refreshAccessToken();
        }
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}