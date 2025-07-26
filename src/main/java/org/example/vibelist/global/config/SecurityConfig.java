package org.example.vibelist.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.oauth2.*;
import org.example.vibelist.global.constants.TokenConstants;
import org.example.vibelist.global.response.GlobalException;
import org.example.vibelist.global.response.ResponseCode;
import org.example.vibelist.global.response.RsData;
import org.example.vibelist.global.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2UserService oAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LogoutSuccessHandler oAuth2LogoutSuccessHandler;
    private final CustomAuthorizationCodeTokenResponseClient customTokenResponseClient;
    private final CustomAuthorizationRequestResolver customAuthorizationRequestResolver;

    @Value("${frontend.url:https://your-production-domain.com}")
    private String frontendProdUrl;

    @Value("${frontend.dev.url:http://localhost:3000}")
    private String frontendDevUrl;


    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, ClientRegistrationRepository repo) throws Exception {
        log.info("[Security_CONFIG] DevSecurityConfig 초기화 시작");
        log.info("[Security_CONFIG] OAuth2UserService: {}", oAuth2UserService.getClass().getName());
        
        http
                // CORS 설정 추가
               .cors(cors -> cors.configurationSource(corsConfigurationSource()))
               // CSRF(Cross-Site Request Forgery) 보호 기능을 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // 세션 관리 정책을 STATELESS로 설정 (JWT 사용 시 일반적)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // HTTP 요청에 대한 인증/인가 규칙 설정
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스 및 기본 페이지 허용
                        .requestMatchers("/", "/*.html", "/favicon.ico",
                                "/css/**", "/js/**", "/images/**", "/assets/**",
                                "/.well-known/**").permitAll()
                        // 인증 관련 엔드포인트 허용
                        .requestMatchers("/v1/auth/**").permitAll()
                        // OAuth2 관련 엔드포인트 허용 (쿼리 파라미터 포함)
                        .requestMatchers("/login/oauth2/**", "/oauth2/**", "/v1/oauth2/**").permitAll()
                        // OAuth2 인증 엔드포인트 명시적 허용 (integration_user_id 파라미터 포함)
                        .requestMatchers("/oauth2/authorization/**").permitAll()
                        // 사용자명 설정 페이지 허용
                        .requestMatchers("/setup/username").permitAll()
                        // 헬스체크 및 모니터링 엔드포인트 허용
                        .requestMatchers("/health/**", "/actuator/**", "/prometheus").permitAll()
                        // 웹소켓 엔드포인트 허용
                        .requestMatchers("/ws/**", "/websocket/**").permitAll()
                        // API 문서 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                        // 공개 API 엔드포인트 허용
                        .requestMatchers("/v1/public/**").permitAll()
                        // 탐색(검색/피드/트렌드) 자유롭게 허용
                        .requestMatchers("/v1/explore/**").permitAll()
                        // 게시글, 댓글, 좋아요 관련 API는 모두 허용하고 컨트롤러에서 세밀하게 제어
                        .requestMatchers("/v1/post/**", "/v1/comment/**", "/v1/like/**").permitAll()

                        // 좋아요 조회
                        .requestMatchers("/v1/users/{postname}/profile").permitAll()

                        // 플레이리스트, 태그, 추천 등 기타 공개 API
                        .requestMatchers("/v1/playlist/**").permitAll()
                        .requestMatchers("/v1/tag/**").permitAll()
                        .requestMatchers("/v1/recommend").permitAll()
                        // 사용자 관련 엔드포인트는 인증 필요
                        .requestMatchers("/v1/user/**").authenticated()
                        // 개발용 인증
                        .requestMatchers("/v1/dev_auth/**").permitAll()
                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                // 인증 실패시 예외처리
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(customAuthenticationEntryPoint())
                        .accessDeniedHandler(customAccessDeniedHandler())
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
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(new CustomAuthorizationRequestResolver(repo)))
                )
                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(oAuth2LogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .deleteCookies(TokenConstants.ACCESS_TOKEN_COOKIE, TokenConstants.REFRESH_TOKEN_COOKIE)
                        .permitAll()
                )
                // JWT 토큰 필터를 UsernamePasswordAuthenticationFilter 이전에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(frontendDevUrl, frontendProdUrl, "https://local.vibelist.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public HttpSessionOAuth2AuthorizationRequestRepository authorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            log.warn("[SECURITY] 인증 실패 - URI: {}, 사유: {}", request.getRequestURI(), authException.getMessage());

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");

            // RsData를 사용하여 응답 생성
            RsData<Void> errorResponse = RsData.fail(
                    ResponseCode.AUTH_REQUIRED,
                    "인증이 필요합니다"
            );

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

            String jsonResponse = mapper.writeValueAsString(errorResponse);
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.warn("[SECURITY] 접근 권한 없음 - URI: {}, 사유: {}", request.getRequestURI(), accessDeniedException.getMessage());

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json;charset=UTF-8");

            // RsData를 사용하여 응답 생성
            RsData<Void> errorResponse = RsData.fail(
                    ResponseCode.AUTH_FORBIDDEN,
                    "접근 권한이 없습니다"
            );

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

            String jsonResponse = mapper.writeValueAsString(errorResponse);
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
        };
    }
}
