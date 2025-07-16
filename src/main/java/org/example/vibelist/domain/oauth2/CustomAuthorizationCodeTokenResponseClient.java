package org.example.vibelist.domain.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.auth.repository.AuthRepository;
import org.example.vibelist.global.constants.SocialProviderConstants;
import org.example.vibelist.global.constants.TokenManagementConstants;
import org.example.vibelist.domain.oauth2.dto.TokenInfo;
import org.example.vibelist.domain.oauth2.util.TokenInfoExtractor;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Spotify OAuth2에서 Refresh Token을 확실히 받기 위한 커스텀 토큰 응답 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthorizationCodeTokenResponseClient implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    
    // RestClientAuthorizationCodeTokenResponseClient는 의존성 주입이 아닌 직접 생성
    private final RestClientAuthorizationCodeTokenResponseClient defaultClient = new RestClientAuthorizationCodeTokenResponseClient();

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest) {
        log.info("[OAUTH2_TOKEN] 커스텀 토큰 응답 클라이언트 시작");
        
        try {
            // 기본 구현 호출
            OAuth2AccessTokenResponse tokenResponse = defaultClient.getTokenResponse(authorizationCodeGrantRequest);
            
            // 공통 토큰 정보 로깅
            String registrationId = authorizationCodeGrantRequest.getClientRegistration().getRegistrationId();
            log.info("[OAUTH2_TOKEN] {} 토큰 응답 수신", registrationId.toUpperCase());
            log.info("[OAUTH2_TOKEN] Access Token 존재: {}", tokenResponse.getAccessToken() != null);
            log.info("[OAUTH2_TOKEN] Refresh Token 존재: {}", tokenResponse.getRefreshToken() != null);
            log.info("[OAUTH2_TOKEN] Token Type: {}", tokenResponse.getAccessToken() != null ? tokenResponse.getAccessToken().getTokenType().getValue() : "null");
            
            // TokenInfo 추출 및 로깅
            TokenInfo tokenInfo = TokenInfoExtractor.extractFromTokenResponse(tokenResponse);
            log.info("[OAUTH2_TOKEN] Expires In: {} 초", tokenInfo.getExpiresIn());
            log.info("[OAUTH2_TOKEN] Scope: {}", tokenInfo.getScope());
            
            // Provider별 토큰 정보 추가
            Map<String, Object> additionalParameters = new HashMap<>(tokenResponse.getAdditionalParameters());
            
            if (SocialProviderConstants.SPOTIFY_LOWER.equals(registrationId)) {
                // Provider별 토큰 정보를 enriched parameters에 추가
                Map<String, Object> enrichedParams = TokenInfoExtractor.enrichAdditionalParameters(registrationId, tokenInfo);
                additionalParameters.putAll(enrichedParams);
                
                if (tokenInfo.getRefreshToken() != null) {
                    log.info("[OAUTH2_TOKEN] ✅ Spotify Refresh Token 획득 성공");
                } else {
                    log.warn("[OAUTH2_TOKEN] ⚠️ Spotify Refresh Token이 없습니다. 직접 호출을 시도합니다.");
                    
                    // Spotify에서 refresh token이 없는 경우 직접 호출 시도
                    SpotifyTokenResponse directResponse = getSpotifyTokenResponse(
                        authorizationCodeGrantRequest.getAuthorizationExchange().getAuthorizationResponse().getCode(),
                        authorizationCodeGrantRequest.getClientRegistration().getRedirectUri(),
                        authorizationCodeGrantRequest.getClientRegistration().getClientId(),
                        authorizationCodeGrantRequest.getClientRegistration().getClientSecret()
                    );
                    
                    if (directResponse != null && directResponse.getRefresh_token() != null) {
                        log.info("[OAUTH2_TOKEN] ✅ Spotify 직접 호출로 Refresh Token 획득 성공!");
                        additionalParameters.put(TokenManagementConstants.SPOTIFY_REFRESH_TOKEN_KEY, directResponse.getRefresh_token());
                        additionalParameters.put(TokenManagementConstants.SPOTIFY_ACCESS_TOKEN_KEY, directResponse.getAccess_token());
                        additionalParameters.put(TokenManagementConstants.SPOTIFY_EXPIRES_IN_KEY, directResponse.getExpires_in());
                        additionalParameters.put(TokenManagementConstants.SPOTIFY_SCOPE_KEY, directResponse.getScope());
                        
                        log.info("[OAUTH2_TOKEN] 직접 호출 결과 - Access Token: {}, Refresh Token: {}, Expires In: {} 초, Scope: {}", 
                            directResponse.getAccess_token() != null, 
                            directResponse.getRefresh_token() != null,
                            directResponse.getExpires_in(),
                            directResponse.getScope());
                    } else {
                        log.error("[OAUTH2_TOKEN] ❌ Spotify 직접 호출에서도 Refresh Token을 받지 못했습니다.");
                        log.error("[OAUTH2_TOKEN] Spotify 앱 설정에서 다음을 확인하세요:");
                        log.error("[OAUTH2_TOKEN] 1. Spotify Dashboard > App Settings");
                        log.error("[OAUTH2_TOKEN] 2. Redirect URIs가 정확히 설정되어 있는지 확인");
                        log.error("[OAUTH2_TOKEN] 3. App이 'Development Mode'가 아닌 'Extended Quota Mode'인지 확인");
                    }
                }
                
                log.info("[OAUTH2_TOKEN] Spotify 토큰 정보를 additionalParameters에 추가 완료");
                log.info("[OAUTH2_TOKEN] tokenResponse 데이터는 OAuth2UserService에서 저장됩니다");
            }

            // 수정된 additionalParameters로 새로운 OAuth2AccessTokenResponse 생성
            if (!additionalParameters.equals(tokenResponse.getAdditionalParameters())) {
                return OAuth2AccessTokenResponse.withToken(tokenResponse.getAccessToken().getTokenValue())
                    .tokenType(tokenResponse.getAccessToken().getTokenType())
                    .expiresIn(tokenResponse.getAccessToken().getExpiresAt() != null && tokenResponse.getAccessToken().getIssuedAt() != null ?
                        tokenResponse.getAccessToken().getExpiresAt().getEpochSecond() - tokenResponse.getAccessToken().getIssuedAt().getEpochSecond() : 0)
                    .scopes(tokenResponse.getAccessToken().getScopes())
                    .refreshToken(tokenResponse.getRefreshToken() != null ? tokenResponse.getRefreshToken().getTokenValue() : null)
                    .additionalParameters(additionalParameters)
                    .build();
            }
            
            return tokenResponse;
            
        } catch (Exception e) {
            log.error("[OAUTH2_TOKEN] 토큰 응답 처리 중 오류 발생", e);
            throw e;
        }
    }
    
    /**
     * Spotify 토큰 응답을 직접 파싱하는 메서드 (필요시 사용)
     */
    public SpotifyTokenResponse getSpotifyTokenResponse(String code, String redirectUri, String clientId, String clientSecret) {
        String url = "https://accounts.spotify.com/api/token";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
        
        RestTemplate restTemplate = new RestTemplate();
        
        try {
            ResponseEntity<SpotifyTokenResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, SpotifyTokenResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                SpotifyTokenResponse tokenResponse = response.getBody();
                log.info("[OAUTH2_TOKEN] Spotify 직접 토큰 응답 - Access Token: {}, Refresh Token: {}", 
                    tokenResponse.getAccess_token() != null, tokenResponse.getRefresh_token() != null);
                return tokenResponse;
            }
        } catch (Exception e) {
            log.error("[OAUTH2_TOKEN] Spotify 직접 토큰 요청 실패", e);
        }
        
        return null;
    }
    
    /**
     * Spotify 토큰 응답 DTO - 공통 TokenInfo와 매핑을 위한 임시 DTO
     */
    public static class SpotifyTokenResponse {
        private String access_token;
        private String token_type;
        private int expires_in;
        private String refresh_token;
        private String scope;
        
        // Getters and Setters
        public String getAccess_token() { return access_token; }
        public void setAccess_token(String access_token) { this.access_token = access_token; }
        
        public String getToken_type() { return token_type; }
        public void setToken_type(String token_type) { this.token_type = token_type; }
        
        public int getExpires_in() { return expires_in; }
        public void setExpires_in(int expires_in) { this.expires_in = expires_in; }
        
        public String getRefresh_token() { return refresh_token; }
        public void setRefresh_token(String refresh_token) { this.refresh_token = refresh_token; }
        
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        
        /**
         * 공통 TokenInfo로 변환
         */
        public TokenInfo toTokenInfo() {
            return TokenInfo.builder()
                    .accessToken(access_token)
                    .refreshToken(refresh_token)
                    .tokenType(token_type)
                    .expiresIn(expires_in)
                    .scope(scope)
                    .build();
        }
    }
} 