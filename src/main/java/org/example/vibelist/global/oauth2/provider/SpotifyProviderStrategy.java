package org.example.vibelist.global.oauth2.provider;

import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.global.constants.SocialProviderConstants;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Spotify OAuth2 Provider 전략 구현체
 */
@Slf4j
@Component
public class SpotifyProviderStrategy implements SocialProviderStrategy {
    
    @Override
    public String getProviderName() {
        return SocialProviderConstants.SPOTIFY;
    }
    
    @Override
    public SocialUserInfo extractUserInfo(Map<String, Object> attributes) {
        String providerUserId = attributes.get("id").toString();
        String email = (String) attributes.get("email");
        String username = (String) attributes.get("display_name");
        
        // Spotify에서 display_name이 null일 수 있으므로 대체값 설정
        if (username == null || username.isEmpty()) {
            username = (String) attributes.get("id"); // id를 username으로 사용
        }
        
        log.info("[OAuth2_LOG] Spotify 사용자 정보 - id: {}, email: {}, display_name: {}, 최종 username: {}", 
                providerUserId, email, attributes.get("display_name"), username);
        log.info("[OAuth2_LOG] Spotify 전체 attributes: {}", attributes);
        
        return new SocialUserInfo(providerUserId, username, email);
    }
    
    @Override
    public String getNameAttributeKey(Map<String, Object> attributes) {
        return "id";  // Spotify's unique identifier
    }
    
    @Override
    public boolean requiresSpecialTokenHandling() {
        return true; // Spotify는 특별한 토큰 처리가 필요
    }
} 