package org.example.vibelist.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.global.constants.TokenConstants;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String token = getTokenFromRequest(request);
            
            if (StringUtils.hasText(token)) {
                log.debug("JWT 토큰 발견: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
                
                if (jwtTokenProvider.validateToken(token)) {
                    // 토큰 타입이 ACCESS인지 확인
                    String tokenType = jwtTokenProvider.getTokenType(token);
                    if (JwtTokenType.ACCESS.equals(tokenType)) {
                        Authentication authentication = jwtTokenProvider.getAuthentication(token);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("JWT 토큰 인증 성공: 사용자={}, 권한={}",
                                jwtTokenProvider.getCustomUserDetails(authentication).getUsername(),
                                authentication.getAuthorities());
                    } else {
                        log.warn("잘못된 토큰 타입: {} (ACCESS 토큰이 필요함)", tokenType);
                    }
                } else {
                    log.warn("유효하지 않은 JWT 토큰");
                }
            } else {
                log.debug("JWT 토큰이 요청에 포함되지 않음: {}", request.getRequestURI());
            }
        } catch (Exception e) {
            log.error("JWT 토큰 처리 중 오류 발생: {}", e.getMessage(), e);
            // 인증 컨텍스트를 클리어하여 보안상 안전하게 처리
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        // Priority 1: Authorization header (새로운 표준 방식)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            log.debug("Authorization 헤더에서 토큰 추출: {}...", token.substring(0, Math.min(token.length(), 10)));
            return token;
        }
        
        // Priority 2: 쿠키에서 access token 검색 (하위 호환성을 위한 fallback)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (TokenConstants.ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        log.debug("쿠키에서 토큰 추출 (레거시 방식): {}...", token.substring(0, Math.min(token.length(), 10)));
                        log.warn("쿠키 기반 인증이 사용되었습니다. Authorization 헤더 사용을 권장합니다.");
                        return token;
                    }
                }
            }
        }
        
        return null;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // 정적 리소스나 인증 관련 엔드포인트는 필터링하지 않음
        return path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") || 
               path.startsWith("/assets/") || 
               path.startsWith("/v1/auth/signup") ||
               path.startsWith("/v1/auth/login") ||
               path.startsWith("/v1/auth/refresh") ||
               path.startsWith("/v1/auth/social/complete-signup") ||
               path.startsWith("/health/") ||
               path.startsWith("/actuator/") ||
               path.equals("/favicon.ico");
    }
} 