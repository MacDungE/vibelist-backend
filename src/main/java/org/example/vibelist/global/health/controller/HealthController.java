package org.example.vibelist.global.health.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${spring.application.name:default}")
    private String applicationName;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("stage", activeProfile);
        response.put("message", applicationName + " 서버가 정상적으로 실행 중입니다.");
        return ResponseEntity.ok(response);
    }
} 