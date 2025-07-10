package org.example.vibelist.global.constants;

public class TokenConstants {
    
    // 토큰 이름 상수
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";
    
    // 쿠키 이름 상수
    public static final String ACCESS_TOKEN_COOKIE = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    
    // 토큰 타입 상수
    public static final String TOKEN_TYPE = "Bearer";

    private TokenConstants() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }
} 