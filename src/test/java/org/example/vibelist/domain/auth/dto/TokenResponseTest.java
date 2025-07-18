package org.example.vibelist.domain.auth.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TokenResponse 테스트")
class TokenResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("기본 생성자로 TokenResponse 생성 시 tokenType이 null이어야 함")
    void defaultConstructor_ShouldCreateTokenResponseWithNullTokenType() {
        // given & when
        TokenResponse tokenResponse = new TokenResponse();
        
        // then
        assertThat(tokenResponse.getTokenType()).isNull();
        assertThat(tokenResponse.getAccessToken()).isNull();
    }

    @Test
    @DisplayName("accessToken만으로 TokenResponse 생성 시 tokenType이 Bearer로 설정되어야 함")
    void constructorWithAccessToken_ShouldSetTokenTypeToBearer() {
        // given
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        
        // when
        TokenResponse tokenResponse = new TokenResponse(accessToken);
        
        // then
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getAccessToken()).isEqualTo(accessToken);
    }

    @Test
    @DisplayName("Builder 패턴으로 TokenResponse 생성 시 tokenType이 자동으로 Bearer로 설정되어야 함")
    void builder_WithoutTokenType_ShouldDefaultToBearer() {
        // given
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        
        // when
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(accessToken)
                .build();
        
        // then
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getAccessToken()).isEqualTo(accessToken);
    }

    @Test
    @DisplayName("Builder 패턴으로 tokenType을 명시적으로 설정한 경우 해당 값이 유지되어야 함")
    void builder_WithExplicitTokenType_ShouldKeepSpecifiedValue() {
        // given
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        String customTokenType = "Custom";
        
        // when
        TokenResponse tokenResponse = TokenResponse.builder()
                .tokenType(customTokenType)
                .accessToken(accessToken)
                .build();
        
        // then
        assertThat(tokenResponse.getTokenType()).isEqualTo(customTokenType);
        assertThat(tokenResponse.getAccessToken()).isEqualTo(accessToken);
    }

    @Test
    @DisplayName("TokenResponse JSON 직렬화 시 올바른 형식이어야 함")
    void jsonSerialization_ShouldMatchExpectedFormat() throws Exception {
        // given
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(accessToken)
                .build();
        
        // when
        String json = objectMapper.writeValueAsString(tokenResponse);
        
        // then
        assertThat(json).contains("\"tokenType\":\"Bearer\"");
        assertThat(json).contains("\"accessToken\":\"" + accessToken + "\"");
        // 불필요한 필드들이 포함되지 않았는지 확인
        assertThat(json).doesNotContain("expiresIn");
        assertThat(json).doesNotContain("userId");
        assertThat(json).doesNotContain("username");
        assertThat(json).doesNotContain("role");
    }

    @Test
    @DisplayName("TokenResponse JSON 역직렬화 시 올바르게 객체가 생성되어야 함")
    void jsonDeserialization_ShouldCreateCorrectObject() throws Exception {
        // given
        String json = "{\"tokenType\":\"Bearer\",\"accessToken\":\"test.token.here\"}";
        
        // when
        TokenResponse tokenResponse = objectMapper.readValue(json, TokenResponse.class);
        
        // then
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getAccessToken()).isEqualTo("test.token.here");
    }

    @Test
    @DisplayName("TokenResponse 객체 동등성 비교가 올바르게 작동해야 함")
    void equality_ShouldWorkCorrectly() {
        // given
        String accessToken = "test.token";
        TokenResponse tokenResponse1 = TokenResponse.builder()
                .accessToken(accessToken)
                .build();
        TokenResponse tokenResponse2 = TokenResponse.builder()
                .accessToken(accessToken)
                .build();
        TokenResponse tokenResponse3 = TokenResponse.builder()
                .accessToken("different.token")
                .build();
        
        // then
        assertThat(tokenResponse1).isEqualTo(tokenResponse2);
        assertThat(tokenResponse1).isNotEqualTo(tokenResponse3);
        assertThat(tokenResponse1.hashCode()).isEqualTo(tokenResponse2.hashCode());
    }

    @Test
    @DisplayName("TokenResponse toString 메서드가 올바르게 작동해야 함")
    void toString_ShouldContainAllFields() {
        // given
        String accessToken = "test.token";
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(accessToken)
                .build();
        
        // when
        String toString = tokenResponse.toString();
        
        // then
        assertThat(toString).contains("tokenType=Bearer");
        assertThat(toString).contains("accessToken=" + accessToken);
    }

    @Test
    @DisplayName("빈 accessToken으로 TokenResponse 생성이 가능해야 함")
    void emptyAccessToken_ShouldBeAllowed() {
        // given
        String emptyAccessToken = "";
        
        // when
        TokenResponse tokenResponse = new TokenResponse(emptyAccessToken);
        
        // then
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getAccessToken()).isEqualTo(emptyAccessToken);
    }

    @Test
    @DisplayName("null accessToken으로 TokenResponse 생성이 가능해야 함")
    void nullAccessToken_ShouldBeAllowed() {
        // given & when
        TokenResponse tokenResponse = new TokenResponse(null);
        
        // then
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getAccessToken()).isNull();
    }
}