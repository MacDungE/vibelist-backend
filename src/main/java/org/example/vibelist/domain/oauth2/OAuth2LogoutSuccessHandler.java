package org.example.vibelist.domain.oauth2;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.global.aop.UserActivityLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
public class OAuth2LogoutSuccessHandler implements LogoutSuccessHandler {

    @Value("${frontend.logout.url}")
    private String logoutUrl;

    // 카카오 로그아웃 리다이렉트 URI (환경에 맞게 설정)
    @Value("${oauth2.kakao.logout-redirect-uri:http://localhost:8080/login.html}")
    private String kakaoLogoutRedirectUri;
    
    // 카카오 REST API 키
    @Value("${oauth2.kakao.client-id}")
    private String kakaoClientId;

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {


        // 기본 리디렉션 URL → 일반 로그아웃 시 login.html로 이동
        String redirectUrl = logoutUrl;

        if (authentication != null && authentication.getPrincipal() instanceof DefaultOAuth2User auth2User) {

            Map<String, Object> attributes = auth2User.getAttributes();


            Object email = attributes.get("email");
            if (email != null && email.toString().endsWith("@gmail.com")) {

                log.info("구글 로그아웃입니다.");

                redirectUrl = "https://accounts.google.com/Logout";
            }

            // 카카오 로그인 사용자인 경우 (attributes에 'id' 키가 있음)
            else if (attributes.containsKey("id")) {

                log.info("카카오 로그아웃입니다.");

                redirectUrl = "https://kauth.kakao.com/oauth/logout?client_id=" + kakaoClientId
                        + "&logout_redirect_uri=" + kakaoLogoutRedirectUri;
            }
        }

        // 최종적으로 redirectUrl로 리디렉트
        response.sendRedirect(redirectUrl);

    }
}
