package org.example.vibelist.global.oauth2.provider;

import java.util.Map;

/**
 * 소셜 로그인 Provider별 처리 전략 인터페이스
 */
public interface SocialProviderStrategy {
    
    /**
     * Provider 이름 반환
     */
    String getProviderName();
    
    /**
     * OAuth2 attributes에서 사용자 정보 추출
     */
    SocialUserInfo extractUserInfo(Map<String, Object> attributes);
    
    /**
     * nameAttributeKey 반환 (Spring Security OAuth2에서 사용)
     */
    String getNameAttributeKey(Map<String, Object> attributes);
    
    /**
     * Provider별 특별한 처리가 필요한지 여부
     */
    default boolean requiresSpecialTokenHandling() {
        return false;
    }
    
    /**
     * 추출된 사용자 정보를 담는 내부 클래스
     */
    class SocialUserInfo {
        private final String providerUserId;
        private final String username;
        private final String email;
        
        public SocialUserInfo(String providerUserId, String username, String email) {
            this.providerUserId = providerUserId;
            this.username = username;
            this.email = email;
        }
        
        public String getProviderUserId() { return providerUserId; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
    }
} 