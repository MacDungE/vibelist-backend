package org.example.vibelist.global.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
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
/**
     * Intercepts methods annotated with {@link UserActivityLog} to log user actions and request metadata.
     *
     * Captures user ID, client IP, event type, domain, timestamp, API request details, and request body,
     * then sends this information as a {@link UserLog} using the {@code logSender}. If the method is not
     * executed within an HTTP request context, logging is skipped.
     *
     * @param pjp the join point representing the intercepted method
     * @param userActivityLog the annotation instance providing event type information
     * @return the result of the intercepted method execution
     * @throws Throwable if the intercepted method throws any exception
     */

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
                .requestBody(extractRequestBody(pjp))
                .build();

        logSender.send(logData);
        return result;
    }

    /**
     * Extracts the user ID of the currently authenticated user.
     *
     * Handles various authentication types, including OAuth2 and custom user details, and returns the user ID as a string.
     * Returns "anonymous" if the user is not authenticated, or "unknown" if the user ID cannot be determined.
     *
     * @return the user ID as a string, or "anonymous"/"unknown" if not available
     */
    public String extractUserId() {
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
    /**
     * Determines the API domain based on the package name of the target class in the join point.
     *
     * @param joinPoint the join point representing the intercepted method call
     * @return the domain name (e.g., "auth", "batch", "explore", etc.), or "unknown" if no match is found
     */
    public String extractDomain(JoinPoint joinPoint) {
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
    /**
     * Retrieves the client's IP address from the current HTTP request.
     *
     * Attempts to extract the IP from the "X-Forwarded-For" header; if unavailable, falls back to the remote address.
     * Returns "unknown" if no HTTP request is present.
     *
     * @return the client's IP address, or "unknown" if not available
     */
    public String extractClientIp() {
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
    /**
     * Retrieves the current HTTP request's URI and decoded query string.
     *
     * @return the request URI with decoded query parameters if available, or "unknown" if no HTTP request is present
     */
    public String extractRequestDetails() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String uri = request.getRequestURI(); // 예: /v1/post/1
            String query = request.getQueryString(); // 예: foo=bar
            if(query!=null){ //query값이 넘어올때
                String decodedQuery = URLDecoder.decode(query, StandardCharsets.UTF_8);
                return uri + "?" + decodedQuery; // ex: v1/explore/search?q=우울함
            }
            return uri;
        }
        return "unknown";
    }
    /**
     * Extracts and serializes the first non-request/response argument from the join point as a JSON string.
     *
     * Iterates through the method arguments, skipping instances of {@code HttpServletRequest} and {@code HttpServletResponse}.
     * Attempts to serialize the first eligible argument to JSON. Returns {@code "body_parsing_error"} if serialization fails,
     * or {@code null} if no suitable argument is found.
     *
     * @param joinPoint the join point representing the method invocation
     * @return the serialized JSON string of the request body, "body_parsing_error" on serialization failure, or null if no body is present
     */
    public String extractRequestBody(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        ObjectMapper objectMapper = new ObjectMapper();
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) continue;

            try {
                return objectMapper.writeValueAsString(arg); // JSON 직렬화
            } catch (JsonProcessingException e) {
                return "body_parsing_error";
            }
        }
        return null;
    }
}




