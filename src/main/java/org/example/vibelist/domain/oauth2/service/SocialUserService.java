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
import org.example.vibelist.domain.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;

/**
 * 소셜 사용자 생성 및 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialUserService {
    
    private final UserService userService;
    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    
    /**
     * 새로운 소셜 사용자 생성
     */
    @Transactional
    public User createNewSocialUser(String providerUserId, String username, String email, String provider) {
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

        // Auth 생성
        Auth newAuth = Auth.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .providerEmail(email)
                .build();
        authRepository.save(newAuth);

        log.info("[SOCIAL_USER] 신규 소셜 사용자 생성 완료 - userId: {}, provider: {}, providerUserId: {}", 
                user.getId(), provider, providerUserId);

        return user;
    }
    
    /**
     * Auth 정보 업데이트 또는 생성 (Upsert)
     */
    @Transactional
    public void upsertAuth(User user, String provider, String providerUserId, String email, String refreshToken) {
        Optional<Auth> authOpt = userService.findAuthByUserIdAndProvider(user.getId(), provider);

        Auth auth = authOpt.orElseGet(() -> Auth.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .providerEmail(email)
                .build());

        // refresh token만 업데이트
        auth.updateRefreshToken(refreshToken);
        authRepository.save(auth);
        
        log.info("[SOCIAL_USER] Auth 정보 업데이트 완료 - userId: {}, provider: {}", user.getId(), provider);
    }
    
    /**
     * 기존 소셜 사용자 찾기
     */
    public Optional<User> findExistingSocialUser(String provider, String providerUserId) {
        return authRepository.findByProviderAndProviderUserId(provider.toUpperCase(), providerUserId)
                .map(Auth::getUser);
    }
    
    /**
     * 임시 사용자명 생성
     */
    private String generateTempUsername(String provider) {
        String[] prefixes = {"temp", "new", "user"};
        String prefix = prefixes[new Random().nextInt(prefixes.length)];
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8); // 마지막 4자리
        return prefix + "_" + timestamp;
    }
} 