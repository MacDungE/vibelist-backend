package org.example.vibelist.domain.integration.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.vibelist.global.jpa.entity.BaseTime;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
public class DevIntegrationTokenInfo extends BaseTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "text",nullable = false)
    private String accessToken;
    @Column(columnDefinition = "text",nullable = false)
    private String refreshToken;
    @Column(nullable = false)
    private LocalDateTime tokenExpiresAt; // 토큰 만료 시간
}
