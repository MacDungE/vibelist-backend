package org.example.vibelist.global.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogSender {
    private final ObjectMapper objectMapper ;
    public LogSender(){
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Jackson에서 직렬화 등록, 기본적으로 지원을 안함
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    }

    public void send(UserLog logData){
        try{
            String jsonLog = objectMapper.writeValueAsString(logData);
            log.info(jsonLog);
        }
        catch(JsonProcessingException e){
            e.printStackTrace();
            log.error("로그 직렬화 실패",e);
        }
    }
}
