package org.example.vibelist.global.aop;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserLog {
    private String userId;
    private String eventType; // ex: LOGIN,CREATE_POST,CREATE_PLAYLIST, etc
    private String domain; // ex: user,post, etc
    private LocalDateTime timestamp;
    private String ip;
    private String message;
}
