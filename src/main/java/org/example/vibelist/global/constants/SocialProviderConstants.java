package org.example.vibelist.global.constants;

import java.util.Map;

/**
 * 소셜 로그인 제공자 상수 클래스
 * enum 대신 상수를 사용하여 가변성을 높이고 중복을 줄입니다.
 */
public final class SocialProviderConstants {
    
    // 소셜 제공자 상수
    public static final String KAKAO = "KAKAO";
    public static final String GOOGLE = "GOOGLE";
    public static final String SPOTIFY = "SPOTIFY";
    public static final String NAVER = "NAVER";
    public static final String FACEBOOK = "FACEBOOK";
    public static final String APPLE = "APPLE";
    
    // 소셜 제공자 소문자 상수 (OAuth2 registration ID와 매칭)
    public static final String KAKAO_LOWER = "kakao";
    public static final String GOOGLE_LOWER = "google";
    public static final String SPOTIFY_LOWER = "spotify";
    public static final String NAVER_LOWER = "naver";
    public static final String FACEBOOK_LOWER = "facebook";
    public static final String APPLE_LOWER = "apple";
    
    // OAuth2 로그인 URL 경로
    public static final String OAUTH2_AUTHORIZATION_PATH = "/oauth2/authorization/";
    
    private SocialProviderConstants() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }
    
    /**
     * OAuth2 attributes에서 소셜 제공자를 식별합니다.
     * @param attributes OAuth2 사용자 속성
     * @return 식별된 소셜 제공자, 식별할 수 없는 경우 null
     */
    public static String fromAttributes(Map<String, Object> attributes) {
        if (attributes.containsKey("kakao_account")) {
            return KAKAO;
        } else if (attributes.containsKey("email") && attributes.get("email") != null) {
            String email = (String) attributes.get("email");
            if (email.endsWith("@gmail.com")) {
                return GOOGLE;
            }
        } else if (attributes.containsKey("id") && attributes.get("type") != null) {
            // Spotify는 id와 type 속성을 가짐
            String type = (String) attributes.get("type");
            if ("user".equals(type)) {
                return SPOTIFY;
            }
        }
        return null;
    }
    
    /**
     * 소셜 제공자의 소문자 이름을 반환합니다.
     * @param provider 대문자 provider 상수
     * @return 소문자로 변환된 제공자 이름
     */
    public static String getLowerCaseName(String provider) {
        if (provider == null) {
            return null;
        }
        return provider.toLowerCase();
    }
    
    /**
     * 소셜 제공자의 OAuth2 로그인 URL을 반환합니다.
     * @param provider 소문자 provider 이름
     * @return OAuth2 로그인 URL
     */
    public static String getOAuth2LoginUrl(String provider) {
        return OAUTH2_AUTHORIZATION_PATH + provider;
    }
    
    /**
     * 유효한 소셜 제공자인지 확인합니다.
     * @param provider 확인할 provider 문자열
     * @return 유효한 provider인 경우 true
     */
    public static boolean isValidProvider(String provider) {
        if (provider == null) {
            return false;
        }
        return KAKAO.equals(provider) || GOOGLE.equals(provider) || 
               SPOTIFY.equals(provider) || NAVER.equals(provider) || 
               FACEBOOK.equals(provider) || APPLE.equals(provider);
    }
    
    /**
     * 지원되는 모든 소셜 제공자 목록을 반환합니다.
     * @return 지원되는 provider 배열
     */
    public static String[] getSupportedProviders() {
        return new String[]{KAKAO, GOOGLE, SPOTIFY, NAVER, FACEBOOK, APPLE};
    }
} 