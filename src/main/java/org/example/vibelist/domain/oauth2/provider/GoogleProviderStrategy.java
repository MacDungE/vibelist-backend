package org.example.vibelist.domain.oauth2.provider;

import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.global.constants.SocialProviderConstants;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Google OAuth2 Provider 전략 구현체
 */
@Slf4j
@Component
public class GoogleProviderStrategy implements SocialProviderStrategy {
    
    @Override
    public String getProviderName() {
        return SocialProviderConstants.GOOGLE;
    }
    
    @Override
    public SocialUserInfo extractUserInfo(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        String username = (String) attributes.get("name");
        
        // Google OAuth2 API는 다양한 필드명을 사용할 수 있으므로 여러 방법으로 시도
        String providerUserId = (String) attributes.get("id");
        if (providerUserId == null || providerUserId.isEmpty()) {
            providerUserId = (String) attributes.get("sub");  // OIDC 표준 식별자
        }
        if (providerUserId == null || providerUserId.isEmpty()) {
            providerUserId = email;  // 최후의 수단으로 email 사용
        }
        
        log.info("[OAuth2_LOG] 구글 사용자 정보 - email: {}, id: {}, sub: {}, name: {}, 사용할 providerUserId: {}", 
                email, attributes.get("id"), attributes.get("sub"), username, providerUserId);
        log.info("[OAuth2_LOG] 전체 attributes: {}", attributes);
        
        return new SocialUserInfo(providerUserId, username, email);
    }
    
    @Override
    public String getNameAttributeKey(Map<String, Object> attributes) {
        // Google에서 실제로 제공하는 식별자 필드를 확인하여 사용
        if (attributes.containsKey("id") && attributes.get("id") != null) {
            return "id";
        } else if (attributes.containsKey("sub") && attributes.get("sub") != null) {
            return "sub";
        } else {
            return "email"; // 최후의 수단
        }
    }
} 