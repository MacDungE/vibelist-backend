package org.example.vibelist.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.vibelist.global.constants.TokenConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 테스트")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    private static final String BEARER_TOKEN = "Bearer " + VALID_TOKEN;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Authorization 헤더에서 토큰 추출 및 인증 성공")
    void doFilterInternal_WithValidAuthorizationHeader_ShouldAuthenticateSuccessfully() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getTokenType(VALID_TOKEN)).thenReturn(JwtTokenType.ACCESS);
        when(jwtTokenProvider.getAuthentication(VALID_TOKEN)).thenReturn(authentication);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext).setAuthentication(authentication);
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider).validateToken(VALID_TOKEN);
        verify(jwtTokenProvider).getTokenType(VALID_TOKEN);
        verify(jwtTokenProvider).getAuthentication(VALID_TOKEN);
    }

    @Test
    @DisplayName("쿠키에서 토큰 추출 및 인증 성공 (하위 호환성)")
    void doFilterInternal_WithValidCookieToken_ShouldAuthenticateSuccessfully() throws Exception {
        // given
        Cookie accessTokenCookie = new Cookie(TokenConstants.ACCESS_TOKEN_COOKIE, VALID_TOKEN);
        Cookie[] cookies = {accessTokenCookie};
        
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(cookies);
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getTokenType(VALID_TOKEN)).thenReturn(JwtTokenType.ACCESS);
        when(jwtTokenProvider.getAuthentication(VALID_TOKEN)).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testUser");
        when(authentication.getAuthorities()).thenReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext).setAuthentication(authentication);
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider).validateToken(VALID_TOKEN);
        verify(jwtTokenProvider).getTokenType(VALID_TOKEN);
        verify(jwtTokenProvider).getAuthentication(VALID_TOKEN);
    }

    @Test
    @DisplayName("Authorization 헤더가 쿠키보다 우선순위를 가져야 함")
    void doFilterInternal_WithBothHeaderAndCookie_ShouldPrioritizeAuthorizationHeader() throws Exception {
        // given
        String cookieToken = "cookie.token.value";
        Cookie accessTokenCookie = new Cookie(TokenConstants.ACCESS_TOKEN_COOKIE, cookieToken);
        Cookie[] cookies = {accessTokenCookie};
        
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(request.getCookies()).thenReturn(cookies);
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getTokenType(VALID_TOKEN)).thenReturn(JwtTokenType.ACCESS);
        when(jwtTokenProvider.getAuthentication(VALID_TOKEN)).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testUser");
        when(authentication.getAuthorities()).thenReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtTokenProvider).validateToken(VALID_TOKEN); // Authorization 헤더의 토큰 사용
        verify(jwtTokenProvider, never()).validateToken(cookieToken); // 쿠키 토큰은 사용하지 않음
        verify(securityContext).setAuthentication(authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 인증 실패")
    void doFilterInternal_WithInvalidToken_ShouldNotAuthenticate() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(request.getCookies()).thenReturn(null);
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(false);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(jwtTokenProvider, never()).getAuthentication(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("REFRESH 토큰 타입으로 인증 실패")
    void doFilterInternal_WithRefreshTokenType_ShouldNotAuthenticate() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(request.getCookies()).thenReturn(null);
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getTokenType(VALID_TOKEN)).thenReturn(JwtTokenType.REFRESH);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(jwtTokenProvider, never()).getAuthentication(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰이 없는 경우 인증하지 않고 필터 체인 계속 진행")
    void doFilterInternal_WithNoToken_ShouldContinueFilterChain() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 접두사가 없는 Authorization 헤더는 무시")
    void doFilterInternal_WithInvalidAuthorizationHeaderFormat_ShouldIgnore() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn("Basic " + VALID_TOKEN);
        when(request.getCookies()).thenReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("빈 Authorization 헤더는 무시")
    void doFilterInternal_WithEmptyAuthorizationHeader_ShouldIgnore() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn("");
        when(request.getCookies()).thenReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(securityContext, never()).setAuthentication(any());
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰 처리 중 예외 발생 시 SecurityContext 클리어 후 필터 체인 계속 진행")
    void doFilterInternal_WithException_ShouldClearSecurityContextAndContinue() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenThrow(new RuntimeException("Token validation error"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        // SecurityContextHolder.clearContext()가 호출되는지는 정적 메서드라 직접 검증하기 어려움
        // 하지만 예외가 발생해도 필터 체인이 계속 진행되는지 확인
    }

    @Test
    @DisplayName("여러 쿠키 중에서 ACCESS_TOKEN_COOKIE만 추출")
    void doFilterInternal_WithMultipleCookies_ShouldExtractCorrectCookie() throws Exception {
        // given
        Cookie otherCookie = new Cookie("other", "value");
        Cookie accessTokenCookie = new Cookie(TokenConstants.ACCESS_TOKEN_COOKIE, VALID_TOKEN);
        Cookie anotherCookie = new Cookie("another", "value");
        Cookie[] cookies = {otherCookie, accessTokenCookie, anotherCookie};
        
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(cookies);
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getTokenType(VALID_TOKEN)).thenReturn(JwtTokenType.ACCESS);
        when(jwtTokenProvider.getAuthentication(VALID_TOKEN)).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testUser");
        when(authentication.getAuthorities()).thenReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtTokenProvider).validateToken(VALID_TOKEN);
        verify(securityContext).setAuthentication(authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("빈 값을 가진 ACCESS_TOKEN_COOKIE는 무시")
    void doFilterInternal_WithEmptyAccessTokenCookie_ShouldIgnore() throws Exception {
        // given
        Cookie accessTokenCookie = new Cookie(TokenConstants.ACCESS_TOKEN_COOKIE, "");
        Cookie[] cookies = {accessTokenCookie};
        
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(cookies);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }
}