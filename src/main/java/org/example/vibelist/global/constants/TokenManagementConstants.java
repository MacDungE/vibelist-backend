package org.example.vibelist.global.constants;

/**
 * 토큰 관리 관련 공통 상수
 */
public class TokenManagementConstants {
    
    // 토큰 타입
    public static final String BEARER_TOKEN_TYPE = "Bearer";
    
    // 토큰 응답 키
    public static final String ACCESS_TOKEN_KEY = "access_token";
    public static final String REFRESH_TOKEN_KEY = "refresh_token";
    public static final String TOKEN_TYPE_KEY = "token_type";
    public static final String EXPIRES_IN_KEY = "expires_in";
    public static final String SCOPE_KEY = "scope";
    
    // Provider별 토큰 키 (additionalParameters용)
    public static final String SPOTIFY_ACCESS_TOKEN_KEY = "spotify_access_token";
    public static final String SPOTIFY_REFRESH_TOKEN_KEY = "spotify_refresh_token";
    public static final String SPOTIFY_TOKEN_TYPE_KEY = "spotify_token_type";
    public static final String SPOTIFY_EXPIRES_IN_KEY = "spotify_expires_in";
    public static final String SPOTIFY_SCOPE_KEY = "spotify_scope";
    
    // 기본 토큰 만료시간 (초)
    public static final int DEFAULT_TOKEN_EXPIRES_IN = 3600;
    
    private TokenManagementConstants() {
        // 유틸리티 클래스이므로 인스턴스 생성 방지
    }
} 