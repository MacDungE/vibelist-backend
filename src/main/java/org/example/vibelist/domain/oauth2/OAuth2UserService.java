package org.example.vibelist.domain.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.oauth2.service.OAuth2UserProcessor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth2 사용자 서비스 - 리팩토링된 버전
 * 기존의 복잡한 로직을 각 책임별로 분리된 서비스들에 위임합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final OAuth2UserProcessor oAuth2UserProcessor;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            String provider = userRequest.getClientRegistration().getRegistrationId();
            log.info("[OAuth2_LOG] OAuth2UserService.loadUser 시작 - provider: {}", provider);

            // 기본 OAuth2User 정보 가져오기
            OAuth2User oAuth2User = super.loadUser(userRequest);

            // 분리된 프로세서에 위임하여 처리
            OAuth2User processedUser = oAuth2UserProcessor.processOAuth2User(userRequest, oAuth2User);

            log.info("[OAuth2_LOG] OAuth2UserService.loadUser 완료 - provider: {}", provider);
            return processedUser;

        } catch (Exception e) {
            log.error("[OAuth2_LOG] OAuth2UserService.loadUser에서 오류 발생", e);
            throw new OAuth2AuthenticationException("OAuth2 사용자 정보 처리 중 오류가 발생했습니다.");
        }
    }
} 