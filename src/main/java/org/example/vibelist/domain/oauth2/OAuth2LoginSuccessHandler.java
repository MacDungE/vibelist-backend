package org.example.vibelist.domain.oauth2;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.global.constants.TokenConstants;
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

    // Oauth2 로그인 성공시 트리거 되는것
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        log.info("[OAuth2_LOG] OAuth2 로그인 성공 핸들러 시작");
        log.info("[OAuth2_LOG] Request URI: {}", request.getRequestURI());
        log.info("[OAuth2_LOG] Authentication: {}", authentication);

        try {
            DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();

            log.info("[OAuth2_LOG] OAuth2User attributes: {}", attributes);

            String accessToken = (String) attributes.get(TokenConstants.ACCESS_TOKEN);
            String refreshToken = (String) attributes.get(TokenConstants.REFRESH_TOKEN);
            String name = (String) attributes.get("name");

            log.info("[OAuth2_LOG] 소셜 로그인 시도한 이름 = {}", name);
            log.info("[OAuth2_LOG] AccessToken 존재: {}", accessToken != null);
            log.info("[OAuth2_LOG] RefreshToken 존재: {}", refreshToken != null);

            // 사용자 ID를 안전하게 꺼내기 (null 체크 및 타입 캐스팅)
            String id = null;
            Object idObj = attributes.get("id");
            if (idObj != null) {
                // 소셜 플랫폼의 ID는 Long 범위를 초과할 수 있으므로 String으로 처리
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
            String provider = (String) attributes.get("provider");
            
            log.info("[OAuth2_LOG] 신규 사용자 여부: {}", isNewUser);
            log.info("[OAuth2_LOG] 임시 사용자 ID: {}", tempUserId);
            log.info("[OAuth2_LOG] Provider: {}", provider);

            // 토큰이 없는 경우 처리
            if (accessToken == null || refreshToken == null) {
                log.error("[OAuth2_LOG] 토큰이 없습니다. accessToken: {}, refreshToken: {}", accessToken, refreshToken);
                response.sendRedirect("/login.html?error=token_missing");
                return;
            }

            // 토큰 전달방식 - HttpOnly 쿠키로 전달
            Cookie accessTokenCookie = new Cookie(TokenConstants.ACCESS_TOKEN_COOKIE, accessToken);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(60 * 30); // 30분짜리 액세스 토큰
            response.addCookie(accessTokenCookie);

            Cookie refreshTokenCookie = new Cookie(TokenConstants.REFRESH_TOKEN_COOKIE, refreshToken);
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge(60 * 60 * 24 * 7); // 7일짜리 리프레시 토큰
            response.addCookie(refreshTokenCookie);

            log.info("[OAuth2_LOG] 쿠키 설정 완료");

            // 신규 사용자인지에 따라 다른 페이지로 리다이렉트
            String redirectUrl;
            if (isNewUser) {
                // 신규 사용자: 사용자명 설정 페이지로 리다이렉트
                redirectUrl = "/social-signup.html";
                if (tempUserId != null) {
                    redirectUrl += "?tempUserId=" + tempUserId;
                }
                if (provider != null) {
                    redirectUrl += (redirectUrl.contains("?") ? "&" : "?") + "provider=" + provider;
                }
                log.info("[OAuth2_LOG] 신규 사용자 - 사용자명 설정 페이지로 리다이렉트: {}", redirectUrl);
            } else {
                // 기존 사용자: 메인 페이지로 리다이렉트
                redirectUrl = "/main.html";
                if (id != null) {
                    redirectUrl += "?id=" + id;
                }
                log.info("[OAuth2_LOG] 기존 사용자 - 메인 페이지로 리다이렉트: {}", redirectUrl);
            }
            
            response.sendRedirect(redirectUrl);
            
        } catch (Exception e) {
            log.error("[OAuth2_LOG] OAuth2 로그인 성공 처리 중 오류 발생", e);
            response.sendRedirect("/login.html?error=oauth2_error");
        }
    }
}
