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
     * 현재 환경이 프로덕션인지 확인
     *
     * @return 프로덕션 환경이면 true, 아니면 false
     */
    private boolean isProduction() {
        return "prod".equals(activeProfile);
    }

    /**
     * Refresh Token을 쿠키에 설정
     *
     * 보안을 위해 다음 속성들을 설정합니다:
     * - httpOnly: JavaScript에서 쿠키 접근 방지 (XSS 공격 방어)
     * - secure: HTTPS 연결에서만 쿠키 전송
     * - sameSite=None: 크로스-사이트 요청에서도 쿠키 전송 허용 (CORS 지원)
     *
     * @param response HttpServletResponse 객체
     * @param refreshToken 설정할 Refresh Token 값
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
    
    /**
     * Refresh Token 쿠키 삭제
     *
     * 쿠키를 삭제하기 위해 동일한 이름과 경로로 빈 값의 쿠키를 생성하고
     * maxAge를 0으로 설정하여 브라우저가 즉시 삭제하도록 합니다.
     *
     * @param response HttpServletResponse 객체
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
     * 모든 인증 쿠키 삭제
     *
     * 현재는 리프레시 토큰 쿠키만 삭제합니다.
     * (액세스 토큰은 더 이상 쿠키로 관리하지 않고 Authorization 헤더를 통해 전달됨)
     *
     * @param response HttpServletResponse 객체
     */
    public void removeAllAuthCookies(HttpServletResponse response) {
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

    /**
     * 요청에서 refresh 토큰 쿠키를 추출
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