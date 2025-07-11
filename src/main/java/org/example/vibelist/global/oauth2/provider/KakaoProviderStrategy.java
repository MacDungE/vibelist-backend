package org.example.vibelist.global.oauth2.provider;

import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.global.constants.SocialProviderConstants;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kakao OAuth2 Provider 전략 구현체
 */
@Slf4j
@Component
public class KakaoProviderStrategy implements SocialProviderStrategy {
    
    @Override
    public String getProviderName() {
        return SocialProviderConstants.KAKAO;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public SocialUserInfo extractUserInfo(Map<String, Object> attributes) {
        String providerUserId = attributes.get("id").toString();
        
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        String username = (String) profile.get("nickname");
        
        log.info("[OAuth2_LOG] 카카오 사용자 정보 - id: {}, email: {}, nickname: {}", 
                providerUserId, email, username);
        
        return new SocialUserInfo(providerUserId, username, email);
    }
    
    @Override
    public String getNameAttributeKey(Map<String, Object> attributes) {
        return "id";  // Kakao's unique identifier
    }
} 