package org.example.vibelist.domain.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.auth.dto.LoginResponse;
import org.example.vibelist.domain.auth.dto.TokenResponse;
import org.example.vibelist.domain.auth.entity.Auth;
import org.example.vibelist.domain.auth.repository.AuthRepository;
import org.example.vibelist.domain.auth.util.AuthUtil;
import org.example.vibelist.domain.auth.util.CookieUtil;
import org.example.vibelist.domain.auth.util.SocialAuthUtil;
import org.example.vibelist.global.constants.Role;
import org.example.vibelist.global.constants.TokenConstants;
import org.example.vibelist.global.security.jwt.JwtTokenProvider;
import org.example.vibelist.global.security.jwt.JwtTokenType;
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.domain.user.entity.UserProfile;
import org.example.vibelist.domain.user.service.UserService;
import org.example.vibelist.global.util.UsernameGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.vibelist.global.response.ResponseCode;
import org.example.vibelist.global.response.GlobalException;
import org.example.vibelist.global.response.RsData;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {
    
    private final UserService userService;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private final UsernameGenerator usernameGenerator;
    private final AuthUtil authUtil;
    private final SocialAuthUtil socialAuthUtil;
    private final CookieUtil cookieUtil;
    
    /**
     * 회원가입 처리
     */
    public void signup(String username, String password, String email, String name, String phone) {
        // 사용자명 중복 확인
        if (userService.existsByUsername(username)) {
            throw new GlobalException(ResponseCode.USER_ALREADY_EXISTS, "username='" + username + "'은 이미 존재합니다.");
        }

        // 이메일 중복 확인
        if (userService.existsByEmail(email)) {
            throw new GlobalException(ResponseCode.USER_ALREADY_EXISTS, "email='" + email + "'은 이미 존재합니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);

        // User 생성 및 저장 - Builder 패턴 사용
        User savedUser = userService.createUser(username, encodedPassword, Role.USER);

        // UserProfile 생성 및 저장 - Builder 패턴 사용
        userService.createUserProfile(savedUser, email, name, phone);
    }
    
    /**
     * 로그인 처리 - JWT 토큰 반환
     */
    public RsData<TokenResponse> login(String username, String password) {
        try {
            Optional<User> userOpt = userService.findUserByUsername(username);
            if (userOpt.isEmpty()) {
                throw new GlobalException(ResponseCode.USER_NOT_FOUND, "username='" + username + "'인 사용자를 찾을 수 없습니다.");
            }
            User user = userOpt.get();
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new GlobalException(ResponseCode.USER_PASSWORD_INVALID, "비밀번호 불일치 - username='" + username + "'");
            }
            TokenResponse token = authUtil.createTokenResponse(user);
            return RsData.success(ResponseCode.USER_LOGIN_SUCCESS, token);
        } catch (GlobalException ce) {
            throw ce;
        } catch (Exception e) {
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "로그인 처리 중 오류: " + e.getMessage());
        }
    }

    /**
     * 로그인 처리 - JWT 토큰과 refresh token 반환
     */
    public RsData<LoginResponse> loginWithRefreshToken(String username, String password) {
        try {
            Optional<User> userOpt = userService.findUserByUsername(username);
            if (userOpt.isEmpty()) {
                throw new GlobalException(ResponseCode.USER_NOT_FOUND, "username='" + username + "'인 사용자를 찾을 수 없습니다.");
            }
            User user = userOpt.get();
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new GlobalException(ResponseCode.USER_PASSWORD_INVALID, "비밀번호 불일치 - username='" + username + "'");
            }
            LoginResponse loginResponse = authUtil.createLoginResponse(user);
            return RsData.success(ResponseCode.USER_LOGIN_SUCCESS, loginResponse);
        } catch (GlobalException ce) {
            throw ce;
        } catch (Exception e) {
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "로그인 처리 중 오류: " + e.getMessage());
        }
    }

    /**
     * 토큰 갱신
     */
    public TokenResponse refreshToken(String refreshToken) {
        // 리프레시 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken) || 
            !JwtTokenType.REFRESH.equals(jwtTokenProvider.getTokenType(refreshToken))) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "유효하지 않은 리프레시 토큰입니다.");
        }

        // 사용자 정보 조회
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        Optional<User> userOpt = userService.findUserById(userId);
        
        if (userOpt.isEmpty()) {
            throw new GlobalException(ResponseCode.USER_NOT_FOUND, "userId=" + userId + "인 사용자를 찾을 수 없습니다.");
        }

        User user = userOpt.get();

        return authUtil.createTokenResponse(user);
    }

    public LoginResponse loginWithAccessToken(String accessToken) {
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new IllegalArgumentException("유효하지 않은 액세스 토큰입니다.");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        User user = userService.findUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return authUtil.createLoginResponse(user);
    }

    /**
     * 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public UserProfile getUserProfile(Long userId) {
        Optional<User> userOpt = userService.findUserById(userId);
        if (userOpt.isEmpty()) {
            throw new GlobalException(ResponseCode.USER_NOT_FOUND, "userId=" + userId + "인 사용자를 찾을 수 없습니다.");
        }

        Optional<UserProfile> profileOpt = userService.findUserProfileById(userId);
        if (profileOpt.isEmpty()) {
            throw new GlobalException(ResponseCode.USER_NOT_FOUND, "userId=" + userId + "인 사용자의 프로필을 찾을 수 없습니다.");
        }
        
        return profileOpt.get();
    }
    
    /**
     * 사용자 프로필 업데이트
     */
    public void updateUserProfile(Long userId, String name, String phone, String avatarUrl, String bio) {
        Optional<UserProfile> profileOpt = userService.findUserProfileById(userId);
        if (profileOpt.isEmpty()) {
            throw new GlobalException(ResponseCode.USER_NOT_FOUND, "userId=" + userId + "인 사용자의 프로필을 찾을 수 없습니다.");
        }

        UserProfile existingProfile = profileOpt.get();
        
        // 새로운 UserProfile 객체 생성 - Builder 패턴 사용
        UserProfile updatedProfile = UserProfile.builder()
                .user(existingProfile.getUser())
                .email(existingProfile.getEmail())
                .name(name)
                .phone(phone)
                .avatarUrl(avatarUrl)
                .bio(bio)
                .locale(existingProfile.getLocale())
                .build();
        
        userService.saveUserProfile(updatedProfile);
    }
    
    /**
     * 사용자 검색
     */
    @Transactional(readOnly = true)
    public List<UserProfile> searchUsers(String name) {
        return userService.findUserProfilesByName(name);
    }
    
    /**
     * 사용자 삭제
     */
    public void deleteUser(Long userId) {
        Optional<User> userOpt = userService.findUserById(userId);
        if (userOpt.isEmpty()) {
            throw new GlobalException(ResponseCode.USER_NOT_FOUND, "userId=" + userId + "인 사용자를 찾을 수 없습니다.");
        }

        userService.deleteUserProfile(userId);
        userService.deleteUser(userId);
    }
    
    /**
     * 소셜 로그인 연동 정보 조회
     */
    @Transactional(readOnly = true)
    public List<Auth> getAuthAccounts(Long userId) {
        return userService.findAuthsByUserId(userId);
    }
    

    
    /**
     * 로그아웃 처리
     * - 클라이언트의 access token과 refresh token 쿠키를 삭제
     * - 서버 측에서는 JWT 토큰이 stateless이므로 별도 처리 불필요
     */
    public void logout(HttpServletResponse response) {
        // access token과 refresh token 쿠키 삭제
        cookieUtil.removeAllAuthCookies(response);
    }

    // Auth 관련 메소드들
    /**
     * 소셜 제공자와 제공자 사용자 ID로 소셜 계정 조회
     */
    @Transactional(readOnly = true)
    public Optional<Auth> findAuthByProviderAndProviderUserId(String provider, String providerUserId) {
        return authRepository.findByProviderAndProviderUserId(provider, providerUserId);
    }

    /**
     * 소셜 제공자와 제공자 이메일로 소셜 계정 조회
     */
    @Transactional(readOnly = true)
    public Optional<Auth> findAuthByProviderAndProviderEmail(String provider, String providerEmail) {
        return authRepository.findByProviderAndProviderEmail(provider, providerEmail);
    }

    /**
     * 소셜 제공자별 소셜 계정 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Auth> findAuthsByProvider(String provider) {
        return authRepository.findByProvider(provider);
    }

    /**
     * 특정 소셜 제공자 계정 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean existsByProviderAndProviderUserId(String provider, String providerUserId) {
        return authRepository.existsByProviderAndProviderUserId(provider, providerUserId);
    }

    /**
     * Auth 저장
     */
    @Transactional
    public Auth saveAuth(Auth auth) {
        return authRepository.save(auth);
    }

    /**
     * Builder 패턴을 사용한 Auth 생성 및 저장 (소셜 로그인용)
     */
    @Transactional
    public Auth createAuth(User user, String provider, String providerUserId, 
                                     String providerEmail, String refreshTokenEnc) {
        Auth auth = Auth.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .providerEmail(providerEmail)
                .refreshTokenEnc(refreshTokenEnc)
                .tokenType(TokenConstants.TOKEN_TYPE)
                .build();
        return authRepository.save(auth);
    }

    /**
     * 일반 로그인용 Auth 생성 및 저장
     */
    @Transactional
    public Auth createRegularAuth(User user, String refreshToken, String tokenType) {
        Auth auth = new Auth(user, refreshToken, tokenType);
        return authRepository.save(auth);
    }

    /**
     * 사용자의 Auth 정보 조회 (일반 로그인용)
     */
    @Transactional(readOnly = true)
    public Optional<Auth> findRegularAuthByUserId(Long userId) {
        return authRepository.findByUserIdAndProviderIsNull(userId);
    }

    /**
     * 사용자의 Auth 정보 조회 (소셜 로그인용)
     */
    @Transactional(readOnly = true)
    public Optional<Auth> findSocialAuthByUserIdAndProvider(Long userId, String provider) {
        return authRepository.findByUserIdAndProvider(userId, provider);
    }

    /**
     * Auth 삭제
     */
    @Transactional
    public void deleteAuth(Long id) {
        authRepository.deleteById(id);
    }
    
    /**
     * 소셜 회원가입 완료 (사용자명 설정)
     */
    public void completeSocialSignup(String username, String provider, String tempUserId) {
        // 사용자명 중복 확인
        if (userService.existsByUsername(username)) {
            throw new GlobalException(ResponseCode.USER_ALREADY_EXISTS, "username='" + username + "'은 이미 존재합니다.");
        }
        
        // 임시 사용자 ID로 사용자 찾기
        Long userId = Long.parseLong(tempUserId);
        Optional<User> userOpt = userService.findUserById(userId);
        
        if (userOpt.isEmpty()) {
            throw new GlobalException(ResponseCode.USER_NOT_FOUND, "tempUserId=" + tempUserId + "인 임시 사용자를 찾을 수 없습니다.");
        }
        
        User user = userOpt.get();
        
        // 사용자명 업데이트
        user.updateUsername(username);
        userService.saveUser(user);
        
        log.info("소셜 회원가입 완료 - 사용자 ID: {}, 사용자명: {}, Provider: {}", userId, username, provider);
    }
}
