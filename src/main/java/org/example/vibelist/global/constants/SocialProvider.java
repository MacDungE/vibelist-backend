package org.example.vibelist.global.constants;

import java.util.Map;

public enum SocialProvider {
    KAKAO,      // 카카오 로그인
    GOOGLE;     // 구글 로그인
    // NAVER,      // 네이버 로그인
    // FACEBOOK,   // 페이스북 로그인
    // APPLE       // 애플 로그인
    
    /**
     * OAuth2 attributes에서 소셜 제공자를 식별합니다.
     * @param attributes OAuth2 사용자 속성
     * @return 식별된 소셜 제공자, 식별할 수 없는 경우 null
     */
    public static SocialProvider fromAttributes(Map<String, Object> attributes) {
        if (attributes.containsKey("kakao_account")) {
            return KAKAO;
        } else if (attributes.containsKey("email") && attributes.get("email") != null) {
            String email = (String) attributes.get("email");
            if (email.endsWith("@gmail.com")) {
                return GOOGLE;
            }
        }
        return null;
    }
    
    /**
     * 소셜 제공자의 소문자 이름을 반환합니다.
     * @return 소문자로 변환된 제공자 이름
     */
    public String getLowerCaseName() {
        return this.name().toLowerCase();
    }
} 