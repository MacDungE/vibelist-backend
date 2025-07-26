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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
        // 요청 스레드가 아닌 경우 로그 제외
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return pjp.proceed();
        }

        double start = System.currentTimeMillis(); // 시작 시간 측정
        Object result;
        try {
            result = pjp.proceed(); // 실제 메서드 실행
            return result;
        } finally {
            double end = System.currentTimeMillis(); // 종료 시간 측정
            double duration = end - start;

            UserLog logData = UserLog.builder()
                    .userId(extractUserId())
                    .ip(extractClientIp())
                    .eventType(userActivityLog.action())
                    .domain(extractDomain(pjp))
                    .timestamp(LocalDateTime.now())
                    .api(extractRequestDetails())
                    .requestBody(extractRequestBody(pjp))
                    .duration(duration) // ⬅️ 여기 추가
                    .build();

            logSender.send(logData);
        }
    }

    /*
     *OAuth2 로그인시 id는 provider에 따라 String,Long,또는 char[]일 수 있습니다.
     * 명시적인 type check를 수행하여 id를 반환합니다.
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
    /*
    호출되는 API의 domain를 반환합니다.
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
    /*
    요청자의 IP를 추출합니다.
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
    /*
    호출되는 API르 반환합니다.
     */
    public String extractRequestDetails() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";
        /* oauth2 로그인 로직*/
        HttpServletRequest request = attrs.getRequest();
        String uri = request.getRequestURI(); // 예: /login/oauth2/code/google

        if (uri == null) return "unknown";

        // /login/oauth2/code/{provider} 에서 provider만 추출
        Pattern pattern = Pattern.compile("^/login/oauth2/code/([^/?]+)");
        Matcher matcher = pattern.matcher(uri);
        if (matcher.find()) {
            String provider = matcher.group(1); // google, kakao, spotify
            return "/login/oauth2/" + provider;
        }

        // 게시글 상세/삭제/수정: /v1/post/{id}
        if (uri.matches("^/v1/post/[^/]+$") && request.getMethod().matches("GET|DELETE|PATCH")) {
            return "/v1/post/{id}";
        }

        // 사용자가 작성한 게시글 목록: /v1/post/{username}/posts
        if (uri.matches("^/v1/post/[^/]+/posts$") && request.getMethod().equals("GET")) {
            return "/v1/post/{username}/posts";
        }

        // 사용자가 좋아요한 게시글 목록: /v1/post/{username}/likes
        if (uri.matches("^/v1/post/[^/]+/likes$") && request.getMethod().equals("GET")) {
            return "/v1/post/{username}/likes";
        }

        if (uri.matches("^/v1/comments/\\d+$") && request.getMethod().matches("PUT|DELETE")) {
            return "/v1/comments/{id}";
        }
        if (uri.equals("/v1/comments") && request.getMethod().matches("GET|POST")) {
            return "/v1/comments";
        }
        return uri; // fallback
    }
    /*
    Request body를 추출합니다.
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




