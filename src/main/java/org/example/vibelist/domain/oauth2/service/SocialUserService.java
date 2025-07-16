package org.example.vibelist.domain.oauth2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.auth.entity.Auth;
import org.example.vibelist.domain.auth.repository.AuthRepository;
import org.example.vibelist.global.constants.Role;
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.domain.user.entity.UserProfile;
import org.example.vibelist.domain.user.repository.UserProfileRepository;
import org.example.vibelist.domain.user.repository.UserRepository;
import org.example.vibelist.global.util.UsernameGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 소셜 로그인 사용자 관리 서비스
 * 소셜 로그인 사용자의 생성, 조회, Auth 정보 관리를 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialUserService {
    
    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UsernameGenerator usernameGenerator;
    
    /**
     * 새로운 소셜 사용자 생성
     * User, UserProfile, Auth를 한 번에 생성합니다.
     */
    @Transactional
    public User createNewSocialUser(String providerUserId, String username, String email, String provider) {
        log.info("[SOCIAL_USER] 신규 소셜 사용자 생성 시작 - provider: {}, providerUserId: {}", provider, providerUserId);
        
        // 1. User 생성 (임시 사용자명 사용)
        String tempUsername = usernameGenerator.generateUniqueUsername(
            userRepository::existsByUsername
        );
        User user = User.builder()
                .username(tempUsername)
                .password("") // 소셜 로그인은 패스워드 없음
                .role(Role.USER)
                .build();
        user = userRepository.save(user);

        // 2. UserProfile 생성
        UserProfile userProfile = UserProfile.builder()
                .user(user)
                .email(email)
                .name(username)
                .phone("")
                .build();
        userProfileRepository.save(userProfile);

        // 3. Auth 생성
        Auth auth = Auth.builder()
                .user(user)
                .provider(provider.toUpperCase())
                .providerUserId(providerUserId)
                .providerEmail(email)
                .build();
        authRepository.save(auth);

        log.info("[SOCIAL_USER] 신규 소셜 사용자 생성 완료 - userId: {}, tempUsername: {}", user.getId(), tempUsername);
        return user;
    }
    
    /**
     * 기존 소셜 사용자 조회
     */
    public Optional<User> findExistingSocialUser(String provider, String providerUserId) {
        return authRepository.findByProviderAndProviderUserId(provider.toUpperCase(), providerUserId)
                .map(Auth::getUser);
    }
    
    /**
     * Auth 정보 업데이트 (JWT 리프레시 토큰 저장용)
     * 일반 로그인 시에만 사용됩니다.
     */
    @Transactional
    public void updateAuthRefreshToken(User user, String provider, String refreshToken) {
        Optional<Auth> authOpt = authRepository.findByUserIdAndProvider(user.getId(), provider.toUpperCase());
        
        if (authOpt.isPresent()) {
            Auth auth = authOpt.get();
            auth.updateRefreshToken(refreshToken);
            authRepository.save(auth);
            log.info("[SOCIAL_USER] Auth 리프레시 토큰 업데이트 완료 - userId: {}, provider: {}", user.getId(), provider);
        } else {
            log.warn("[SOCIAL_USER] Auth 정보를 찾을 수 없습니다 - userId: {}, provider: {}", user.getId(), provider);
        }
    }
    
    /**
     * 소셜 계정 연동 중복 검증
     * Integration 요청 시 해당 소셜 계정이 이미 다른 사용자에게 연동되어 있는지 확인합니다.
     */
    public boolean isAlreadyLinkedToOtherUser(String provider, String providerUserId, Long currentUserId) {
        Optional<User> existingUser = findExistingSocialUser(provider, providerUserId);
        return existingUser.isPresent() && !existingUser.get().getId().equals(currentUserId);
    }
}
