package org.example.vibelist.domain.track.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrackDto {

    private String trackId;

    //Track 엔티티와 매핑되는 변수
    private String title;
    private String artist;
    private String album;

    //AudioFeature 엔티티와 매핑되는 변수
    private double danceability;
    private double energy;
    private int key;
    private double loudness;
    private int mood;
    private double speechiness;
    private double acousticness;
    private double instrumentalness;
    private double liveness;
    private double valence;
    private double tempo;
    private int timeSignature;
    private String genres;

    //Youtube 엔티티와 매핑되는 변수
    private String url;
    private int durationSeconds;
}
