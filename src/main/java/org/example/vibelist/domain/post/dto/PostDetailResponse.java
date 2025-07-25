package org.example.vibelist.domain.post.dto;



import java.time.LocalDateTime;
import java.util.List;

/** 게시글 + 플레이리스트 상세 응답 */
public record PostDetailResponse(
        Long                    id,
        Long                    userId,
        String                  userName,
        String                  userProfileName,
        String                  content,
        List<String>            tags,
        Boolean                 isPublic,
        Long                    likeCnt,
        Long                    viewCnt,
        LocalDateTime           createdAt,
        LocalDateTime           updatedAt,
        PlaylistDetailResponse  playlist
) { }