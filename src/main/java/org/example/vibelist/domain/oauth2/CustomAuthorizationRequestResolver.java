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
 * OAuth2 Authorization Request 커스터마이징
 * - Spotify refresh token 확보 최적화
 * - Integration 요청 처리 간소화
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
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(authorizationRequest);
        
        // Spotify 전용 설정
        if (SocialProviderConstants.SPOTIFY_LOWER.equals(registrationId)) {
            Map<String, Object> additionalParameters = new HashMap<>(authorizationRequest.getAdditionalParameters());
            additionalParameters.put("show_dialog", "true");
            additionalParameters.put("access_type", "offline");
            builder.additionalParameters(additionalParameters);
            
            log.info("[OAUTH2_RESOLVER] Spotify 파라미터 추가 - show_dialog: true, access_type: offline");
        }
        
        // Integration 요청 처리 (간소화)
        String integrationUserId = request.getParameter("integration_user_id");
        if (integrationUserId != null) {
            // state에 integration 정보 직접 포함
            String newState = authorizationRequest.getState() + ":integration:" + integrationUserId;
            builder.state(newState);
            
            log.info("[OAUTH2_RESOLVER] Integration 요청 감지 - userId: {}, newState: {}", integrationUserId, newState);
        }

        return builder.build();
    }

    /**
     * Authorization Request에서 registration ID 추출
     */
    private String extractRegistrationId(OAuth2AuthorizationRequest authorizationRequest) {
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
        
        return "";
    }
} 