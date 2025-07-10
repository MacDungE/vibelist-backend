package org.example.vibelist.global.auth.util;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.global.auth.entity.UserSocial;
import org.example.vibelist.global.auth.repository.UserSocialRepository;
import org.example.vibelist.global.constants.Role;
import org.example.vibelist.global.constants.TokenConstants;
import org.example.vibelist.global.user.entity.User;
import org.example.vibelist.global.user.service.UserService;
import org.example.vibelist.global.util.UsernameGenerator;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SocialAuthUtil {
    
    private final UserService userService;
    private final UserSocialRepository userSocialRepository;
    private final UsernameGenerator usernameGenerator;
    
    /**
     * 소셜 계정으로 사용자 찾기 또는 생성
     */
    public User findOrCreateSocialUser(String provider, String providerUserId, 
                                     String email, String name, String refreshToken) {
        // 기존 소셜 계정 확인
        Optional<UserSocial> existingSocial = userSocialRepository.findByProviderAndProviderUserId(provider, providerUserId);
        
        if (existingSocial.isPresent()) {
            // 기존 사용자 로그인 - refresh token 업데이트
            UserSocial userSocial = existingSocial.get();
            userSocial.updateRefreshToken(refreshToken);
            userSocialRepository.save(userSocial);
            return userSocial.getUser();
        } else {
            // 신규 가입
            return createNewSocialUser(provider, providerUserId, email, name, refreshToken);
        }
    }
    
    /**
     * 새로운 소셜 사용자 생성
     */
    private User createNewSocialUser(String provider, String providerUserId, 
                                   String email, String name, String refreshToken) {
        // 이메일 중복 확인
        if (email != null && userService.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        
        // 랜덤 사용자명 생성
        String username = usernameGenerator.generateUniqueUsername(userService::existsByUsername);
        
        // User 생성
        User user = User.builder()
                .username(username)
                .password("") // 소셜 로그인은 비밀번호 없음
                .role(Role.USER)
                .build();
        
        User savedUser = userService.saveUser(user);
        
        // UserProfile 생성
        userService.createUserProfile(savedUser, email, name, null);
        
        // UserSocial 생성
        UserSocial userSocial = UserSocial.builder()
                .user(savedUser)
                .provider(provider)
                .providerUserId(providerUserId)
                .providerEmail(email)
                .refreshTokenEnc(refreshToken)
                .tokenType(TokenConstants.TOKEN_TYPE)
                .build();
        userSocialRepository.save(userSocial);
        
        return savedUser;
    }
    
    /**
     * 소셜 계정 연동
     */
    public void linkSocialAccount(Long userId, String provider, String providerUserId, 
                                 String providerEmail, String refreshToken) {
        Optional<User> userOpt = userService.findUserById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        
        // 이미 연동된 계정인지 확인
        if (userSocialRepository.existsByProviderAndProviderUserId(provider, providerUserId)) {
            throw new IllegalArgumentException("이미 연동된 소셜 계정입니다.");
        }
        
        User user = userOpt.get();
        UserSocial userSocial = UserSocial.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .providerEmail(providerEmail)
                .refreshTokenEnc(refreshToken)
                .tokenType(TokenConstants.TOKEN_TYPE)
                .build();
        userSocialRepository.save(userSocial);
    }
} 