package org.example.vibelist.global.oauth2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.global.constants.SocialProviderConstants;
import org.example.vibelist.global.constants.TokenManagementConstants;
import org.example.vibelist.global.integration.service.IntegrationTokenInfoService;
import org.example.vibelist.global.oauth2.dto.TokenInfo;
import org.example.vibelist.global.oauth2.util.TokenInfoExtractor;
import org.example.vibelist.global.user.entity.User;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * OAuth2 토큰 처리 핸들러
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TokenHandler {
    
    private final IntegrationTokenInfoService integrationTokenInfoService;
    
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
     * Spotify 전용 토큰 정보 저장
     */
    private void saveSpotifyIntegrationTokenInfo(User user, String provider, OAuth2UserRequest userRequest) {
        // additionalParameters에서 Spotify 전용 토큰 정보 추출
        Map<String, Object> additionalParams = userRequest.getAdditionalParameters();
        
        String spotifyAccessToken = (String) additionalParams.get(TokenManagementConstants.SPOTIFY_ACCESS_TOKEN_KEY);
        String spotifyRefreshToken = (String) additionalParams.get(TokenManagementConstants.SPOTIFY_REFRESH_TOKEN_KEY);
        String spotifyTokenType = (String) additionalParams.get(TokenManagementConstants.SPOTIFY_TOKEN_TYPE_KEY);
        Integer expiresIn = extractIntegerFromAdditionalParams(additionalParams, TokenManagementConstants.SPOTIFY_EXPIRES_IN_KEY);
        String spotifyScope = (String) additionalParams.get(TokenManagementConstants.SPOTIFY_SCOPE_KEY);
        
        log.info("[TOKEN_HANDLER] Spotify 토큰 정보 - accessToken: {}, refreshToken: {}, tokenType: {}, expiresIn: {}초, scope: {}", 
                spotifyAccessToken != null, spotifyRefreshToken != null, spotifyTokenType, expiresIn, spotifyScope);
        
        // TokenInfo 생성
        TokenInfo tokenInfo = TokenInfo.builder()
                .accessToken(spotifyAccessToken)
                .refreshToken(spotifyRefreshToken)
                .tokenType(spotifyTokenType != null ? spotifyTokenType : TokenManagementConstants.BEARER_TOKEN_TYPE)
                .expiresIn(expiresIn)
                .scope(spotifyScope)
                .additionalParameters(additionalParams)
                .build();
        
        // IntegrationTokenInfo에 저장
        integrationTokenInfoService.saveOrUpdateTokenInfo(user, provider.toUpperCase(), tokenInfo);
        
        log.info("[TOKEN_HANDLER] Spotify 토큰 정보 저장 완료 - userId: {}, provider: {}", user.getId(), provider);
    }
    
    /**
     * 일반 소셜 로그인 토큰 정보 저장
     */
    private void saveGeneralIntegrationTokenInfo(User user, String provider, OAuth2UserRequest userRequest) {
        // TokenInfoExtractor를 사용하여 토큰 정보 추출
        TokenInfo tokenInfo = TokenInfoExtractor.extractFromUserRequest(userRequest);
        
        if (tokenInfo != null) {
            log.info("[TOKEN_HANDLER] 소셜 로그인 토큰 정보 추출 완료 - provider: {}, accessToken: {}, tokenType: {}, expiresIn: {}초",
                    provider, tokenInfo.getAccessToken() != null, tokenInfo.getTokenType(), tokenInfo.getExpiresIn());
            
            // IntegrationTokenInfo에 저장
            integrationTokenInfoService.saveOrUpdateTokenInfo(user, provider.toUpperCase(), tokenInfo);
            
            log.info("[TOKEN_HANDLER] 소셜 로그인 토큰 정보 저장 완료 - userId: {}, provider: {}", user.getId(), provider);
        } else {
            log.warn("[TOKEN_HANDLER] 토큰 정보 추출 실패 - userId: {}, provider: {}", user.getId(), provider);
        }
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
} 