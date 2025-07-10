package org.example.vibelist.global.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.global.auth.entity.UserSocial;
import org.example.vibelist.global.auth.repository.UserSocialRepository;
import org.example.vibelist.global.constants.Role;
import org.example.vibelist.global.constants.SocialProvider;
import org.example.vibelist.global.constants.TokenConstants;
import org.example.vibelist.global.security.jwt.JwtTokenProvider;
import org.example.vibelist.global.user.entity.User;
import org.example.vibelist.global.user.entity.UserProfile;
import org.example.vibelist.global.user.repository.UserProfileRepository;
import org.example.vibelist.global.user.repository.UserRepository;
import org.example.vibelist.global.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;
    private final UserSocialRepository userSocialRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.access-token-validity}")
    private Long jwtAccessTokenExpirationTime;
    @Value("${jwt.refresh-token-validity}")
    private Long jwtRefreshTokenExpirationTime;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 소셜 로그인 성공시 사용자 정보를 구글이나 카카오에서 받아서 처리하는 메서드

        try {
            // 어떤 소셜 로그인 제공자인지 (google, kakao 등)
            String provider = userRequest.getClientRegistration().getRegistrationId();

            log.info("[OAuth2_LOG] OAuth2UserService.loadUser 시작 - provider: {}",
                    provider);

            // 기본 OAuth2User 정보 가져오기 (email, name 등)
            OAuth2User oAuth2User = super.loadUser(userRequest);




            // 소셜 사용자 정보 추출
            Map<String, Object> attributes = oAuth2User.getAttributes();
            String providerUserId, username, email;

            // provider 별로 파싱 방식이 다름
            if ("google".equals(provider)) {
                email = (String) attributes.get("email");    // 구글의 이메일
                // Google OAuth2 API는 다양한 필드명을 사용할 수 있으므로 여러 방법으로 시도
                providerUserId = (String) attributes.get("id");
                if (providerUserId == null || providerUserId.isEmpty()) {
                    providerUserId = (String) attributes.get("sub");  // OIDC 표준 식별자
                }
                if (providerUserId == null || providerUserId.isEmpty()) {
                    providerUserId = email;  // 최후의 수단으로 email 사용
                }
                username = (String) attributes.get("name");
                
                log.info("[OAuth2_LOG] 구글 사용자 정보 - email: {}, id: {}, sub: {}, name: {}, 사용할 providerUserId: {}", 
                        email, attributes.get("id"), attributes.get("sub"), username, providerUserId);
                log.info("[OAuth2_LOG] 전체 attributes: {}", attributes);

            } else if ("kakao".equals(provider)) {
                providerUserId = attributes.get("id").toString();   // 카카오 고유 id
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                email = (String) kakaoAccount.get("email");
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                username = (String) profile.get("nickname");
                
                log.info("[OAuth2_LOG] 카카오 사용자 정보 - id: {}, email: {}, nickname: {}", providerUserId, email, username);

            } else {
                // 기타 provider 처리 안함
                email = null;
                username = null;
                providerUserId = null;
                log.warn("[OAuth2_LOG] 지원하지 않는 provider: {}", provider);
            }

            log.info("{} 로그인 확인 providerUserId = {}", provider, providerUserId);
            log.info("{} 로그인 확인 email = {}", provider, email);
            log.info("{} 로그인 확인 username = {}", provider, username);

            // 필수 정보 검증
            if (providerUserId == null || providerUserId.isEmpty()) {
                throw new OAuth2AuthenticationException("Provider user ID가 없습니다.");
            }

            // SocialProvider enum으로 변환
            SocialProvider socialProvider = SocialProvider.valueOf(provider.toUpperCase());

            // 회원 정보가 DB에 존재하는지 확인 (UserSocial 테이블에서 확인)
            Optional<UserSocial> existingUserSocial = userSocialRepository.findByProviderAndProviderUserId(
                    socialProvider, providerUserId);

            User user;
            if (existingUserSocial.isPresent()) {
                // 기존 회원인 경우
                user = existingUserSocial.get().getUser();
                log.info("기존 소셜 회원 로그인: userId = {}", user.getId());
            } else {
                // 신규 회원인 경우 자동 회원가입 처리
                user = createNewSocialUser(providerUserId, username, email, socialProvider);
                log.info("신규 소셜 회원가입: userId = {}", user.getId());
            }

            // JWT 액세스 & 리프레시 토큰 발급
            String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

            log.info("[OAuth2_LOG] JWT 토큰 생성 완료 - userId: {}, accessToken 존재: {}, refreshToken 존재: {}", 
                    user.getId(), accessToken != null, refreshToken != null);

            // UserSocial 정보 업데이트 (refresh token만 저장)
            upsertUserSocial(user, socialProvider, providerUserId, email, refreshToken);

            // JWT는 SuccessHandler에서 쿠키/쿼리로 전달 → 여기선 속성에만 담아 둠
            Map<String, Object> customAttributes = new HashMap<>(attributes);
            customAttributes.put(TokenConstants.ACCESS_TOKEN, accessToken);
            customAttributes.put(TokenConstants.REFRESH_TOKEN, refreshToken);
            customAttributes.put("userId", user.getId()); // 내부 DB ID

            log.info("[OAuth2_LOG] 커스텀 속성 설정 완료 - userId: {}, accessToken: {}, refreshToken: {}",
                    customAttributes.get("userId"),
                    customAttributes.get(TokenConstants.ACCESS_TOKEN) != null,
                    customAttributes.get(TokenConstants.REFRESH_TOKEN) != null);

            // 신규 사용자인지 확인하고 플래그 추가
            boolean isNewUser = existingUserSocial.isEmpty();
            customAttributes.put("isNewUser", isNewUser);
            customAttributes.put("tempUserId", user.getId());
            customAttributes.put("provider", provider);

            // 최종적으로 Spring Security에 전달할 OAuth2User 반환
            String nameAttributeKey;
            if ("google".equals(provider)) {
                // Google에서 실제로 제공하는 식별자 필드를 확인하여 사용
                if (attributes.containsKey("id") && attributes.get("id") != null) {
                    nameAttributeKey = "id";
                } else if (attributes.containsKey("sub") && attributes.get("sub") != null) {
                    nameAttributeKey = "sub";
                } else {
                    nameAttributeKey = "email"; // 최후의 수단
                }
            } else if ("kakao".equals(provider)) {
                nameAttributeKey = "id";  // Kakao's unique identifier
            } else {
                nameAttributeKey = "id";  // Default to 'id'
            }

            // 모든 provider에 대해 선택된 nameAttributeKey에 providerUserId를 설정
            customAttributes.put(nameAttributeKey, providerUserId);
            
            // 안전을 위해 "id" 필드도 설정 (다른 곳에서 참조할 수 있음)
            if (!"id".equals(nameAttributeKey)) {
                customAttributes.put("id", providerUserId);
            }

            log.info("[OAuth2_LOG] nameAttributeKey 설정: {} (provider: {})", nameAttributeKey, provider);
            log.info("[OAuth2_LOG] nameAttributeKey에 해당하는 값: {}", customAttributes.get(nameAttributeKey));
            log.info("[OAuth2_LOG] customAttributes keys: {}", customAttributes.keySet());
            log.info("[OAuth2_LOG] 신규 사용자 여부: {}", isNewUser);

            // nameAttributeKey에 해당하는 값이 null이면 안되므로 검증
            if (customAttributes.get(nameAttributeKey) == null) {
                log.error("[OAuth2_LOG] nameAttributeKey '{}'에 해당하는 값이 null입니다. providerUserId를 사용합니다.", nameAttributeKey);
                customAttributes.put(nameAttributeKey, providerUserId);
            }

            log.info("[OAuth2_LOG] 최종 DefaultOAuth2User 생성 준비 완료 - nameAttributeKey: {}, 권한: ROLE_{}", 
                    nameAttributeKey, user.getRole().name());

            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())), // 권한
                    customAttributes,  // 속성 정보 (JWT 포함)
                    nameAttributeKey // PK로 사용할 식별자
            );
            
        } catch (Exception e) {
            log.error("[OAuth2_LOG] OAuth2UserService.loadUser에서 오류 발생", e);
            throw new OAuth2AuthenticationException("OAuth2 사용자 정보 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 새로운 소셜 사용자 생성
     */
    @Transactional
    private User createNewSocialUser(String providerUserId, String username, String email, SocialProvider provider) {
        // 임시 사용자명 생성 (나중에 사용자가 변경할 수 있음)
        String tempUsername = generateTempUsername(provider);

        // User 생성
        User user = User.builder()
                .username(tempUsername)
                .password("") // 소셜 로그인은 패스워드 없음
                .role(Role.USER)
                .build();
        user = userRepository.save(user);

        // UserProfile 생성
        UserProfile userProfile = UserProfile.builder()
                .user(user)
                .email(email)
                .name(username)
                .phone("")
                .build();
        userProfileRepository.save(userProfile);

        // UserSocial 생성
        UserSocial newUserSocial = UserSocial.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .providerEmail(email)
                .build();
        userSocialRepository.save(newUserSocial);

        return user;
    }

    /**
     * 임시 사용자명 생성
     */
    private String generateTempUsername(SocialProvider provider) {
        String[] prefixes = {"temp", "new", "user"};
        String prefix = prefixes[new Random().nextInt(prefixes.length)];
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8); // 마지막 4자리
        return prefix + "_" + timestamp;
    }

    /**
     * UserSocial 정보 업데이트 또는 생성 (Upsert)
     */
    private void upsertUserSocial(User user, SocialProvider provider, String providerUserId,
                                  String email, String refreshToken) {
        Optional<UserSocial> userSocialOpt = userService.findUserSocialByUserIdAndProvider(user.getId(), provider);

        UserSocial userSocial = userSocialOpt.orElseGet(() -> UserSocial.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .providerEmail(email)
                .build());

        // refresh token만 업데이트
        userSocial.updateRefreshToken(refreshToken);

        userSocialRepository.save(userSocial);
    }
} 