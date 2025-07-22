package org.example.vibelist.global.aop;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserLog {
    private String userId;
    private String ip; // ex: 127.0.0.1
    private String eventType; // ex: LOGIN,CREATE_POST,CREATE_PLAYLIST, etc
    private String domain; // ex: user,post, etc
    private String api;// ex: api/v1/post
    private String requestBody;
    private LocalDateTime timestamp;
}
