package org.example.vibelist.domain.track.controller;


import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Base64;
import java.util.Collections;



@RestController
public class SpotifyController {

    @Value("${spotify.clientId}")
    private String clientId;

    @Value("${spotify.clientSecret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/{id}")
    public String test(@PathVariable String id) {
        try {
            String token = getAccessToken();

            // 트랙 ID 입력
            String trackId = "5z6b9pAx6rkAmg6jm6fA7n";
            String trackUrl = "https://api.spotify.com/v1/tracks/" + id;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    trackUrl, HttpMethod.GET, entity, String.class
            );

            // JSON 파싱
            JSONObject json = new JSONObject(response.getBody());

            String name = json.getString("name");
            String artist = json.getJSONArray("artists")
                    .getJSONObject(0)
                    .getString("name");
            String album = json.getJSONObject("album").getString("name");
            int durationMs = json.getInt("duration_ms");

            return "🎵 Title: " + name + "\n"
                    + "🎤 Artist: " + artist + "\n"
                    + "💿 Album: " + album + "\n"
                    + "⏱ Duration: " + (durationMs / 1000) + " seconds";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    private String getAccessToken() {
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

        ResponseEntity<SpotifyTokenResponse> response = restTemplate.postForEntity(
                url, request, SpotifyTokenResponse.class);

        return response.getBody().getAccess_token();
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
