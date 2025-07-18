package org.example.vibelist.domain.auth.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.example.vibelist.global.security.core.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;


@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuthLoggingAspect {
    private final AuthLoggingService authLoggingService;

    @Pointcut("execution(* org.springframework.security.web.authentication.AuthenticationSuccessHandler.onAuthenticationSuccess(..))")
    public void loginSuccess() {
    }

    @Pointcut("execution(* org.springframework.security.web.authentication.logout.LogoutSuccessHandler.onLogoutSuccess(..))")
    public void logoutSuccess() {
    }

    @After("loginSuccess()")
    public void afterLoginSuccess(JoinPoint joinPoint) {
        HttpServletRequest request = (HttpServletRequest) joinPoint.getArgs()[0];
        Authentication auth = (Authentication) joinPoint.getArgs()[2];
        Object principal = auth.getPrincipal();

        if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;
            Map<String, Object> attributes = oAuth2User.getAttributes();

            String userId = attributes.get("userId").toString(); // or "sub" or "id" depending on provider
            authLoggingService.logAuthEvent(userId, "LOGIN", request);
        } else {
            log.warn("Principal is not OAuth2User: {}", principal.getClass());
        }

    }

    @After("logoutSuccess()")
    public void afterLogoutSuccess(JoinPoint joinPoint) {
        HttpServletRequest request = (HttpServletRequest) joinPoint.getArgs()[0];
        Authentication auth = (Authentication) joinPoint.getArgs()[2];
        Object principal = auth.getPrincipal();

        if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;
            Map<String, Object> attributes = oAuth2User.getAttributes();

            String userId = attributes.get("userid").toString(); // or "sub" or "id" depending on provider
            authLoggingService.logAuthEvent(userId, "LOGOUT", request);
        } else {
            log.warn("Principal is not OAuth2User: {}", principal.getClass());
        }
    }
}

