package org.example.vibelist.domain.elasticsearch.dto;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Document(indexName = "audio_feature_index")
public class EsDoc {
    @Id
    private String id;

    @Field(type = FieldType.Double)
    private double danceability;

    @Field(type = FieldType.Double)
    private double energy;

    @Field(type = FieldType.Integer)
    private int key;

    @Field(type = FieldType.Double)
    private double loudness;

    @Field(type = FieldType.Integer)
    private int mode;

    @Field(type = FieldType.Double)
    private double speechiness;

    @Field(type = FieldType.Double)
    private double acousticness;

    @Field(type = FieldType.Double)
    private double instrumentalness;

    @Field(type = FieldType.Double)
    private double liveness;

    @Field(type = FieldType.Double)
    private double valence;

    @Field(type = FieldType.Double)
    private double tempo;

    @Field(type = FieldType.Integer)
    private int durationMs;

    @Field(type = FieldType.Double)
    private double timeSignature;

    @Field(type = FieldType.Keyword)
    private List<String> genres;

    @Field(type = FieldType.Keyword)
    private String spotifyId;

    @Field(type = FieldType.Object)
    private TrackMetrics trackMetrics;

    //--- track------//

}