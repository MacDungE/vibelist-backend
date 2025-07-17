package org.example.vibelist.domain.auth.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthLoggingService {
    private final RestTemplate restTemplate;

    @Async
    public void logAuthEvent(String userId, String eventType, HttpServletRequest request) {
        Map<String,Object> logEntry = new HashMap<>();
        logEntry.put("userId", userId);
        logEntry.put("eventType", eventType);
        logEntry.put("ip", request.getRemoteAddr());
        logEntry.put("userAgent", request.getHeader("User-Agent"));
        logEntry.put("timestamp", LocalDateTime.now());

        try{
            restTemplate.postForEntity("http://localhost:8080/auth-log/log", logEntry, String.class);
        }
        catch (Exception e){
            log.warn("Elasticsearch log 전송 실패 : {}", e.getMessage());
        }
    }
}
