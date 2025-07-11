package org.example.vibelist.global.oauth2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.global.constants.TokenConstants;
import org.example.vibelist.global.oauth2.provider.SocialProviderFactory;
import org.example.vibelist.global.oauth2.provider.SocialProviderStrategy;
import org.example.vibelist.global.security.jwt.JwtTokenProvider;
import org.example.vibelist.global.user.entity.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * OAuth2 사용자 처리 프로세서
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserProcessor {
    
    private final SocialProviderFactory providerFactory;
    private final SocialUserService socialUserService;
    private final OAuth2TokenHandler tokenHandler;
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * OAuth2 사용자 정보를 처리하여 OAuth2User 반환
     */
    @Transactional
    public OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String provider = userRequest.getClientRegistration().getRegistrationId();
        
        log.info("[OAUTH2_PROCESSOR] OAuth2 사용자 처리 시작 - provider: {}", provider);
        
        // Provider 전략 가져오기
        if (!providerFactory.isSupported(provider)) {
            throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
        }
        
        SocialProviderStrategy strategy = providerFactory.getStrategy(provider);
        
        // 사용자 정보 추출
        Map<String, Object> attributes = oAuth2User.getAttributes();
        SocialProviderStrategy.SocialUserInfo userInfo = strategy.extractUserInfo(attributes);
        
        log.info("[OAUTH2_PROCESSOR] 사용자 정보 추출 완료 - provider: {}, providerUserId: {}, email: {}, username: {}", 
                provider, userInfo.getProviderUserId(), userInfo.getEmail(), userInfo.getUsername());
        
        // 필수 정보 검증
        if (userInfo.getProviderUserId() == null || userInfo.getProviderUserId().isEmpty()) {
            throw new IllegalArgumentException("Provider user ID가 없습니다.");
        }
        
        // 기존 사용자 확인 또는 신규 생성
        Optional<User> existingUserOpt = socialUserService.findExistingSocialUser(provider, userInfo.getProviderUserId());
        User user;
        boolean isNewUser;
        
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            isNewUser = false;
            log.info("[OAUTH2_PROCESSOR] 기존 소셜 회원 로그인: userId = {}", user.getId());
        } else {
            user = socialUserService.createNewSocialUser(
                    userInfo.getProviderUserId(), 
                    userInfo.getUsername(), 
                    userInfo.getEmail(), 
                    provider.toUpperCase()
            );
            isNewUser = true;
            log.info("[OAUTH2_PROCESSOR] 신규 소셜 회원가입: userId = {}", user.getId());
        }
        
        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        
        log.info("[OAUTH2_PROCESSOR] JWT 토큰 생성 완료 - userId: {}", user.getId());
        
        // Auth 정보 업데이트
        socialUserService.upsertAuth(user, provider.toUpperCase(), userInfo.getProviderUserId(), userInfo.getEmail(), refreshToken);
        
        // 통합 토큰 정보 저장
        tokenHandler.saveIntegrationTokenInfo(user, provider, userRequest);
        
        // OAuth2User 속성 구성
        Map<String, Object> customAttributes = createCustomAttributes(
                attributes, userInfo, strategy, user, accessToken, refreshToken, isNewUser, provider
        );
        
        // nameAttributeKey 결정
        String nameAttributeKey = strategy.getNameAttributeKey(attributes);
        
        // nameAttributeKey에 해당하는 값이 null이면 안되므로 검증
        if (customAttributes.get(nameAttributeKey) == null) {
            log.error("[OAUTH2_PROCESSOR] nameAttributeKey '{}'에 해당하는 값이 null입니다. providerUserId를 사용합니다.", nameAttributeKey);
            customAttributes.put(nameAttributeKey, userInfo.getProviderUserId());
        }
        
        log.info("[OAUTH2_PROCESSOR] OAuth2User 생성 완료 - userId: {}, nameAttributeKey: {}, isNewUser: {}", 
                user.getId(), nameAttributeKey, isNewUser);
        
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                customAttributes,
                nameAttributeKey
        );
    }
    
    /**
     * 커스텀 속성 맵 생성
     */
    private Map<String, Object> createCustomAttributes(
            Map<String, Object> originalAttributes,
            SocialProviderStrategy.SocialUserInfo userInfo,
            SocialProviderStrategy strategy,
            User user,
            String accessToken,
            String refreshToken,
            boolean isNewUser,
            String provider) {
        
        Map<String, Object> customAttributes = new HashMap<>(originalAttributes);
        
        // JWT 토큰 정보 추가
        customAttributes.put(TokenConstants.ACCESS_TOKEN, accessToken);
        customAttributes.put(TokenConstants.REFRESH_TOKEN, refreshToken);
        customAttributes.put("userId", user.getId());
        
        // 신규 사용자 정보 추가
        customAttributes.put("isNewUser", isNewUser);
        customAttributes.put("tempUserId", user.getId());
        customAttributes.put("provider", provider);
        
        // nameAttributeKey에 providerUserId 설정
        String nameAttributeKey = strategy.getNameAttributeKey(originalAttributes);
        customAttributes.put(nameAttributeKey, userInfo.getProviderUserId());
        
        // 안전을 위해 "id" 필드도 설정
        if (!"id".equals(nameAttributeKey)) {
            customAttributes.put("id", userInfo.getProviderUserId());
        }
        
        log.info("[OAUTH2_PROCESSOR] 커스텀 속성 설정 완료 - userId: {}, nameAttributeKey: {}", user.getId(), nameAttributeKey);
        
        return customAttributes;
    }
} 