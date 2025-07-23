package org.example.vibelist.global.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogSender {
    private final ObjectMapper objectMapper ;
    private static final Logger userLogger = LoggerFactory.getLogger("user-log"); //LOGSTASH에만 전송되는 로그들을 관리
    public LogSender(){
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Jackson에서 직렬화 등록, 기본적으로 지원을 안함
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    }

    public void send(UserLog logData){
        try{
            String jsonLog = objectMapper.writeValueAsString(logData);
            log.info("USER_LOG_PREVIEW: {}", jsonLog);

            MDC.put("userId", logData.getUserId());
            MDC.put("ip", logData.getIp());
            MDC.put("eventType", logData.getEventType());
            MDC.put("domain", logData.getDomain());
            MDC.put("timestamp", logData.getTimestamp().toString());
            MDC.put("api", logData.getApi());
            MDC.put("requestBody", logData.getRequestBody());

            userLogger.info("User Action Log");
        }
        catch(JsonProcessingException e){
            log.error("로그 직렬화 실패", e);
        } finally {
            MDC.clear();
        }
    }
}
