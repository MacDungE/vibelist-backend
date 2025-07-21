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

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("토큰 보안 테스트")
class TokenSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("리프레시 토큰이 응답 본문에 노출되지 않는지 확인")
    void refreshToken_ShouldNotBeExposedInResponseBody() throws Exception {
        // given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        // when
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        // then
        String responseBody = result.getResponse().getContentAsString();
        
        // 응답 본문에 리프레시 토큰이 포함되지 않았는지 확인
        assertThat(responseBody).doesNotContain("refreshToken");
        assertThat(responseBody).doesNotContain("refresh_token");
        
        // JSON 구조 확인
        TokenResponse tokenResponse = objectMapper.readValue(responseBody, TokenResponse.class);
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getAccessToken()).isNotNull();
        
        // 리프레시 토큰은 쿠키로만 설정되어야 함
        Cookie refreshTokenCookie = result.getResponse().getCookie(TokenConstants.REFRESH_TOKEN_COOKIE);
        assertThat(refreshTokenCookie).isNotNull();
        assertThat(refreshTokenCookie.getValue()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("액세스 토큰이 쿠키로 설정되지 않는지 확인")
    void accessToken_ShouldNotBeSetAsCookie() throws Exception {
        // given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        // when
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        // then
        // 액세스 토큰 쿠키가 설정되지 않았는지 확인
        Cookie accessTokenCookie = result.getResponse().getCookie(TokenConstants.ACCESS_TOKEN_COOKIE);
        assertThat(accessTokenCookie).isNull();
        
        // 응답 본문에는 액세스 토큰이 포함되어야 함
        String responseBody = result.getResponse().getContentAsString();
        TokenResponse tokenResponse = objectMapper.readValue(responseBody, TokenResponse.class);
        assertThat(tokenResponse.getAccessToken()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키의 보안 속성 확인")
    void refreshTokenCookie_ShouldHaveProperSecurityAttributes() throws Exception {
        // given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        // when
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        // then
        Cookie refreshTokenCookie = result.getResponse().getCookie(TokenConstants.REFRESH_TOKEN_COOKIE);
        assertThat(refreshTokenCookie).isNotNull();
        
        // HttpOnly 속성 확인 (XSS 공격 방지)
        assertThat(refreshTokenCookie.isHttpOnly()).isTrue();
        
        // Path 설정 확인
        assertThat(refreshTokenCookie.getPath()).isEqualTo("/");
        
        // MaxAge 설정 확인 (7일)
        assertThat(refreshTokenCookie.getMaxAge()).isEqualTo(7 * 24 * 60 * 60);
        
        // 토큰 값이 비어있지 않은지 확인
        assertThat(refreshTokenCookie.getValue()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("토큰 갱신 시 새로운 액세스 토큰이 쿠키로 설정되지 않는지 확인")
    void tokenRefresh_ShouldNotSetAccessTokenAsCookie() throws Exception {
        // given - 로그인하여 리프레시 토큰 획득
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshTokenCookie = loginResult.getResponse().getCookie(TokenConstants.REFRESH_TOKEN_COOKIE);

        // when - 토큰 갱신
        MvcResult refreshResult = mockMvc.perform(post("/v1/auth/refresh")
                        .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andReturn();

        // then
        // 새로운 액세스 토큰이 쿠키로 설정되지 않았는지 확인
        Cookie newAccessTokenCookie = refreshResult.getResponse().getCookie(TokenConstants.ACCESS_TOKEN_COOKIE);
        assertThat(newAccessTokenCookie).isNull();
        
        // 응답 본문에는 새로운 액세스 토큰이 포함되어야 함
        String responseBody = refreshResult.getResponse().getContentAsString();
        TokenResponse tokenResponse = objectMapper.readValue(responseBody, TokenResponse.class);
        assertThat(tokenResponse.getAccessToken()).isNotNull().isNotEmpty();
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("인증 우선순위 시스템이 올바르게 작동하는지 확인")
    void authenticationPrioritySystem_ShouldWorkCorrectly() throws Exception {
        // given - 유효한 토큰 획득
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

        // 유효한 토큰을 Authorization 헤더에, 무효한 토큰을 쿠키에 설정
        Cookie invalidCookie = new Cookie(TokenConstants.ACCESS_TOKEN_COOKIE, "invalid.token");

        // when & then - Authorization 헤더가 우선되어 인증 성공
        mockMvc.perform(get("/v1/auth/status")
                        .header("Authorization", "Bearer " + validToken)
                        .cookie(invalidCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));

        // 반대 상황: 무효한 Authorization 헤더, 유효한 쿠키
        Cookie validCookie = new Cookie(TokenConstants.ACCESS_TOKEN_COOKIE, validToken);

        mockMvc.perform(get("/v1/auth/status")
                        .header("Authorization", "Bearer invalid.token")
                        .cookie(validCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false)); // Authorization 헤더가 우선되어 인증 실패
    }

    @Test
    @DisplayName("민감한 정보가 토큰 응답에 포함되지 않는지 확인")
    void tokenResponse_ShouldNotContainSensitiveInformation() throws Exception {
        // given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        // when
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        // then
        String responseBody = result.getResponse().getContentAsString();
        
        // 민감한 정보들이 응답에 포함되지 않았는지 확인
        assertThat(responseBody).doesNotContain("password");
        assertThat(responseBody).doesNotContain("userId");
        assertThat(responseBody).doesNotContain("username");
        assertThat(responseBody).doesNotContain("role");
        assertThat(responseBody).doesNotContain("expiresIn");
        assertThat(responseBody).doesNotContain("refreshToken");
        
        // 필요한 정보만 포함되었는지 확인
        assertThat(responseBody).contains("tokenType");
        assertThat(responseBody).contains("accessToken");
        assertThat(responseBody).contains("Bearer");
    }

    @Test
    @DisplayName("잘못된 형식의 Authorization 헤더 보안 테스트")
    void malformedAuthorizationHeader_SecurityTest() throws Exception {
        // 다양한 잘못된 형식의 Authorization 헤더 테스트
        String[] malformedHeaders = {
            "Bearer", // 토큰 없음
            "bearer validtoken", // 소문자
            "Basic validtoken", // 잘못된 타입
            "Bearer ", // 빈 토큰
            "Bearer  ", // 공백만 있는 토큰
            "validtoken", // Bearer 접두사 없음
            "Bearer\nvalidtoken", // 개행 문자 포함
            "Bearer\tvalidtoken", // 탭 문자 포함
        };

        for (String malformedHeader : malformedHeaders) {
            // when & then
            mockMvc.perform(get("/v1/auth/status")
                            .header("Authorization", malformedHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authenticated").value(false));
        }
    }

    @Test
    @DisplayName("쿠키 조작 공격 방어 테스트")
    void cookieManipulationAttack_DefenseTest() throws Exception {
        // 다양한 쿠키 조작 시도
        String[] maliciousTokens = {
            "null",
            "undefined",
            "",
            " ",
            "javascript:alert('xss')",
            "<script>alert('xss')</script>",
            "../../etc/passwd",
            "admin.token.fake",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.fake.signature"
        };

        for (String maliciousToken : maliciousTokens) {
            Cookie maliciousCookie = new Cookie(TokenConstants.ACCESS_TOKEN_COOKIE, maliciousToken);
            
            // when & then - 모든 악의적인 토큰은 인증 실패해야 함
            mockMvc.perform(get("/v1/auth/status")
                            .cookie(maliciousCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authenticated").value(false));
        }
    }

    @Test
    @DisplayName("토큰 없이 보호된 엔드포인트 접근 시 적절한 응답")
    void protectedEndpoint_WithoutToken_ShouldReturnUnauthenticated() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    @DisplayName("만료된 토큰으로 접근 시 보안 처리 확인")
    void expiredToken_SecurityHandling() throws Exception {
        // given - 만료된 토큰 시뮬레이션 (실제로는 JWT 토큰 생성 로직에 의존)
        String expiredToken = "expired.jwt.token";
        
        // when & then
        mockMvc.perform(get("/v1/auth/status")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }
}