package org.example.vibelist.domain.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.global.constants.SocialProviderConstants;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Spotify OAuth2에서 Refresh Token을 확실히 받기 위한 커스텀 Authorization Request Resolver
 */
@Slf4j
@Component
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        return customizeAuthorizationRequest(authorizationRequest, request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        return customizeAuthorizationRequest(authorizationRequest, request);
    }

    /**
     * Provider별로 Authorization Request를 커스터마이징
     */
    private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request) {
        if (authorizationRequest == null) {
            return null;
        }

        String registrationId = extractRegistrationId(authorizationRequest);
        
        if (SocialProviderConstants.SPOTIFY_LOWER.equals(registrationId)) {
            return customizeSpotifyAuthorizationRequest(authorizationRequest, request);
        }

        return authorizationRequest;
    }

    /**
     * Spotify용 Authorization Request 커스터마이징
     * - show_dialog=true: 사용자가 매번 동의하도록 강제 (refresh token 확률 증가)
     * - access_type=offline: 오프라인 액세스를 위한 refresh token 요청
     * - state: 사용자 정의 state 파라미터 처리 (integration 요청 구분)
     */
    private OAuth2AuthorizationRequest customizeSpotifyAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request) {
        log.info("[OAUTH2_RESOLVER] Spotify Authorization Request 커스터마이징 시작");

        Map<String, Object> additionalParameters = new HashMap<>(authorizationRequest.getAdditionalParameters());
        
        // Spotify에서 refresh token을 확실히 받기 위한 파라미터 추가
        additionalParameters.put("show_dialog", "true");
        additionalParameters.put("access_type", "offline");
        
        log.info("[OAUTH2_RESOLVER] Spotify 추가 파라미터 설정 완료 - show_dialog: true, access_type: offline");

        // HttpServletRequest에서 state 파라미터 확인 (integration 요청인지 판단)
        String customState = request.getParameter("state");
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additionalParameters);
                
        if (customState != null && customState.startsWith("integration")) {
            // 기존 Spring Security state와 사용자 정의 state를 결합
            String combinedState = authorizationRequest.getState() + ":" + customState;
            builder.state(combinedState);
            log.info("[OAUTH2_RESOLVER] Integration state 감지 - 결합된 state: {}", combinedState);
        }

        return builder.build();
    }

    /**
     * Authorization Request에서 registration ID 추출
     */
    private String extractRegistrationId(OAuth2AuthorizationRequest authorizationRequest) {
        // clientId나 authorizationUri에서 registration ID를 추정
        String authorizationUri = authorizationRequest.getAuthorizationUri();
        
        if (authorizationUri.contains("accounts.spotify.com")) {
            return SocialProviderConstants.SPOTIFY_LOWER;
        }
        if (authorizationUri.contains("kauth.kakao.com")) {
            return SocialProviderConstants.KAKAO_LOWER;
        }
        if (authorizationUri.contains("accounts.google.com")) {
            return SocialProviderConstants.GOOGLE_LOWER;
        }
        
        // 기본값으로 빈 문자열 반환
        return "";
    }
} 