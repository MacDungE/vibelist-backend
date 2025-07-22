package org.example.vibelist.domain.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.integration.entity.DevIntegrationTokenInfo;
import org.example.vibelist.domain.integration.entity.IntegrationTokenInfo;
import org.example.vibelist.global.response.GlobalException;
import org.example.vibelist.global.response.ResponseCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    private final RestTemplate restTemplate= new RestTemplate();

    private final IntegrationTokenInfoService integrationTokenInfoService;
    private final DevIntegrationTokenInfoService devIntegrationTokenInfoService;

    public  Map<String,String> refreshAccessToken(String refreshToken) {
        if (refreshToken == null) throw new GlobalException(ResponseCode.INTEGRATION_TOKEN_INVALID);

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
            throw new GlobalException(ResponseCode.INTEGRATION_SPOTIFY_REFRESH_FAIL);
        }
    }
/*
*
 */
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
            throw new GlobalException(ResponseCode.INTEGRATION_SPOTIFY_EXTRACT_USERID_FAIL);
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

    /*
       userId를 통해 accesstoken을 반환합니다.
     */
    public String resolveValidAccessToken(Long userid) {
        Optional<IntegrationTokenInfo> optionalInfo = integrationTokenInfoService.getTokenInfo(userid, "SPOTIFY");

        if (optionalInfo.isPresent()) {
            IntegrationTokenInfo info = optionalInfo.get();

            if (info.isExpired()) {
                log.info("사용자의 Access Token이 만료됨. Refresh 진행..");
                Map<String, String> tokenMap = refreshAccessToken(info.getRefreshToken());

                if (!info.getRefreshToken().equals(tokenMap.get("refresh_token"))) {
                    throw new GlobalException(ResponseCode.INTEGRATION_TOKEN_INVALID);
                }

                String newAccessToken = tokenMap.get("access_token");
                int expiresIn = Integer.parseInt(tokenMap.get("expires_in"));

                integrationTokenInfoService.updateAccessToken(userid, "SPOTIFY", newAccessToken, expiresIn);
                return newAccessToken;
            }

            return info.getAccessToken();
        } else {
            DevIntegrationTokenInfo dev = devIntegrationTokenInfoService.getDevAuth("sung_1");
            if (dev.getTokenExpiresAt() == null || LocalDateTime.now().isAfter(dev.getTokenExpiresAt().minusSeconds(60))) {
                log.info("개발자의 Access Token 만료됨. Refresh 진행..");

                Map<String, String> tokenMap = refreshAccessToken(dev.getRefreshToken());

                if (!dev.getRefreshToken().equals(tokenMap.get("refresh_token"))) {
                    throw new GlobalException(ResponseCode.INTEGRATION_TOKEN_INVALID);
                }

                String newAccessToken = tokenMap.get("access_token");
                LocalDateTime newExpires = LocalDateTime.now().plusSeconds(Integer.parseInt(tokenMap.get("expires_in")));

                devIntegrationTokenInfoService.updateDev("sung_1", newAccessToken, dev.getRefreshToken(), newExpires);
                return newAccessToken;
            }
            return dev.getAccessToken();
        }
    }
}