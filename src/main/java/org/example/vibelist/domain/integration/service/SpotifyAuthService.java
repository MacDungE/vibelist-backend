package org.example.vibelist.domain.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.integration.entity.DevAuthToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpotifyAuthService {
    //application.properties에 명시된 비밀값
    @Value("${spring.security.oauth2.client.registration.spotify.client-id}")
    private String clientId;

    //application.properties에 명시된 비밀값
    @Value("${spring.security.oauth2.client.registration.spotify.client-secret}")
    private String clientSecret;

    private String name = "sung_1"; //여러분이 사용하실 admin user name을 입력해주시면 됩니다.
    //private final DevAuthTokenService devAuthTokenService;
    private final RestTemplate restTemplate= new RestTemplate();

    public  Map<String,String> refreshAccessToken(String refreshToken) {
        if (refreshToken == null) throw new IllegalStateException("Refresh token 없음");

        String url = "https://accounts.spotify.com/api/token";
        String auth = clientId + ":" + clientSecret;
        log.info("client id : {}, client secret : {}", clientId, clientSecret);
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        //header 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        //body 설정
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        try {
            JsonNode json = new ObjectMapper().readTree(response.getBody());
            String accessToken = json.get("access_token").asText(); //access_token 추출
            String scope = json.get("scope").asText();  // ⭐ scope 확인
            if (json.has("refresh_token")) {
                refreshToken = json.get("refresh_token").asText(); // refresh_token 추출
            }
            Integer expiresIn = json.get("expires_in").asInt(); //(현재시간 + expiry_time)값을 테이블에 저장
            Map<String,String> tokenMap = new HashMap<>();
            tokenMap.put("access_token",accessToken);
            tokenMap.put("refresh_token",refreshToken);
            tokenMap.put("expires_in",expiresIn.toString());
            return tokenMap;
        } catch (Exception e) {
            throw new RuntimeException("Refresh 실패", e);
        }
    }

    public  String getSpotifyUserId(String accessToken) {
        String url = "https://api.spotify.com/v1/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        log.info(headers.toString());

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String.class
        );
        log.info("response : {}", response.getBody());
        try {
            JsonNode root = new ObjectMapper().readTree(response.getBody());
            return root.get("id").asText(); //spotify상의 user_id 추출
        } catch (Exception e) {
            log.error("Failed to extract user ID from Spotify response", e);
            throw new RuntimeException("Failed to get Spotify user id", e);
        }
    }
    public boolean isAccessTokenValid(String accessToken) {
        String url = "https://api.spotify.com/v1/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
            );
            // 200 OK 응답이면 유효한 토큰
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            // 401 Unauthorized 이면 유효하지 않은 토큰
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return false;
            }
            throw e; // 다른 에러는 위임
        }
    }
}