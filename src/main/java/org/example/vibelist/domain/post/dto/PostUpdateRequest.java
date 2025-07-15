package org.example.vibelist.domain.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostUpdateRequest {

    private Long id;
    private String content;
    private Boolean isPublic;


}