package org.example.vibelist.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.auth.dto.CompleteSocialSignupRequest;
import org.example.vibelist.domain.auth.dto.StatusResponse;
import org.example.vibelist.domain.auth.dto.TokenResponse;
import org.example.vibelist.domain.auth.service.AuthService;
import org.example.vibelist.domain.auth.util.CookieUtil;

import org.example.vibelist.global.aop.UserActivityLog;
import org.example.vibelist.global.constants.SocialProviderConstants;
import org.example.vibelist.global.constants.TokenConstants;
import org.example.vibelist.domain.user.dto.SocialAccountResponse;
import org.example.vibelist.domain.user.service.UserService;
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
@Tag(name = "인증 관리", description = "사용자 인증 및 권한 관리 API")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final CookieUtil cookieUtil;

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용해 새로운 액세스 토큰을 발급받습니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        @ApiResponse(responseCode = "401", description = "리프레시 토큰이 없거나 유효하지 않음")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @Parameter(description = "리프레시 토큰 (쿠키)")
            @CookieValue(name = TokenConstants.REFRESH_TOKEN_COOKIE, required = false) String refreshToken, 
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

    @Operation(summary = "로그아웃", description = "사용자를 로그아웃하고 토큰을 무효화합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @SecurityRequirement(name = "bearer-key")
    @PostMapping("/logout")//AOP에 전송
    @UserActivityLog(action = "LOGOUT")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok("로그아웃 성공");
    }

    @Operation(summary = "현재 사용자 소셜 계정 조회", description = "현재 인증된 사용자의 소셜 로그인 연동 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "소셜 계정 조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearer-key")
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

    @Operation(summary = "사용자 소셜 계정 조회", description = "특정 사용자의 소셜 로그인 연동 정보를 조회합니다. (관리자용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "소셜 계정 조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/{userId}/social")
    public ResponseEntity<?> getUserSocialAccounts(
            @Parameter(description = "조회할 사용자 ID") @PathVariable Long userId) {
        List<SocialAccountResponse> socialAccounts = userService.findUserSocialAccounts(userId);
        return ResponseEntity.ok(socialAccounts);
    }

    @Operation(summary = "소셜 회원가입 완료", description = "소셜 로그인 후 사용자명을 설정하여 회원가입을 완료합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원가입 완료 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (중복된 사용자명 등)")
    })
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

    @Operation(summary = "인증 상태 확인", description = "현재 사용자의 인증 상태와 정보를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "인증 상태 조회 성공")
    })
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

    @Operation(summary = "소셜 로그인 URL 조회", description = "Google, Kakao, Spotify 등 소셜 로그인 URL을 제공합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 URL 조회 성공")
    })
    @GetMapping("/social-login-urls")
    public ResponseEntity<Map<String, String>> getLoginUrls() {
        Map<String, String> loginUrls = new HashMap<>();
        
        // Google 로그인 URL
        loginUrls.put(SocialProviderConstants.GOOGLE_LOWER, SocialProviderConstants.getOAuth2LoginUrl(SocialProviderConstants.GOOGLE_LOWER));
        
        // Kakao 로그인 URL
        loginUrls.put(SocialProviderConstants.KAKAO_LOWER, SocialProviderConstants.getOAuth2LoginUrl(SocialProviderConstants.KAKAO_LOWER));
        
        // Spotify 로그인 URL
        loginUrls.put(SocialProviderConstants.SPOTIFY_LOWER, SocialProviderConstants.getOAuth2LoginUrl(SocialProviderConstants.SPOTIFY_LOWER));
        
        return ResponseEntity.ok(loginUrls);
    }

    // 소셜 제공자 정보 추출
    private String getProviderFromAttributes(Map<String, Object> attributes) {
        String provider = SocialProviderConstants.fromAttributes(attributes);
        return provider != null ? SocialProviderConstants.getLowerCaseName(provider) : "unknown";
    }
}
