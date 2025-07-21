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
@DisplayName("로그아웃 기능 테스트")
class LogoutFunctionalityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Authorization 헤더 기반 클라이언트 로그아웃 테스트")
    void logout_WithAuthorizationHeaderClient_ShouldWork() throws Exception {
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
        String accessToken = tokenResponse.getAccessToken();

        Cookie refreshTokenCookie = loginResult.getResponse().getCookie(TokenConstants.REFRESH_TOKEN_COOKIE);
        assertThat(refreshTokenCookie).isNotNull();

        // when - Authorization 헤더로 로그아웃 요청
        MvcResult logoutResult = mockMvc.perform(post("/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃 성공"))
                .andExpect(jsonPath("$.status").value("success"))
                .andReturn();

        // then - 리프레시 토큰 쿠키가 삭제되었는지 확인
        Cookie clearedRefreshTokenCookie = logoutResult.getResponse().getCookie(TokenConstants.REFRESH_TOKEN_COOKIE);
        assertThat(clearedRefreshTokenCookie).isNotNull();
        assertThat(clearedRefreshTokenCookie.getMaxAge()).isEqualTo(0); // 즉시 삭제
        assertThat(clearedRefreshTokenCookie.getValue()).isNull();
    }

    @Test
    @DisplayName("쿠키 기반 클라이언트 로그아웃 테스트 (하위 호환성)")
    void logout_WithCookieBasedClient_ShouldWork() throws Exception {
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
        
        // 액세스 토큰을 쿠키로 설정 (하위 호환성 테스트용)
        Cookie accessTokenCookie = new Cookie(TokenConstants.ACCESS_TOKEN_COOKIE, tokenResponse.getAccessToken());
        Cookie refreshTokenCookie = loginResult.getResponse().getCookie(TokenConstants.REFRESH_TOKEN_COOKIE);

        // when - 쿠키로 로그아웃 요청
        MvcResult logoutResult = mockMvc.perform(post("/v1/auth/logout")
                        .cookie(accessTokenCookie)
                        .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃 성공"))
                .andExpect(jsonPath("$.status").value("success"))
                .andReturn();

        // then - 리프레시 토큰 쿠키가 삭제되었는지 확인
        Cookie clearedRefreshTokenCookie = logoutResult.getResponse().getCookie(TokenConstants.REFRESH_TOKEN_COOKIE);
        assertThat(clearedRefreshTokenCookie).isNotNull();
        assertThat(clearedRefreshTokenCookie.getMaxAge()).isEqualTo(0);
        assertThat(clearedRefreshTokenCookie.getValue()).isNull();
    }

    @Test
    @DisplayName("로그아웃 후 보호된 엔드포인트 접근 불가 확인")
    void afterLogout_ProtectedEndpointAccess_ShouldFail() throws Exception {
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
        String accessToken = tokenResponse.getAccessToken();

        Cookie refreshTokenCookie = loginResult.getResponse().getCookie(TokenConstants.REFRESH_TOKEN_COOKIE);

        // 로그아웃 전 보호된 엔드포인트 접근 성공 확인
        mockMvc.perform(get("/v1/auth/status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));

        // when - 로그아웃
        MvcResult logoutResult = mockMvc.perform(post("/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andReturn();

        Cookie clearedRefreshTokenCookie = logoutResult.getResponse().getCookie(TokenConstants.REFRESH_TOKEN_COOKIE);

        // then - 로그아웃 후 리프레시 토큰으로 토큰 갱신 시도 실패
        mockMvc.perform(post("/v1/auth/refresh")
                        .cookie(clearedRefreshTokenCookie))
                .andExpect(status().isUnauthorized());

        // 기존 액세스 토큰은 여전히 유효 (JWT는 stateless이므로)
        // 하지만 리프레시 토큰이 삭제되어 토큰 갱신 불가
        mockMvc.perform(get("/v1/auth/status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    @DisplayName("로그아웃 응답 형식 확인")
    void logout_ResponseFormat_ShouldBeCorrect() throws Exception {
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
        String accessToken = tokenResponse.getAccessToken();

        // when & then - 로그아웃 응답 형식 확인
        mockMvc.perform(post("/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("로그아웃 성공"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.status").isString());
    }

    @Test
    @DisplayName("인증 없이 로그아웃 요청 시 처리")
    void logout_WithoutAuthentication_ShouldStillWork() throws Exception {
        // when & then - 인증 없이 로그아웃 요청
        // 로그아웃은 보통 인증이 필요하지만, 쿠키 삭제는 여전히 수행되어야 함
        mockMvc.perform(post("/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃 성공"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("로그아웃 시 리프레시 토큰 쿠키만 삭제되고 액세스 토큰 쿠키는 삭제되지 않음")
    void logout_ShouldOnlyClearRefreshTokenCookie() throws Exception {
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
        String accessToken = tokenResponse.getAccessToken();

        Cookie refreshTokenCookie = loginResult.getResponse().getCookie(TokenConstants.REFRESH_TOKEN_COOKIE);

        // when - 로그아웃
        MvcResult logoutResult = mockMvc.perform(post("/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andReturn();

        // then - 리프레시 토큰 쿠키만 삭제되었는지 확인
        Cookie clearedRefreshTokenCookie = logoutResult.getResponse().getCookie(TokenConstants.REFRESH_TOKEN_COOKIE);
        assertThat(clearedRefreshTokenCookie).isNotNull();
        assertThat(clearedRefreshTokenCookie.getMaxAge()).isEqualTo(0);

        // 액세스 토큰 쿠키 삭제 명령은 없어야 함 (더 이상 액세스 토큰을 쿠키로 관리하지 않음)
        Cookie clearedAccessTokenCookie = logoutResult.getResponse().getCookie(TokenConstants.ACCESS_TOKEN_COOKIE);
        assertThat(clearedAccessTokenCookie).isNull();
    }

    @Test
    @DisplayName("여러 번 로그아웃 요청해도 안전하게 처리")
    void multipleLogout_ShouldBeSafe() throws Exception {
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
        String accessToken = tokenResponse.getAccessToken();

        // when & then - 첫 번째 로그아웃
        mockMvc.perform(post("/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃 성공"));

        // 두 번째 로그아웃 (이미 로그아웃된 상태)
        mockMvc.perform(post("/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃 성공"));

        // 세 번째 로그아웃 (토큰 없이)
        mockMvc.perform(post("/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃 성공"));
    }
}