package org.example.vibelist.domain.auth.entity;

import jakarta.persistence.*;


@Entity
public class DevAuthToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = true)
    private String refreshToken;
}
