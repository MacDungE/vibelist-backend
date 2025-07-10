package org.example.vibelist.domain.elasticsearch.dto;

import lombok.Data;

@Data
public class YoutubeMetrics {
    //Youtube 정보
    private String url;
    private int durationSeconds;
}
