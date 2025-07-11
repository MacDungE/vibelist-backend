package org.example.vibelist.domain.batch.elasticsearch.dto;

import lombok.Data;

@Data
public class TrackMetrics {
    //track 정보
    private Long id;
    private String title;
    private String artist;
    private String album;
    private int popularity;
    private boolean explicit;
    private String imageUrl;
}
