package org.example.vibelist.domain.batch.spotify.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.batch.spotify.dto.SpotifyTrackMetaDto;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpotifyApiClient {


    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${spotify.clientId}")
    private final String clientId;
    @Value("${spotify.clientSecret}")
    private final String clientSecret;

    public SpotifyTrackMetaDto getTrackMeta(String spotifyId) {
        try {
            String token = getAccessToken();

            String trackUrl = "https://api.spotify.com/v1/tracks/" + spotifyId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    trackUrl, HttpMethod.GET, entity, String.class
            );

            JSONObject json = new JSONObject(response.getBody());

            String title = json.getString("name");
            String artist = json.getJSONArray("artists").getJSONObject(0).getString("name");
            String album = json.getJSONObject("album").getString("name");
            int durationMs = json.getInt("duration_ms");
            boolean explicit = json.getBoolean("explicit");
            int popularity = json.getInt("popularity");
            String imageUrl = json.getJSONObject("album")
                    .getJSONArray("images")
                    .getJSONObject(0)
                    .getString("url");

            log.info("title: {}, artist: {}, album: {}, durationMs: {}, explicit: {}, popularity: {}, imageUrl: {}",
                    title, artist, album, durationMs, explicit, popularity, imageUrl);

            SpotifyTrackMetaDto dto = SpotifyTrackMetaDto.builder()
                    .title(title)
                    .artist(artist)
                    .album(album)
                    .durationMs(durationMs)
                    .popularity(popularity)
                    .explicit(explicit)
                    .imageUrl(imageUrl)
                    .build();

            return dto;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getAccessToken() {
        String url = "https://accounts.spotify.com/api/token";

        // Authorization 헤더 설정 (Base64 인코딩)
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // grant_type 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<SpotifyApiClient.SpotifyTokenResponse> response = restTemplate.postForEntity(
                url, request, SpotifyApiClient.SpotifyTokenResponse.class);

        return response.getBody().getAccess_token();
    }

    public String getAccessTokenFromCode(String code) {
        String url = "https://accounts.spotify.com/api/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret); // Base64 자동 처리

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", "http://localhost:8080/callback"); // Spotify 앱에 등록된 redirect_uri

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        Map<String, Object> tokenMap = response.getBody();

        String accessToken = (String) tokenMap.get("access_token");
        String refreshToken = (String) tokenMap.get("refresh_token");

        // 🔐 저장 필요 시 DB나 Redis 등에 저장
        System.out.println("access_token = " + accessToken);
        System.out.println("refresh_token = " + refreshToken);

        return accessToken;
    }
    /*
    userid를 추출하는 메소드입니다.
     */
    public String getSpotifyUserId(String accessToken) throws Exception {
        String url = "https://api.spotify.com/v1/me";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, request, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(response.getBody());

        return root.get("id").asText(); // Spotify 유저 ID 반환
    }
    // 내부 클래스: 토큰 응답 파싱용
    static class SpotifyTokenResponse {
        private String access_token;

        public String getAccess_token() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }
    }



}
