package org.example.vibelist.domain.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;


@Entity
@Getter
@Setter
public class DevAuthToken {
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
    private Instant expiresIn;

}
