package org.example.vibelist.domain.auth.util;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.auth.dto.LoginResponse;
import org.example.vibelist.domain.auth.dto.TokenResponse;
import org.example.vibelist.global.constants.Role;
import org.example.vibelist.global.security.jwt.JwtTokenProvider;
import org.example.vibelist.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUtil {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * 사용자 정보로부터 TokenResponse 생성
     */
    public TokenResponse createTokenResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        
        return TokenResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .build();
    }
    
    /**
     * 사용자 정보로부터 LoginResponse 생성 (refresh token 포함)
     */
    public LoginResponse createLoginResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        
        return LoginResponse.builder()
                .tokenResponse(TokenResponse.builder()
                        .tokenType("Bearer")
                        .accessToken(accessToken)
                        .build())
                .refreshToken(refreshToken)
                .build();
    }
    
    /**
     * 사용자 ID, 사용자명, 역할로부터 TokenResponse 생성
     */
    public TokenResponse createTokenResponse(Long userId, String username, Role role) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId, username, role);
        
        return TokenResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .build();
    }
} 