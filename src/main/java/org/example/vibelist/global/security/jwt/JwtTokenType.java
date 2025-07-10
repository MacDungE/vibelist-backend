package org.example.vibelist.global.security.jwt;

/**
 * JWT 토큰 타입을 정의하는 상수 클래스
 */
public final class JwtTokenType {
    
    public static final String ACCESS = "ACCESS";
    public static final String REFRESH = "REFRESH";
    
    private JwtTokenType() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }
} 