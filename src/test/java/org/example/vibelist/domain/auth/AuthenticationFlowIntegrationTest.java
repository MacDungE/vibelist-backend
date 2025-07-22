package org.example.vibelist.domain.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.vibelist.domain.auth.dto.LoginRequest;
import org.example.vibelist.domain.auth.dto.TokenResponse;
import org.example.vibelist.global.constants.TokenConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.example.vibelist.global.config.SecurityConfig;
import org.example.vibelist.global.config.WebConfig;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("인증 플로우 통합 테스트")
@ContextConfiguration(classes = {SecurityConfig.class, WebConfig.class})
class AuthenticationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("완전한 로그인 플로우 - 새로운 토큰 응답 형식 확인")
    void completeLoginFlow_WithNewTokenResponseFormat() throws Exception {
        // given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        // when - 로그인 요청
        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                // 제거된 필드들이 응답에 포함되지 않는지 확인
                .andExpect(jsonPath("$.expiresIn").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(cookie().exists(TokenConstants.REFRESH_TOKEN_COOKIE))
                .andExpect(cookie().httpOnly(TokenConstants.REFRESH_TOKEN_COOKIE, true))
                .andReturn();

        // then - 응답에서 액세스 토큰 추출
        String responseBody = loginResult.getResponse().getContentAsString();
        TokenResponse tokenResponse = objectMapper.readValue(responseBody, TokenResponse.class);
        
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getAccessToken()).isNotNull().isNotEmpty();

        // 리프레시 토큰 쿠키 확인
        Cookie refreshTokenCookie = loginResult.getResponse().getCookie(TokenConstants.REFRESH_TOKEN_COOKIE);
        assertThat(refreshTokenCookie).isNotNull();
        assertThat(refreshTokenCookie.getValue()).isNotNull().isNotEmpty();
        assertThat(refreshTokenCookie.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("토큰 갱신 플로우 - 새로운 응답 형식 확인")
    void tokenRefreshFlow_WithNewResponseFormat() throws Exception {
        // given - 먼저 로그인하여 리프레시 토큰 획득
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshTokenCookie = loginResult.getResponse().getCookie(TokenConstants.REFRESH_TOKEN_COOKIE);
        assertThat(refreshTokenCookie).isNotNull();

        // when - 토큰 갱신 요청
        mockMvc.perform(post("/v1/auth/refresh")
                        .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                // 제거된 필드들이 응답에 포함되지 않는지 확인
                .andExpect(jsonPath("$.expiresIn").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist())
                // 새로운 액세스 토큰이 쿠키로 설정되지 않는지 확인
                .andExpect(cookie().doesNotExist(TokenConstants.ACCESS_TOKEN_COOKIE));
    }

    @Test
    @DisplayName("Authorization 헤더를 사용한 보호된 엔드포인트 접근")
    void protectedEndpointAccess_WithAuthorizationHeader() throws Exception {
        // given - 로그인하여 액세스 토큰 획득
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        TokenResponse tokenResponse = objectMapper.readValue(responseBody, TokenResponse.class);
        String accessToken = tokenResponse.getAccessToken();

        // when & then - Authorization 헤더로 보호된 엔드포인트 접근
        mockMvc.perform(get("/v1/auth/status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    @DisplayName("쿠키 기반 인증 하위 호환성 확인")
    void backwardCompatibility_WithCookieBasedAuthentication() throws Exception {
        // given - 로그인하여 토큰 획득
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        TokenResponse tokenResponse = objectMapper.readValue(responseBody, TokenResponse.class);
        
        // 액세스 토큰을 수동으로 쿠키로 설정 (하위 호환성 테스트용)
        Cookie accessTokenCookie = new Cookie(TokenConstants.ACCESS_TOKEN_COOKIE, tokenResponse.getAccessToken());

        // when & then - 쿠키로 보호된 엔드포인트 접근 (하위 호환성)
        mockMvc.perform(get("/v1/auth/status")
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    @DisplayName("Authorization 헤더가 쿠키보다 우선순위를 가지는지 확인")
    void authenticationPriority_HeaderOverCookie() throws Exception {
        // given - 유효한 토큰과 무효한 토큰 준비
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        TokenResponse tokenResponse = objectMapper.readValue(responseBody, TokenResponse.class);
        String validToken = tokenResponse.getAccessToken();
        String invalidToken = "invalid.token.value";

        // 무효한 토큰을 쿠키로, 유효한 토큰을 Authorization 헤더로 설정
        Cookie invalidAccessTokenCookie = new Cookie(TokenConstants.ACCESS_TOKEN_COOKIE, invalidToken);

        // when & then - Authorization 헤더의 유효한 토큰이 우선되어 인증 성공
        mockMvc.perform(get("/v1/auth/status")
                        .header("Authorization", "Bearer " + validToken)
                        .cookie(invalidAccessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 토큰 갱신 실패")
    void tokenRefresh_WithInvalidRefreshToken_ShouldFail() throws Exception {
        // given
        Cookie invalidRefreshTokenCookie = new Cookie(TokenConstants.REFRESH_TOKEN_COOKIE, "invalid.refresh.token");

        // when & then
        mockMvc.perform(post("/v1/auth/refresh")
                        .cookie(invalidRefreshTokenCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("리프레시 토큰 없이 토큰 갱신 요청 시 실패")
    void tokenRefresh_WithoutRefreshToken_ShouldFail() throws Exception {
        // when & then
        mockMvc.perform(post("/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("리프레시 토큰이 없습니다."));
    }

    @Test
    @DisplayName("유효하지 않은 Authorization 헤더로 보호된 엔드포인트 접근 실패")
    void protectedEndpointAccess_WithInvalidAuthorizationHeader_ShouldFail() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/auth/status")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    @DisplayName("Authorization 헤더 없이 보호된 엔드포인트 접근")
    void protectedEndpointAccess_WithoutAuthorizationHeader_ShouldReturnUnauthenticated() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }
}