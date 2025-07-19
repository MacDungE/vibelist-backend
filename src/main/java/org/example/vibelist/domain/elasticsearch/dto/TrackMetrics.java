package org.example.vibelist.domain.elasticsearch.dto;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
public class TrackMetrics {
    //track 정보
    @Field(type = FieldType.Long)
    private Long trackId;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String artist;

    @Field(type = FieldType.Text)
    private String album;

    @Field(type = FieldType.Integer)
    private int popularity;

    @Field(type = FieldType.Boolean)
    private boolean explicit;

    @Field(type = FieldType.Keyword)
    private String imageUrl;
}