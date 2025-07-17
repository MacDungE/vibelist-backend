package org.example.vibelist.domain.auth.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuthLoggingAspect {
    private final AuthLoggingService authLoggingService;
    @Pointcut("execution(* org.springframework.security.web.authentication.AuthenticationSuccessHandler.onAuthenticationSuccess(..))")
    public void loginSuccess() {}

    @Pointcut("execution(* org.springframework.security.web.authentication.logout.LogoutSuccessHandler.onLogoutSuccess(..))")
    public void logoutSuccess() {}

    @After("loginSuccess()")
    public void afterLoginSuccess(JoinPoint joinPoint) {
        HttpServletRequest request = (HttpServletRequest) joinPoint.getArgs()[0];
        Authentication auth = (Authentication) joinPoint.getArgs()[2];
        String userId = auth.getName(); // 또는 CustomUserDetails에서 ID 추출

        authLoggingService.logAuthEvent(userId, "LOGIN", request);
    }

    @After("logoutSuccess()")
    public void afterLogoutSuccess(JoinPoint joinPoint) {
        HttpServletRequest request = (HttpServletRequest) joinPoint.getArgs()[0];
        Authentication auth = (Authentication) joinPoint.getArgs()[2];
        String userId = auth.getName(); // 또는 CustomUserDetails에서 ID 추출

        authLoggingService.logAuthEvent(userId, "LOGOUT", request);
    }
}

