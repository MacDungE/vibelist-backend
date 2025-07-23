package org.example.vibelist.global.health.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.example.vibelist.global.response.RsData;
import org.example.vibelist.global.response.ResponseCode;

@RestController
@Tag(name = "시스템 상태", description = "애플리케이션 상태 확인 API")
public class HealthController {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${spring.application.name:default}")
    private String applicationName;

    @Operation(summary = "헬스 체크", description = "서버 상태 확인용 API")
    @GetMapping("/health")
    public ResponseEntity<RsData<String>> healthCheck() {
        return ResponseEntity.ok(RsData.success(ResponseCode.HEALTH_OK, "ok"));
    }
} 