package org.example.vibelist.global.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.example.vibelist.global.security.core.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;


@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {
    private final LogSender logSender;

    // 모든 도메인에 대하 로그 발생
//    @Pointcut("execution(* org.example.vibelist.domain..*.*(..))")
//    public void allDomainMethods(){}
//    @AfterReturning(pointcut = "allDomainMethods()", returning = "result")
//    public void logAfterService(JoinPoint joinPoint, Object result) {
//        String domain = extractDomain(joinPoint);
//        String eventType = joinPoint.getSignature().getName().toUpperCase();
//        String userId = extractUserId(); // SecurityContext에서
//
//        UserLog log = UserLog.builder()
//                .userId(userId)
//                .domain(domain)
//                .eventType(eventType)
//                .timestamp(LocalDateTime.now())
//                .ip(extractClientIp())
//                .message("Called " + joinPoint.getSignature())
//                .build();
//
//        logSender.send(log);
//    }
    @Around("@annotation(userActivityLog)")
    public Object logUserAction(ProceedingJoinPoint pjp, UserActivityLog userActivityLog) throws Throwable {
        // 요청이 아닌 다른 스레드에서 실행된 것이라면 로깅 무시
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return pjp.proceed(); // 내부에서 호출된 경우 로그 남기지 않음
        }

        Object result = pjp.proceed(); // 실제 메서드 실행

        // 로그 기록
        UserLog logData = UserLog.builder()
                .userId(extractUserId())
                .ip(extractClientIp())
                .eventType(userActivityLog.action())
                .domain(extractDomain(pjp))
                .timestamp(LocalDateTime.now())
                .api(extractRequestDetails())
                .build();

        logSender.send(logData);
        return result;
    }

    /*
     *OAuth2 로그인시 id는 provider에 따라 String,Long,또는 char[]일 수 있습니다.
     * 명시적인 type check를 수행하여 id를 반환합니다.
     */
    private String extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "anonymous"; // 비로그인 사용자

        Object principal = auth.getPrincipal();
        if (principal instanceof OAuth2User user) {
            Object idAttr = user.getAttribute("userId");
            if (idAttr instanceof String id) {
                return id;
            } else if (idAttr instanceof Number num) {
                return num.toString();  // Long, Integer 등 처리
            } else if (idAttr instanceof char[] chars) {
                return new String(chars);
            } else if (idAttr != null) {
                return idAttr.toString();  // fallback
            } else {
                return "unknown";
            }
        }
        //CustomUserDetail 처리
        if (principal instanceof CustomUserDetails customUserDetails) {
            Long userId = customUserDetails.getId();
            return String.valueOf(userId);
        }
        return auth.getName(); // 일반 로그인 사용자
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

    private String extractRequestDetails() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String uri = request.getRequestURI(); // 예: /v1/post/1
            String query = request.getQueryString(); // 예: foo=bar
            if(query!=null){ //query값이 넘어올때
                String decodedQuery = URLDecoder.decode(query, StandardCharsets.UTF_8);
                return uri + "?" + decodedQuery;
            }
            return uri;
        }
        return "unknown";
    }
}




