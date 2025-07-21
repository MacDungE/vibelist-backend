package org.example.vibelist.global.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        logEntry.put("@timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        try{
            restTemplate.postForEntity("http://localhost:9200/auth-log/_doc", logEntry, String.class);
            log.info("Elasticsearch에 전송 성공 : {}", logEntry.toString());
        }
        catch (Exception e){
            log.warn("Elasticsearch log 전송 실패 : {}", e.getMessage());
        }
    }
}
