package org.example.vibelist.global.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.global.auth.dto.CompleteSocialSignupRequest;
import org.example.vibelist.global.auth.dto.StatusResponse;
import org.example.vibelist.global.auth.dto.TokenResponse;
import org.example.vibelist.global.auth.service.AuthService;
import org.example.vibelist.global.auth.util.CookieUtil;
import org.example.vibelist.global.constants.SocialProvider;
import org.example.vibelist.global.constants.TokenConstants;
import org.example.vibelist.global.user.dto.SocialAccountResponse;
import org.example.vibelist.global.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final CookieUtil cookieUtil;



    // 토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(name = TokenConstants.REFRESH_TOKEN_COOKIE, required = false) String refreshToken, 
                                         HttpServletResponse response) {
        try {
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("리프레시 토큰이 없습니다.");
            }
            TokenResponse tokenResponse = authService.refreshToken(refreshToken);
            
            // 새로운 access token을 쿠키에 설정
            cookieUtil.setAccessTokenCookie(response, tokenResponse.getAccessToken());
            
            return ResponseEntity.ok(tokenResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }



    // 통합 로그아웃 (일반 로그인 및 OAuth2 로그인 모두 지원)
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok("로그아웃 성공");
    }

    // 현재 사용자 소셜 로그인 연동 정보 조회
    @GetMapping("/me/social")
    public ResponseEntity<?> getCurrentUserSocialAccounts() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = (Long) authentication.getPrincipal();
            
            List<SocialAccountResponse> socialAccounts = userService.findUserSocialAccounts(userId);
            return ResponseEntity.ok(socialAccounts);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 소셜 로그인 연동 정보 조회 (관리자용)
    @GetMapping("/{userId}/social")
    public ResponseEntity<?> getUserSocialAccounts(@PathVariable Long userId) {
        List<SocialAccountResponse> socialAccounts = userService.findUserSocialAccounts(userId);
        return ResponseEntity.ok(socialAccounts);
    }
    


    // 소셜 회원가입 완료 (사용자명 설정)
    @PostMapping("/social/complete-signup")
    public ResponseEntity<?> completeSocialSignup(@RequestBody CompleteSocialSignupRequest request) {
        try {
            authService.completeSocialSignup(request.getUsername(), request.getProvider(), request.getUserId());
            Map<String, String> response = new HashMap<>();
            response.put("message", "사용자명 설정이 완료되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 인증 상태 확인
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> getAuthStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.ok(StatusResponse.unauthenticated());
        }
        
        // OAuth2 사용자인 경우
        if (authentication.getPrincipal() instanceof DefaultOAuth2User oAuth2User) {
            Map<String, Object> attributes = oAuth2User.getAttributes();
            return ResponseEntity.ok(StatusResponse.authenticated(
                getProviderFromAttributes(attributes),
                (String) attributes.get("email"),
                (String) attributes.get("name")
            ));
        } 
        
        // JWT 토큰으로 인증된 사용자인 경우
        if (authentication.getPrincipal() instanceof Long userId) {
            try {
                var userOpt = userService.findUserById(userId);
                if (userOpt.isPresent()) {
                    var user = userOpt.get();
                    var profileOpt = userService.findUserProfileById(userId);
                    
                    return ResponseEntity.ok(StatusResponse.authenticated(
                        "jwt",
                        profileOpt.map(p -> p.getEmail()).orElse(""),
                        profileOpt.map(p -> p.getName()).orElse(user.getUsername())
                    ));
                }
            } catch (Exception e) {
                // 예외 발생 시 인증되지 않은 상태로 처리
            }
        }
        
        return ResponseEntity.ok(StatusResponse.unauthenticated());
    }

    // OAuth2 로그인 URL 제공
    @GetMapping("/social-login-urls")
    public ResponseEntity<Map<String, String>> getLoginUrls() {
        Map<String, String> loginUrls = new HashMap<>();
        
        // Google 로그인 URL
        loginUrls.put(SocialProvider.GOOGLE.name().toLowerCase(), "/oauth2/authorization/google");
        
        // Kakao 로그인 URL
        loginUrls.put(SocialProvider.KAKAO.name().toLowerCase(), "/oauth2/authorization/kakao");
        
        return ResponseEntity.ok(loginUrls);
    }

    // 소셜 제공자 정보 추출
    private String getProviderFromAttributes(Map<String, Object> attributes) {
        SocialProvider provider = SocialProvider.fromAttributes(attributes);
        return provider != null ? provider.getLowerCaseName() : "unknown";
    }
}
