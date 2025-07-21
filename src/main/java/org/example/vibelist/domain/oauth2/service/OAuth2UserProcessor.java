package org.example.vibelist.domain.oauth2.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.domain.user.service.UserService;
import org.example.vibelist.global.constants.SocialProviderConstants;
import org.example.vibelist.global.constants.TokenConstants;
import org.example.vibelist.global.security.jwt.JwtTokenProvider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth2 사용자 처리 프로세서
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserProcessor {

    private final SocialUserService socialUserService;
    private final OAuth2TokenHandler tokenHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    /**
     * OAuth2 사용자 정보를 처리하여 OAuth2User 반환
     */
    @Transactional
    public OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String provider = userRequest.getClientRegistration().getRegistrationId();

        log.info("[OAUTH2_PROCESSOR] OAuth2 사용자 처리 시작 - provider: {}", provider);

        // 세션에서 Integration 요청 정보 확인
        boolean tempIsIntegrationRequest = false;
        Long tempIntegrationUserId = null;

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            HttpSession session = request.getSession(false);

            if (session != null) {
                Object userIdObj = session.getAttribute("oauth2_integration_user_id");
                Object timestampObj = session.getAttribute("oauth2_integration_timestamp");

                if (userIdObj != null && timestampObj != null) {
                    long timestamp = (Long) timestampObj;
                    long currentTime = System.currentTimeMillis();

                    // 5분(300초) 만료 체크
                    if (currentTime - timestamp < 300000) {
                        tempIsIntegrationRequest = true;
                        tempIntegrationUserId = Long.parseLong(userIdObj.toString());

                        log.info("[OAUTH2_PROCESSOR] Integration 요청 확인 - targetUserId: {}, provider: {}", tempIntegrationUserId, provider);

                        // 세션 정보 정리
                        session.removeAttribute("oauth2_integration_user_id");
                        session.removeAttribute("oauth2_integration_timestamp");
                    } else {
                        log.warn("[OAUTH2_PROCESSOR] Integration 세션이 만료됨 ({}초 경과)", (currentTime - timestamp) / 1000);
                        session.removeAttribute("oauth2_integration_user_id");
                        session.removeAttribute("oauth2_integration_timestamp");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[OAUTH2_PROCESSOR] 세션에서 Integration 정보 확인 실패 - 일반 로그인으로 처리: {}", e.getMessage());
        }

        final boolean isIntegrationRequest = tempIsIntegrationRequest;
        final Long integrationUserId = tempIntegrationUserId;

        log.info("[OAUTH2_PROCESSOR] Integration 요청 판단 결과 - isIntegrationRequest: {}, integrationUserId: {}",
                isIntegrationRequest, integrationUserId);

        // 사용자 정보 추출 (간소화된 방식)
        Map<String, Object> attributes = oAuth2User.getAttributes();
        SocialUserInfo userInfo = extractUserInfo(provider, attributes);

        log.info("[OAUTH2_PROCESSOR] 사용자 정보 추출 완료 - provider: {}, providerUserId: {}, email: {}, username: {}",
                provider, userInfo.getProviderUserId(), userInfo.getEmail(), userInfo.getUsername());

        // 필수 정보 검증
        if (userInfo.getProviderUserId() == null || userInfo.getProviderUserId().isEmpty()) {
            throw new IllegalArgumentException("Provider user ID가 없습니다.");
        }

        User user;
        boolean isNewUser = false;

        if (isIntegrationRequest && integrationUserId != null) {
            // Integration 요청인 경우: 기존 사용자 조회만 수행
            user = userService.findUserById(integrationUserId)
                    .orElseThrow(() -> new IllegalArgumentException("연동 대상 사용자를 찾을 수 없습니다: " + integrationUserId));

            isNewUser = false;

            log.info("[OAUTH2_PROCESSOR] Integration 요청 - 기존 사용자 사용: userId = {}, isNewUser = {}", user.getId(), isNewUser);

//            // 중복 연동 검증
//            if (socialUserService.isAlreadyLinkedToOtherUser(provider, userInfo.getProviderUserId(), integrationUserId)) {
//                throw new IllegalArgumentException("해당 소셜 계정은 이미 다른 사용자에게 연동되어 있습니다.");
//            }

        } else {
            // 일반 로그인 요청인 경우: 기존 로직 수행
            Optional<User> existingUserOpt = socialUserService.findExistingSocialUser(provider, userInfo.getProviderUserId());

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
        }

        // JWT 토큰 생성
        String accessToken = null;
        String refreshToken = null;

        // Auth 정보 업데이트 (일반 로그인인 경우만)
        if (!isIntegrationRequest) {
            try {
                accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
                refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

                log.info("[OAUTH2_PROCESSOR] JWT 토큰 생성 완료 - userId: {}, accessToken 존재: {}, refreshToken 존재: {}",
                        user.getId(), accessToken != null, refreshToken != null);
            } catch (Exception e) {
                log.error("[OAUTH2_PROCESSOR] JWT 토큰 생성 실패 - userId: {}, 오류: {}", user.getId(), e.getMessage(), e);
                throw new RuntimeException("JWT 토큰 생성에 실패했습니다.", e);
            }


            socialUserService.updateAuthRefreshToken(user, provider, refreshToken);
        }

        // 통합 토큰 정보 저장
        tokenHandler.saveIntegrationTokenInfo(user, provider, userRequest);

        // OAuth2User 속성 구성
        Map<String, Object> customAttributes = createCustomAttributes(
                attributes, userInfo, user, accessToken, refreshToken, isNewUser, provider, isIntegrationRequest
        );

        // nameAttributeKey 결정
        String nameAttributeKey = getNameAttributeKey(provider, attributes);

        // nameAttributeKey에 해당하는 값이 null이면 안되므로 검증
        if (customAttributes.get(nameAttributeKey) == null) {
            log.error("[OAUTH2_PROCESSOR] nameAttributeKey '{}'에 해당하는 값이 null입니다. providerUserId를 사용합니다.", nameAttributeKey);
            customAttributes.put(nameAttributeKey, userInfo.getProviderUserId());
        }

        log.info("[OAUTH2_PROCESSOR] OAuth2User 생성 완료 - userId: {}, nameAttributeKey: {}, isNewUser: {}, isIntegration: {}",
                user.getId(), nameAttributeKey, isNewUser, isIntegrationRequest);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                customAttributes,
                nameAttributeKey
        );
    }

    /**
     * Provider별 사용자 정보 추출 (간소화)
     */
    private SocialUserInfo extractUserInfo(String provider, Map<String, Object> attributes) {
        switch (provider.toLowerCase()) {
            case "google":
                return extractGoogleUserInfo(attributes);
            case "kakao":
                return extractKakaoUserInfo(attributes);
            case "spotify":
                return extractSpotifyUserInfo(attributes);
            default:
                throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
        }
    }

    /**
     * Google 사용자 정보 추출
     */
    @SuppressWarnings("unchecked")
    private SocialUserInfo extractGoogleUserInfo(Map<String, Object> attributes) {
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

        log.info("[OAUTH2_PROCESSOR] 구글 사용자 정보 - email: {}, id: {}, sub: {}, name: {}, 사용할 providerUserId: {}",
                email, attributes.get("id"), attributes.get("sub"), username, providerUserId);

        return new SocialUserInfo(providerUserId, username, email);
    }

    /**
     * Kakao 사용자 정보 추출
     */
    @SuppressWarnings("unchecked")
    private SocialUserInfo extractKakaoUserInfo(Map<String, Object> attributes) {
        String providerUserId = attributes.get("id").toString();

        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        String email = (String) kakaoAccount.get("email");

        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        String username = (String) profile.get("nickname");

        log.info("[OAUTH2_PROCESSOR] 카카오 사용자 정보 - id: {}, email: {}, nickname: {}",
                providerUserId, email, username);

        return new SocialUserInfo(providerUserId, username, email);
    }

    /**
     * Spotify 사용자 정보 추출
     */
    private SocialUserInfo extractSpotifyUserInfo(Map<String, Object> attributes) {
        String providerUserId = attributes.get("id").toString();
        String email = (String) attributes.get("email");
        String username = (String) attributes.get("display_name");

        // Spotify에서 display_name이 null일 수 있으므로 대체값 설정
        if (username == null || username.isEmpty()) {
            username = (String) attributes.get("id"); // id를 username으로 사용
        }

        log.info("[OAUTH2_PROCESSOR] Spotify 사용자 정보 - id: {}, email: {}, display_name: {}, 최종 username: {}",
                providerUserId, email, attributes.get("display_name"), username);

        return new SocialUserInfo(providerUserId, username, email);
    }

    /**
     * Provider별 nameAttributeKey 반환
     */
    private String getNameAttributeKey(String provider, Map<String, Object> attributes) {
        switch (provider.toLowerCase()) {
            case "google":
                // Google에서 실제로 제공하는 식별자 필드를 확인하여 사용
                if (attributes.containsKey("id") && attributes.get("id") != null) {
                    return "id";
                } else if (attributes.containsKey("sub") && attributes.get("sub") != null) {
                    return "sub";
                } else {
                    return "email"; // 최후의 수단
                }
            case "kakao":
            case "spotify":
                return "id";
            default:
                return "id";
        }
    }

    /**
     * Spotify가 특별한 토큰 처리가 필요한지 확인
     */
    private boolean requiresSpecialTokenHandling(String provider) {
        return SocialProviderConstants.SPOTIFY_LOWER.equals(provider);
    }

    /**
     * 커스텀 속성 맵 생성
     */
    private Map<String, Object> createCustomAttributes(
            Map<String, Object> originalAttributes,
            SocialUserInfo userInfo,
            User user,
            String accessToken,
            String refreshToken,
            boolean isNewUser,
            String provider,
            boolean isIntegrationRequest) {

        Map<String, Object> customAttributes = new HashMap<>(originalAttributes);

        // JWT 토큰 정보 추가
        customAttributes.put(TokenConstants.ACCESS_TOKEN, accessToken);
        customAttributes.put(TokenConstants.REFRESH_TOKEN, refreshToken);
        customAttributes.put("userId", user.getId());

        // 신규 사용자 및 연동 요청 정보 추가
        customAttributes.put("isNewUser", isNewUser);
        customAttributes.put("isIntegrationRequest", isIntegrationRequest);
        customAttributes.put("tempUserId", user.getId());
        customAttributes.put("provider", provider);

        // nameAttributeKey에 providerUserId 설정
        String nameAttributeKey = getNameAttributeKey(provider, originalAttributes);
        customAttributes.put(nameAttributeKey, userInfo.getProviderUserId());

        // 안전을 위해 "id" 필드도 설정
        if (!"id".equals(nameAttributeKey)) {
            customAttributes.put("id", userInfo.getProviderUserId());
        }

        log.info("[OAUTH2_PROCESSOR] 커스텀 속성 설정 완료 - userId: {}, nameAttributeKey: {}, isIntegration: {}",
                user.getId(), nameAttributeKey, isIntegrationRequest);

        return customAttributes;
    }

    /**
     * 추출된 사용자 정보를 담는 내부 클래스
     */
    public static class SocialUserInfo {
        private final String providerUserId;
        private final String username;
        private final String email;

        public SocialUserInfo(String providerUserId, String username, String email) {
            this.providerUserId = providerUserId;
            this.username = username;
            this.email = email;
        }

        public String getProviderUserId() {
            return providerUserId;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }
    }
} 