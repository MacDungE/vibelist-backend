package org.example.vibelist.domain.recommend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.vibelist.domain.elasticsearch.dto.TrackMetrics;
import org.springframework.data.elasticsearch.annotations.Document;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "audio_feature_index")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioFeatureEsDocument {
    private String spotifyId;
    private int durationMs;



    private TrackMetrics trackMetrics;

//    private String trackId;
//    private String title;
//    private String artist;
//    private String album;
//    private int popularity;
//    private String imageUrl;
//    private boolean explicit;
}

