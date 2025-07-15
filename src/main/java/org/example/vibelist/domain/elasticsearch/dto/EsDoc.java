package org.example.vibelist.domain.elasticsearch.dto;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.List;

@Data
@Document(indexName = "audio_feature_index")
public class EsDoc {
    @Id
    private String id;
    private double danceability;
    private double energy;
    private int key;
    private double loudness;
    private int mode;
    private double speechiness;
    private double acousticness;
    private double instrumentalness;
    private double liveness;
    private double valence;
    private double tempo;
    private int durationMs;
    private double timeSignature;
    private List<String> genres;//RDS에는 하나의 String으로 저장되어 있는데 나중에 ; 기준으로 Split해야함
    private String spotifyId; //playlist에 대한 id

    //--------audio feature -----//


    private TrackMetrics trackMetrics;

    //--- track------//

}