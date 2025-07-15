package org.example.vibelist.domain.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.auth.entity.Auth;
import org.example.vibelist.domain.auth.repository.AuthRepository;
import org.example.vibelist.global.constants.SocialProviderConstants;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * OAuth2 토큰 관리를 위한 서비스
 * OAuth2AuthorizedClientService를 통해 Refresh Token에 접근
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TokenService {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final AuthRepository authRepository;

    /**
     * Spotify Refresh Token을 OAuth2AuthorizedClient에서 조회
     */
    public Optional<String> getSpotifyRefreshToken(String principalName) {
        try {
            log.info("[OAUTH2_TOKEN] Spotify Refresh Token 조회 시도 - principalName: {}", principalName);
            
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                SocialProviderConstants.SPOTIFY_LOWER, principalName);
            
            if (authorizedClient != null && authorizedClient.getRefreshToken() != null) {
                String refreshToken = authorizedClient.getRefreshToken().getTokenValue();
                log.info("[OAUTH2_TOKEN] Spotify Refresh Token 획득 성공");
                return Optional.of(refreshToken);
            } else {
                log.warn("[OAUTH2_TOKEN] Spotify Refresh Token이 없습니다. authorizedClient: {}, refreshToken: {}", 
                    authorizedClient != null, 
                    authorizedClient != null ? authorizedClient.getRefreshToken() != null : false);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("[OAUTH2_TOKEN] Spotify Refresh Token 조회 중 오류 발생", e);
            return Optional.empty();
        }
    }

    /**
     * Spotify Access Token을 OAuth2AuthorizedClient에서 조회
     */
    public Optional<String> getSpotifyAccessToken(String principalName) {
        try {
            log.info("[OAUTH2_TOKEN] Spotify Access Token 조회 시도 - principalName: {}", principalName);
            
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                SocialProviderConstants.SPOTIFY_LOWER, principalName);
            
            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                String accessToken = authorizedClient.getAccessToken().getTokenValue();
                log.info("[OAUTH2_TOKEN] Spotify Access Token 획득 성공");
                return Optional.of(accessToken);
            } else {
                log.warn("[OAUTH2_TOKEN] Spotify Access Token이 없습니다.");
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("[OAUTH2_TOKEN] Spotify Access Token 조회 중 오류 발생", e);
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