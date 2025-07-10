package org.example.vibelist.global.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.example.vibelist.global.constants.TokenConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {
    
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7일
    private static final int ACCESS_TOKEN_MAX_AGE = 30 * 60; // 30분
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    /**
     * 현재 환경이 프로덕션인지 확인
     */
    private boolean isProduction() {
        return "prod".equals(activeProfile);
    }
    
    /**
     * Refresh Token을 쿠키에 설정
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshTokenCookie = new Cookie(TokenConstants.REFRESH_TOKEN_COOKIE, refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(isProduction()); // 프로덕션에서만 HTTPS
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
        response.addCookie(refreshTokenCookie);
    }
    
    /**
     * Access Token을 쿠키에 설정
     */
    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie accessTokenCookie = new Cookie(TokenConstants.ACCESS_TOKEN_COOKIE, accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(isProduction()); // 프로덕션에서만 HTTPS
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(ACCESS_TOKEN_MAX_AGE);
        response.addCookie(accessTokenCookie);
    }
    
    /**
     * Refresh Token 쿠키 삭제
     */
    public void removeRefreshTokenCookie(HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie(TokenConstants.REFRESH_TOKEN_COOKIE, null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(isProduction());
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0); // 즉시 삭제
        response.addCookie(refreshTokenCookie);
    }
    
    /**
     * Access Token 쿠키 삭제
     */
    public void removeAccessTokenCookie(HttpServletResponse response) {
        Cookie accessTokenCookie = new Cookie(TokenConstants.ACCESS_TOKEN_COOKIE, null);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(isProduction());
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0); // 즉시 삭제
        response.addCookie(accessTokenCookie);
    }
    
    /**
     * 모든 인증 쿠키 삭제
     */
    public void removeAllAuthCookies(HttpServletResponse response) {
        removeAccessTokenCookie(response);
        removeRefreshTokenCookie(response);
    }
    
    /**
     * 커스텀 쿠키 생성
     */
    public Cookie createCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction());
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        return cookie;
    }
} 