package org.example.vibelist.domain.oauth2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.auth.entity.Auth;
import org.example.vibelist.domain.auth.repository.AuthRepository;
import org.example.vibelist.global.constants.SocialProviderConstants;
import org.example.vibelist.global.constants.TokenManagementConstants;
import org.example.vibelist.domain.integration.service.IntegrationTokenInfoService;
import org.example.vibelist.domain.oauth2.dto.TokenInfo;
import org.example.vibelist.domain.oauth2.util.TokenInfoExtractor;
import org.example.vibelist.domain.user.entity.User;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * OAuth2 토큰 통합 관리 서비스
 * 토큰 저장, 조회, 관리를 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TokenHandler {
    
    private final IntegrationTokenInfoService integrationTokenInfoService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final AuthRepository authRepository;
    
    /**
     * Provider별 통합 토큰 정보 저장
     */
    public void saveIntegrationTokenInfo(User user, String provider, OAuth2UserRequest userRequest) {
        try {
            if (SocialProviderConstants.SPOTIFY_LOWER.equals(provider)) {
                saveSpotifyIntegrationTokenInfo(user, provider, userRequest);
            } else {
                saveGeneralIntegrationTokenInfo(user, provider, userRequest);
            }
        } catch (Exception e) {
            log.error("[TOKEN_HANDLER] 토큰 정보 저장 중 오류 발생 - userId: {}, provider: {}", user.getId(), provider, e);
        }
    }
    
    /**
     * Spotify Refresh Token을 OAuth2AuthorizedClient에서 조회
     */
    public Optional<String> getSpotifyRefreshToken(String principalName) {
        try {
            log.info("[TOKEN_HANDLER] Spotify Refresh Token 조회 시도 - principalName: {}", principalName);
            
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                SocialProviderConstants.SPOTIFY_LOWER, principalName);
            
            if (authorizedClient != null && authorizedClient.getRefreshToken() != null) {
                String refreshToken = authorizedClient.getRefreshToken().getTokenValue();
                log.info("[TOKEN_HANDLER] Spotify Refresh Token 획득 성공");
                return Optional.of(refreshToken);
            } else {
                log.warn("[TOKEN_HANDLER] Spotify Refresh Token이 없습니다.");
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("[TOKEN_HANDLER] Spotify Refresh Token 조회 중 오류 발생", e);
            return Optional.empty();
        }
    }

    /**
     * Spotify Access Token을 OAuth2AuthorizedClient에서 조회
     */
    public Optional<String> getSpotifyAccessToken(String principalName) {
        try {
            log.info("[TOKEN_HANDLER] Spotify Access Token 조회 시도 - principalName: {}", principalName);
            
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                SocialProviderConstants.SPOTIFY_LOWER, principalName);
            
            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                String accessToken = authorizedClient.getAccessToken().getTokenValue();
                log.info("[TOKEN_HANDLER] Spotify Access Token 획득 성공");
                return Optional.of(accessToken);
            } else {
                log.warn("[TOKEN_HANDLER] Spotify Access Token이 없습니다.");
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("[TOKEN_HANDLER] Spotify Access Token 조회 중 오류 발생", e);
            return Optional.empty();
        }
    }

    /**
     * 사용자 ID로 Spotify 토큰 정보 조회 (DB + OAuth2AuthorizedClient)
     */
    public IntegratedTokenInfo getSpotifyTokenInfo(Long userId, String principalName) {
        // DB에서 Auth 정보 조회
        Optional<Auth> authOpt = authRepository.findByUserIdAndProvider(userId, SocialProviderConstants.SPOTIFY);
        
        // OAuth2AuthorizedClient에서 토큰 조회
        Optional<String> oauth2AccessToken = getSpotifyAccessToken(principalName);
        Optional<String> oauth2RefreshToken = getSpotifyRefreshToken(principalName);
        
        return new IntegratedTokenInfo(
            userId,
            principalName,
            authOpt.map(Auth::getRefreshTokenEnc).orElse(null),
            oauth2AccessToken.orElse(null),
            oauth2RefreshToken.orElse(null)
        );
    }
    
    /**
     * Spotify 전용 토큰 정보 저장
     */
    private void saveSpotifyIntegrationTokenInfo(User user, String provider, OAuth2UserRequest userRequest) {
        // 1. additionalParameters에서 직접 토큰 정보 추출
        Map<String, Object> additionalParams = userRequest.getAdditionalParameters();
        
        String accessToken = extractStringFromAdditionalParams(additionalParams, TokenManagementConstants.SPOTIFY_ACCESS_TOKEN_KEY);
        String refreshToken = extractStringFromAdditionalParams(additionalParams, TokenManagementConstants.SPOTIFY_REFRESH_TOKEN_KEY);
        String tokenType = extractStringFromAdditionalParams(additionalParams, TokenManagementConstants.SPOTIFY_TOKEN_TYPE_KEY);
        Integer expiresIn = extractIntegerFromAdditionalParams(additionalParams, TokenManagementConstants.SPOTIFY_EXPIRES_IN_KEY);
        String scope = extractStringFromAdditionalParams(additionalParams, TokenManagementConstants.SPOTIFY_SCOPE_KEY);
        
        log.info("[TOKEN_HANDLER] Spotify 토큰 정보 추출 - accessToken: {}, refreshToken: {}, tokenType: {}, expiresIn: {}초, scope: {}", 
                accessToken != null, refreshToken != null, tokenType, expiresIn, scope);
        
        if (accessToken != null) {
            // IntegrationTokenInfo에 저장
            integrationTokenInfoService.saveOrUpdateTokenInfo(user, provider.toUpperCase(), 
                    accessToken, refreshToken, tokenType, expiresIn, scope);
            
            log.info("[TOKEN_HANDLER] Spotify 토큰 정보 저장 완료 - userId: {}", user.getId());
        } else {
            log.warn("[TOKEN_HANDLER] Spotify Access Token이 없어서 저장하지 않습니다 - userId: {}", user.getId());
        }
    }
    
    private void saveGeneralIntegrationTokenInfo(User user, String provider, OAuth2UserRequest userRequest) {
        // TokenInfoExtractor를 사용하여 토큰 정보 추출
        TokenInfo tokenInfo = TokenInfoExtractor.extractFromUserRequest(userRequest);
        
        if (tokenInfo != null) {
            log.info("[TOKEN_HANDLER] {} 토큰 정보 추출 완료 - accessToken: {}, tokenType: {}, expiresIn: {}초",
                    provider, tokenInfo.getAccessToken() != null, tokenInfo.getTokenType(), tokenInfo.getExpiresIn());
            
            // IntegrationTokenInfo에 저장
            integrationTokenInfoService.saveOrUpdateTokenInfo(user, provider.toUpperCase(), tokenInfo);
            
            log.info("[TOKEN_HANDLER] {} 토큰 정보 저장 완료 - userId: {}", provider, user.getId());
        } else {
            log.warn("[TOKEN_HANDLER] {} 토큰 정보 추출 실패 - userId: {}", provider, user.getId());
        }
    }

    /**
     * additionalParameters에서 String 값을 안전하게 추출
     */
    private String extractStringFromAdditionalParams(Map<String, Object> additionalParams, String key) {
        Object value = additionalParams.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * additionalParameters에서 Integer 값을 안전하게 추출
     */
    private Integer extractIntegerFromAdditionalParams(Map<String, Object> additionalParams, String key) {
        Object value = additionalParams.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("[TOKEN_HANDLER] {} 파싱 실패: {}", key, value);
            }
        }
        return null;
    }

    /**
     * 통합 토큰 정보 조회 결과 DTO
     */
    public static class IntegratedTokenInfo {
        private final Long userId;
        private final String principalName;
        private final String dbRefreshToken;
        private final String oauth2AccessToken;
        private final String oauth2RefreshToken;

        public IntegratedTokenInfo(Long userId, String principalName, String dbRefreshToken, 
                                  String oauth2AccessToken, String oauth2RefreshToken) {
            this.userId = userId;
            this.principalName = principalName;
            this.dbRefreshToken = dbRefreshToken;
            this.oauth2AccessToken = oauth2AccessToken;
            this.oauth2RefreshToken = oauth2RefreshToken;
        }

        // Getters
        public Long getUserId() { return userId; }
        public String getPrincipalName() { return principalName; }
        public String getDbRefreshToken() { return dbRefreshToken; }
        public String getOauth2AccessToken() { return oauth2AccessToken; }
        public String getOauth2RefreshToken() { return oauth2RefreshToken; }
    }
} 