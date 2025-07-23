package org.example.vibelist.domain.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.vibelist.global.constants.TokenConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 쿠키를 통한 인증 토큰 관리 유틸리티 클래스
 *
 * Refresh Token을 HttpOnly, Secure 쿠키로 관리하여 보안을 강화합니다.
 * CORS 환경에서의 크로스-사이트 요청을 지원하기 위해 SameSite=None 설정을 사용합니다.
 */
@Component
public class CookieUtil {

    /** Refresh Token의 유효 기간 (7일) */
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7일

    /** 현재 활성화된 Spring 프로파일 */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Determines whether the current environment is production.
     *
     * @return {@code true} if the active profile is "prod"; {@code false} otherwise.
     */
    private boolean isProduction() {
        return "prod".equals(activeProfile);
    }

    /****
     * Sets the refresh token as an HTTP-only, secure cookie with SameSite=None for cross-site requests.
     *
     * The cookie is added to the response with a 7-day expiration, root path, and attributes to enhance security and support CORS.
     *
     * @param response the HTTP response to which the refresh token cookie will be added
     * @param refreshToken the refresh token value to store in the cookie
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(TokenConstants.REFRESH_TOKEN_COOKIE, refreshToken)
            .httpOnly(true)
            .secure(true) // SameSite=None을 사용하려면 Secure=true 필수
            .path("/")  // "/"로 설정하면 해당 도메인의 모든 경로에서 쿠키에 접근 가능합니다
            .maxAge(REFRESH_TOKEN_MAX_AGE)
            .sameSite("None") // 크로스-사이트 요청에서도 쿠키 전송 허용
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
    
    /****
     * Removes the refresh token cookie from the client's browser.
     *
     * Creates a cookie with the same name and path as the refresh token cookie, sets its value to empty and max age to 0, instructing the browser to delete it immediately. The cookie is set as HttpOnly, Secure, and SameSite=None to match the original security attributes.
     *
     * @param response the HTTP response to which the removal cookie will be added
     */
    public void removeRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(TokenConstants.REFRESH_TOKEN_COOKIE, "")
            .httpOnly(true)
            .secure(true) // SameSite=None을 사용하려면 Secure=true 필수
            .path("/")
            .sameSite("None") // 크로스-사이트 요청에서도 쿠키 전송 허용
            .maxAge(0) // 즉시 삭제
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Removes all authentication-related cookies from the HTTP response.
     *
     * Currently, this only deletes the refresh token cookie. Access tokens are not managed via cookies and are sent through the Authorization header.
     *
     * @param response the HTTP response to which the cookie removal will be applied
     */
    public void removeAllAuthCookies(HttpServletResponse response) {
        removeRefreshTokenCookie(response);
    }
    
    /**
     * Creates a new HTTP cookie with the specified name, value, and maximum age.
     *
     * The cookie is set as HttpOnly, uses the root path ("/"), and is marked as secure if running in a production environment.
     *
     * @param name   the name of the cookie
     * @param value  the value to assign to the cookie
     * @param maxAge the maximum age of the cookie in seconds
     * @return the configured Cookie object
     */
    public Cookie createCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction());
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    /**
     * Retrieves the value of the refresh token cookie from the given HTTP request.
     *
     * @param request the HTTP servlet request containing cookies
     * @return the refresh token value if present; otherwise, {@code null}
     */
    public String resolveRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (TokenConstants.REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
} 