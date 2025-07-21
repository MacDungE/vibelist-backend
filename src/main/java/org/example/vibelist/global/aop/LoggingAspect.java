package org.example.vibelist.global.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;


@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuthLoggingAspect {
    private final LogSender logSender;

    @Pointcut("execution(* org.example.vibelist.domain..*.*(..))")
    public allDomainMethods(){}
    @AfterReturning(pointcut = "allDomainMethods()", returning = "result")
    public void logAfterService(JoinPoint joinPoint, Object result) {
        String domain = extractDomain(joinPoint);
        String eventType = joinPoint.getSignature().getName().toUpperCase();
        String userId = extractUserId(); // SecurityContext에서

        UserLog log = UserLog.builder()
                .userId(userId)
                .domain(domain)
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .message("Called " + joinPoint.getSignature())
                .build();

        logSender.send(log);
    }

    private String extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "anonymous";
        Object principal = auth.getPrincipal();
        if (principal instanceof OAuth2User user) {
            return String.valueOf(user.getAttribute("id"));
        }
        return auth.getName(); // username
    }

    private String extractDomain(JoinPoint joinPoint) {
        String fullClassName = joinPoint.getTarget().getClass().getName();
        if (fullClassName.contains("user")) return "user";
        else if (fullClassName.contains("post")) return "post";
        else return "unknown";
    }
}


}

