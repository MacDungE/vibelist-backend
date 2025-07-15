package org.example.vibelist.domain.elasticsearch.dto;

import lombok.Data;

@Data
public class TrackMetrics {
    //track 정보
    private Long trackId; //이걸로 변경할 거 고려해야 함
    private String title;
    private String artist;
    private String album;
    private int popularity;
    private boolean explicit;
    private String imageUrl;
}