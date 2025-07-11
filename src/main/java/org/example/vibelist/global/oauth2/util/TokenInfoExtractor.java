package org.example.vibelist.global.oauth2.util;

import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.global.constants.TokenManagementConstants;
import org.example.vibelist.global.oauth2.dto.TokenInfo;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * 토큰 정보 추출 유틸리티
 */
@Slf4j
public class TokenInfoExtractor {
    
    private TokenInfoExtractor() {
        // 유틸리티 클래스이므로 인스턴스 생성 방지
    }
    
    /**
     * OAuth2UserRequest에서 TokenInfo를 추출합니다.
     */
    public static TokenInfo extractFromUserRequest(OAuth2UserRequest userRequest) {
        if (userRequest == null || userRequest.getAccessToken() == null) {
            return null;
        }
        
        return TokenInfo.builder()
                .accessToken(userRequest.getAccessToken().getTokenValue())
                .tokenType(userRequest.getAccessToken().getTokenType().getValue())
                .expiresIn(calculateExpiresIn(userRequest))
                .scope(extractScope(userRequest))
                .additionalParameters(userRequest.getAdditionalParameters())
                .build();
    }
    
    /**
     * OAuth2AccessTokenResponse에서 TokenInfo를 추출합니다.
     */
    public static TokenInfo extractFromTokenResponse(OAuth2AccessTokenResponse tokenResponse) {
        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            return null;
        }
        
        return TokenInfo.builder()
                .accessToken(tokenResponse.getAccessToken().getTokenValue())
                .refreshToken(tokenResponse.getRefreshToken() != null ? 
                    tokenResponse.getRefreshToken().getTokenValue() : null)
                .tokenType(tokenResponse.getAccessToken().getTokenType().getValue())
                .expiresIn(calculateExpiresIn(tokenResponse))
                .scope(extractScope(tokenResponse))
                .additionalParameters(tokenResponse.getAdditionalParameters())
                .build();
    }
    
    /**
     * Provider별 토큰 정보를 additionalParameters에 추가합니다.
     */
    public static Map<String, Object> enrichAdditionalParameters(String provider, TokenInfo tokenInfo) {
        Map<String, Object> enrichedParams = new HashMap<>();
        if (tokenInfo.getAdditionalParameters() != null) {
            enrichedParams.putAll(tokenInfo.getAdditionalParameters());
        }
        
        if ("spotify".equalsIgnoreCase(provider)) {
            enrichedParams.put(TokenManagementConstants.SPOTIFY_ACCESS_TOKEN_KEY, tokenInfo.getAccessToken());
            enrichedParams.put(TokenManagementConstants.SPOTIFY_REFRESH_TOKEN_KEY, tokenInfo.getRefreshToken());
            enrichedParams.put(TokenManagementConstants.SPOTIFY_TOKEN_TYPE_KEY, tokenInfo.getTokenType());
            
            if (tokenInfo.getExpiresIn() != null) {
                enrichedParams.put(TokenManagementConstants.SPOTIFY_EXPIRES_IN_KEY, tokenInfo.getExpiresIn());
            }
            
            if (tokenInfo.getScope() != null) {
                enrichedParams.put(TokenManagementConstants.SPOTIFY_SCOPE_KEY, tokenInfo.getScope());
            }
        }
        
        return enrichedParams;
    }
    
    /**
     * 만료 시간을 계산합니다.
     */
    private static Integer calculateExpiresIn(OAuth2UserRequest userRequest) {
        if (userRequest.getAccessToken().getExpiresAt() != null && 
            userRequest.getAccessToken().getIssuedAt() != null) {
            return (int) (userRequest.getAccessToken().getExpiresAt().getEpochSecond() - 
                         userRequest.getAccessToken().getIssuedAt().getEpochSecond());
        }
        return TokenManagementConstants.DEFAULT_TOKEN_EXPIRES_IN;
    }
    
    /**
     * 만료 시간을 계산합니다.
     */
    private static Integer calculateExpiresIn(OAuth2AccessTokenResponse tokenResponse) {
        if (tokenResponse.getAccessToken().getExpiresAt() != null && 
            tokenResponse.getAccessToken().getIssuedAt() != null) {
            return (int) (tokenResponse.getAccessToken().getExpiresAt().getEpochSecond() - 
                         tokenResponse.getAccessToken().getIssuedAt().getEpochSecond());
        }
        return TokenManagementConstants.DEFAULT_TOKEN_EXPIRES_IN;
    }
    
    /**
     * Scope 정보를 추출합니다.
     */
    private static String extractScope(OAuth2UserRequest userRequest) {
        if (userRequest.getAccessToken().getScopes() != null) {
            return String.join(" ", userRequest.getAccessToken().getScopes());
        }
        return null;
    }
    
    /**
     * Scope 정보를 추출합니다.
     */
    private static String extractScope(OAuth2AccessTokenResponse tokenResponse) {
        if (tokenResponse.getAccessToken().getScopes() != null) {
            return String.join(" ", tokenResponse.getAccessToken().getScopes());
        }
        return null;
    }
} 