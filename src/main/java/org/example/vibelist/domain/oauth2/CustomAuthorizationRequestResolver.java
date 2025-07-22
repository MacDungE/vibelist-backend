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
import jakarta.servlet.http.HttpSession;

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
        log.info("[OAUTH2_RESOLVER] Authorization Request 해결 시작 - URI: {}, Method: {}", 
                request.getRequestURI(), request.getMethod());
        log.info("[OAUTH2_RESOLVER] 요청 URL: {}, 쿼리 스트링: {}", 
                request.getRequestURL(), request.getQueryString());
        
        // 모든 쿼리 파라미터 로깅
        request.getParameterMap().forEach((key, values) -> {
            log.info("[OAUTH2_RESOLVER] 쿼리 파라미터 - {}: {}", key, String.join(", ", values));
        });
        
        // integration_user_id 파라미터가 있는 경우 특별 처리
        String integrationUserId = request.getParameter("integration_user_id");
        if (integrationUserId != null) {
            log.info("[OAUTH2_RESOLVER] Integration 요청 감지 - userId: {}", integrationUserId);
            
            // 세션에 Integration 정보 저장
            HttpSession session = request.getSession();
            session.setAttribute("oauth2_integration_user_id", integrationUserId);
            session.setAttribute("oauth2_integration_timestamp", System.currentTimeMillis());
            session.setMaxInactiveInterval(300); // 5분 만료
            
            log.info("[OAUTH2_RESOLVER] 세션에 Integration 정보 저장 완료 - 세션 ID: {}", session.getId());
        }
        
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        
        if (authorizationRequest == null) {
            log.error("[OAUTH2_RESOLVER] 기본 resolver에서 null 반환 - URI: {}", request.getRequestURI());
            log.error("[OAUTH2_RESOLVER] 요청 파라미터: {}", request.getQueryString());
            return null;
        }
        
        log.info("[OAUTH2_RESOLVER] 기본 Authorization Request 생성됨 - Client ID: {}, Authorization URI: {}", 
                authorizationRequest.getClientId(), authorizationRequest.getAuthorizationUri());
        
        return customizeAuthorizationRequest(authorizationRequest, request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        log.info("[OAUTH2_RESOLVER] Authorization Request 해결 시작 (clientRegistrationId) - URI: {}, Client ID: {}", 
                request.getRequestURI(), clientRegistrationId);
        
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        
        if (authorizationRequest == null) {
            log.error("[OAUTH2_RESOLVER] 기본 resolver에서 null 반환 (clientRegistrationId) - URI: {}, Client ID: {}", 
                    request.getRequestURI(), clientRegistrationId);
            return null;
        }
        
        log.info("[OAUTH2_RESOLVER] 기본 Authorization Request 생성됨 (clientRegistrationId) - Client ID: {}, Authorization URI: {}", 
                authorizationRequest.getClientId(), authorizationRequest.getAuthorizationUri());
        
        return customizeAuthorizationRequest(authorizationRequest, request);
    }

    /**
     * Provider별로 Authorization Request를 커스터마이징
     */
    private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request) {
        if (authorizationRequest == null) {
            log.error("[OAUTH2_RESOLVER] Authorization Request가 null입니다");
            return null;
        }

        String registrationId = extractRegistrationId(authorizationRequest);
        log.info("[OAUTH2_RESOLVER] Provider 식별: {}", registrationId);
        
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(authorizationRequest);
        
        // Spotify 전용 설정
        if (SocialProviderConstants.SPOTIFY_LOWER.equals(registrationId)) {
            Map<String, Object> additionalParameters = new HashMap<>(authorizationRequest.getAdditionalParameters());
            additionalParameters.put("show_dialog", "true");
            additionalParameters.put("access_type", "offline");
            builder.additionalParameters(additionalParameters);
            
            log.info("[OAUTH2_RESOLVER] Spotify 파라미터 추가 - show_dialog: true, access_type: offline");
            log.info("[OAUTH2_RESOLVER] Spotify 추가 파라미터: {}", additionalParameters);
        }
        
        // Integration 요청 처리 (이미 resolve 메서드에서 처리됨)
        String integrationUserId = request.getParameter("integration_user_id");
        if (integrationUserId != null) {
            log.info("[OAUTH2_RESOLVER] Integration 요청 확인 - userId: {}", integrationUserId);
        } else {
            log.info("[OAUTH2_RESOLVER] 일반 OAuth2 로그인 요청");
        }

        OAuth2AuthorizationRequest customizedRequest = builder.build();
        log.info("[OAUTH2_RESOLVER] 커스터마이징된 Authorization Request 생성 완료 - Authorization URI: {}", 
                customizedRequest.getAuthorizationUri());
        
        return customizedRequest;
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