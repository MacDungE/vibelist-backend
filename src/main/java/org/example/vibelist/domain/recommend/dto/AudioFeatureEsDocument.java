package org.example.vibelist.domain.recommend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "audio_feature_index")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioFeatureEsDocument {
    private String spotifyId;



    public static AudioFeatureEsDocument from(TrackRsDto dto) {
        return AudioFeatureEsDocument.builder()
                .spotifyId(dto.getSpotifyId())
                .build();
    }
}
