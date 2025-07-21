package org.example.vibelist.global.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;


@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {
    private final LogSender logSender;

    @Pointcut("execution(* org.example.vibelist.domain..*.*(..))")
    public void allDomainMethods(){}
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
                .ip(extractClientIp())
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
        String className = joinPoint.getTarget().getClass().getName();
        if (className.contains(".domain.auth.")) return "auth";
        if (className.contains(".domain.batch.")) return "batch";
        if (className.contains(".domain.explore.")) return "explore";
        if (className.contains(".domain.integration.")) return "integration";
        if (className.contains(".domain.oauth2.")) return "oauth2";
        if (className.contains(".domain.playlist.")) return "playlist";
        if (className.contains(".domain.post.")) return "post";
        if (className.contains(".domain.user.")) return "user";

        return "unknown";
    }
    private String extractClientIp() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            }
            return ip;
        }
        return "unknown";
    }
}




