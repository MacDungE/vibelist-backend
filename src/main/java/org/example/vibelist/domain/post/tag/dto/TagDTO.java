package org.example.vibelist.domain.post.tag.dto;

import org.example.vibelist.domain.post.tag.entity.Tag;

public record TagDTO(Long id, String name){
    public static TagDTO from(Tag tag) {
        return new TagDTO(tag.getId(), tag.getName());
    }
}
