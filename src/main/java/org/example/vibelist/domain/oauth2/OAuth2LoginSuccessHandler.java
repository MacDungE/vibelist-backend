package org.example.vibelist.domain.oauth2;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.auth.util.CookieUtil;
import org.example.vibelist.domain.integration.service.IntegrationTokenInfoService;
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.domain.user.service.UserService;
import org.example.vibelist.global.constants.TokenConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    // 로그인 동작을 커스텀으로 구현하고 싶을때 사용하는 인터페이스

    private final IntegrationTokenInfoService integrationTokenInfoService;
    private final UserService userService;
    private final CookieUtil cookieUtil;

    @Value("${frontend.login.url}")
    private String loginUrl;

    @Value("${frontend.callback.url}")
    private String callbackUrl;

    // Oauth2 로그인 성공시 트리거 되는것
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        log.info("[OAuth2_LOG] OAuth2 로그인 성공 핸들러 시작");
        log.info("[OAuth2_LOG] Request URI: {}", request.getRequestURI());
        log.info("[OAuth2_LOG] Authentication: {}", authentication);
        log.info("[OAuth2_LOG] Authentication Principal: {}", authentication.getPrincipal());
        log.info("[OAuth2_LOG] Authentication Authorities: {}", authentication.getAuthorities());

        try {
            DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();
            String provider = (String) attributes.get("provider");
            
            // OAuth2UserProcessor에서 설정한 isIntegrationRequest 값 확인
            Object isIntegrationObj = attributes.get("isIntegrationRequest");
            boolean isIntegrationRequest = Boolean.TRUE.equals(isIntegrationObj);
            Long userId = (Long) attributes.get("userId");
            boolean isNewUser = Boolean.TRUE.equals(attributes.get("isNewUser"));
            
            log.info("[OAuth2_LOG] 처리 모드 확인 - isIntegration: {} (원본: {}), userId: {}, provider: {}, isNewUser: {}", 
                    isIntegrationRequest, isIntegrationObj, userId, provider, isNewUser);
            log.info("[OAuth2_LOG] 전체 attributes 키: {}", attributes.keySet());

            // 연동 요청인 경우 별도 처리
            if (isIntegrationRequest && userId != null) {
                handleIntegrationRequest(userId, provider, attributes, response);
                return;
            }

            // 일반 로그인 처리 (기존 로직)
            handleRegularLogin(attributes, response);
            
        } catch (Exception e) {
            log.error("[OAuth2_LOG] OAuth2 로그인 성공 처리 중 오류 발생", e);
            response.sendRedirect(loginUrl + "?error=oauth2_error");
        }
    }

    /**
     * 연동 요청 처리 - 토큰 정보만 저장하고 쿠키 설정하지 않음
     */
    private void handleIntegrationRequest(Long userId, String provider, Map<String, Object> attributes, HttpServletResponse response) 
            throws IOException {
        log.info("[OAuth2_LOG] 연동 요청 처리 시작 - userId: {}, provider: {}", userId, provider);

        try {
            // 사용자 조회
            User user = userService.findUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            // 토큰 정보 추출
            String accessToken = (String) attributes.get(TokenConstants.ACCESS_TOKEN);
            String refreshToken = (String) attributes.get(TokenConstants.REFRESH_TOKEN);
            String tokenType = (String) attributes.get("tokenType");
            Integer expiresIn = (Integer) attributes.get("expiresIn");
            String scope = (String) attributes.get("scope");

            log.info("[OAuth2_LOG] 연동 토큰 정보 - AccessToken 존재: {}, RefreshToken 존재: {}", 
                    accessToken != null, refreshToken != null);

            if (accessToken == null) {
                log.error("[OAuth2_LOG] 연동 처리 실패: AccessToken이 없습니다");
                response.sendRedirect(callbackUrl + "?integration=error&reason=no_token");
                return;
            }

            // IntegrationTokenInfo에 토큰 정보 저장
            integrationTokenInfoService.saveOrUpdateTokenInfo(
                    user, provider, accessToken, refreshToken, tokenType, expiresIn, scope
            );

            log.info("[OAuth2_LOG] 연동 토큰 정보 저장 완료 - provider: {}", provider);

            // 성공 리다이렉트 (쿠키 설정 없이)
            String redirectUrl = callbackUrl + "?integration=success&provider=" + provider + "&userId=" + userId;
            log.info("[OAuth2_LOG] 연동 완료 - 메인 페이지로 리다이렉트: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("[OAuth2_LOG] 연동 처리 중 오류 발생 - userId: {}, provider: {}", userId, provider, e);
            response.sendRedirect(callbackUrl + "?integration=error&reason=save_failed");
        }
    }

    /**
     * 일반 로그인 처리 - 기존 로직 유지
     */
    private void handleRegularLogin(Map<String, Object> attributes, HttpServletResponse response) 
            throws IOException {
        log.info("[OAuth2_LOG] 일반 로그인 처리 시작");

        String accessToken = (String) attributes.get(TokenConstants.ACCESS_TOKEN);
        String refreshToken = (String) attributes.get(TokenConstants.REFRESH_TOKEN);
        String name = (String) attributes.get("name");
        String provider = (String) attributes.get("provider");

        log.info("[OAuth2_LOG] 소셜 로그인 시도한 이름 = {}", name);
        log.info("[OAuth2_LOG] AccessToken 존재: {}", accessToken != null);
        log.info("[OAuth2_LOG] RefreshToken 존재: {}", refreshToken != null);
        log.info("[OAuth2_LOG] 전체 attributes 내용: {}", attributes);

        // 사용자 ID를 안전하게 꺼내기 (null 체크 및 타입 캐스팅)
        String id = null;
        Object idObj = attributes.get("userId"); // OAuth2UserProcessor에서 설정한 실제 사용자 ID 사용
        if (idObj != null) {
            // 실제 사용자 ID는 Long 타입이므로 String으로 변환
            id = idObj.toString();
            log.info("[OAuth2_LOG] 사용자 ID: {}", id);
        } else {
            log.warn("[OAuth2_LOG] 사용자 ID가 없습니다!");
        }

        // 신규 사용자인지 확인
        boolean isNewUser = Boolean.TRUE.equals(attributes.get("isNewUser"));
        String tempUserId = null;
        Object tempUserIdObj = attributes.get("tempUserId");
        if (tempUserIdObj != null) {
            tempUserId = tempUserIdObj.toString();
        }
        
        log.info("[OAuth2_LOG] 신규 사용자 여부: {}", isNewUser);
        log.info("[OAuth2_LOG] 임시 사용자 ID: {}", tempUserId);
        log.info("[OAuth2_LOG] Provider: {}", provider);

        // 토큰이 없는 경우 처리
        if (accessToken == null || refreshToken == null) {
            log.error("[OAuth2_LOG] 토큰이 없습니다. accessToken: {}, refreshToken: {}", accessToken, refreshToken);
            log.error("[OAuth2_LOG] 토큰 생성 실패로 인한 로그인 실패");
            response.sendRedirect(loginUrl + "?error=token_missing");
            return;
        }

        // 새로운 토큰 전달방식 - 리프레시 토큰만 HTTP-only 쿠키로 설정
        // 액세스 토큰은 프론트엔드에서 별도 API를 통해 획득하도록 변경
        cookieUtil.setRefreshTokenCookie(response, refreshToken);

        // reseponse에  set-cookie 헤더에 값을 것을 확인 할수 있는 log
        log.info("[OAuth2_LOG] response.getHeaderNames() = {}", response.getHeaderNames());
        
        log.info("[OAuth2_LOG] 리프레시 토큰 쿠키 설정 완료 (액세스 토큰은 별도 API로 제공)");

        // 리다이렉트 URL 결정
        String redirectUrl;
        if (isNewUser) {
            // 신규 사용자: 사용자명 설정 페이지로 리다이렉트
            redirectUrl = callbackUrl + "?isNewUser=true";
            if (tempUserId != null) {
                redirectUrl += "&tempUserId=" + tempUserId;
            }
            if (provider != null) {
                redirectUrl += "&provider=" + provider;
            }
            log.info("[OAuth2_LOG] 신규 사용자 - 사용자명 설정 페이지로 리다이렉트: {}", redirectUrl);
        } else {
            // 기존 사용자의 일반 로그인: 메인 페이지로 리다이렉트
            redirectUrl = callbackUrl;
            if (accessToken != null) {
                redirectUrl += "?accessToken=" + accessToken;
            }
            log.info("[OAuth2_LOG] 기존 사용자 - 메인 페이지로 리다이렉트: {}", redirectUrl);
        }
        
        response.sendRedirect(redirectUrl);
    }
}

