package org.example.vibelist.global.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.global.constants.TokenConstants;
import org.example.vibelist.domain.oauth2.CustomAuthorizationCodeTokenResponseClient;
import org.example.vibelist.domain.oauth2.OAuth2LoginSuccessHandler;
import org.example.vibelist.domain.oauth2.OAuth2LogoutSuccessHandler;
import org.example.vibelist.domain.oauth2.OAuth2UserService;
import org.example.vibelist.global.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class ProdSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final OAuth2UserService oAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LogoutSuccessHandler oAuth2LogoutSuccessHandler;
    private final CustomAuthorizationCodeTokenResponseClient customTokenResponseClient;

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 추가
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // CSRF(Cross-Site Request Forgery) 보호 기능을 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // 세션 관리 정책을 STATELESS로 설정 (JWT 사용 시 일반적)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 보안 헤더 설정
                .headers(headers -> headers
                        .frameOptions().deny() // 운영 환경에서는 iframe 차단
                        .contentTypeOptions().and()
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                // HTTP 요청에 대한 인증/인가 규칙 설정
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스 및 기본 페이지 허용
                        .requestMatchers("/", "/*.html", "/favicon.ico",
                                "/css/**", "/js/**", "/images/**", "/assets/**",
                                "/.well-known/**").permitAll()
                                        // 인증 관련 엔드포인트 허용
                .requestMatchers("/v1/auth/**").permitAll()
                // OAuth2 관련 엔드포인트 허용
                .requestMatchers("/login/oauth2/**", "/oauth2/**", "/v1/oauth2/**").permitAll()
                // 헬스체크 및 모니터링 엔드포인트 허용
                        .requestMatchers("/health/**", "/actuator/health", "/actuator/prometheus").permitAll()
                        // 웹소켓 엔드포인트 허용
                        .requestMatchers("/ws/**", "/websocket/**").permitAll()
                        // 공개 API 엔드포인트 허용
                        .requestMatchers("/v1/public/**").permitAll()
                        // 사용자 관련 엔드포인트는 인증 필요
                        .requestMatchers("/v1/user/**").authenticated()
                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                // 인증 실패시 예외처리
                .exceptionHandling(e -> e
                        // 인증이 안된 사용자가 접근하려고할 때
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                        // 인증은 되었지만 권한이 없을 때
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                        })
                )
                // OAuth2 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService)
                        )
                        .tokenEndpoint(token -> token
                                .accessTokenResponseClient(customTokenResponseClient)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutSuccessHandler(oAuth2LogoutSuccessHandler)
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login.html")
                        .invalidateHttpSession(true)
                        .deleteCookies(TokenConstants.ACCESS_TOKEN_COOKIE, TokenConstants.REFRESH_TOKEN_COOKIE)
                )
                // JWT 토큰 필터를 UsernamePasswordAuthenticationFilter 이전에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
