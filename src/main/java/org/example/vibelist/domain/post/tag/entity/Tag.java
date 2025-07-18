package org.example.vibelist.domain.post.tag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "tags")
@Getter
@Setter
public class Tag {
    @Id
    @GeneratedValue
    private Long id;

    @Column(length = 40, unique = true)   // 태그 중복 방지
    private String name;
}
