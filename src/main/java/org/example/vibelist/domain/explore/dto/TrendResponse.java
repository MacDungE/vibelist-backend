package org.example.vibelist.domain.explore.dto;


import org.example.vibelist.domain.explore.entity.PostTrend;

import java.time.LocalDateTime;

/** 트렌드 게시글 응답 DTO (미리보기용) */
public record TrendResponse(
        Long            postId,
        Double          score,
        Integer         rank,
        Integer         previousRank, // 💡 이전 순위 추가
        PostTrend.TrendStatus trendStatus,
        Integer         rankChange, // 💡 순위 변화량 추가
        String          postContent,
        String          userName,
        String          userProfileName,
        LocalDateTime   snapshotTime
) {}
