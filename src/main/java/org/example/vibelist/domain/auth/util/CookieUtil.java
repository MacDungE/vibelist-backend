package org.example.vibelist.domain.auth.util;

import jakarta.servlet.http.HttpServletResponse;
import org.example.vibelist.global.constants.TokenConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7일

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
        ResponseCookie cookie = ResponseCookie.from(TokenConstants.REFRESH_TOKEN_COOKIE, refreshToken)
            .httpOnly(true)
            .secure(true) // SameSite=None을 사용하려면 Secure=true 필수
            // .path("/")
            .maxAge(REFRESH_TOKEN_MAX_AGE)
            .sameSite("None") // 크로스-사이트 요청에서도 쿠키 전송 허용
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Refresh Token 쿠키 삭제
     */
    public void removeRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(TokenConstants.REFRESH_TOKEN_COOKIE, "")
            .httpOnly(true)
            .secure(true) // SameSite=None을 사용하려면 Secure=true 필수
            // .path("/")
            .sameSite("None") // 크로스-사이트 요청에서도 쿠키 전송 허용
            .maxAge(0) // 즉시 삭제
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * 모든 인증 쿠키 삭제
     * 현재는 리프레시 토큰 쿠키만 삭제합니다. (액세스 토큰은 더 이상 쿠키로 관리하지 않음)
     */
    public void removeAllAuthCookies(HttpServletResponse response) {
        removeRefreshTokenCookie(response);
    }
} 