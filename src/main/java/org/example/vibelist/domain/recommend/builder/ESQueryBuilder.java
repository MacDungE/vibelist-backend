package org.example.vibelist.domain.recommend.builder;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import org.example.vibelist.domain.recommend.dto.DoubleRange;
import org.example.vibelist.domain.recommend.dto.EmotionFeatureProfile;


public class ESQueryBuilder {

    public static Query build(EmotionFeatureProfile profile) {
        return BoolQuery.of(b -> {
            // danceability
            addRangeQuery(b, "danceability", profile.getDanceability());
            addRangeQuery(b, "energy", profile.getEnergy());
            addRangeQuery(b, "loudness", profile.getLoudness());
            addRangeQuery(b, "speechiness", profile.getSpeechiness());
            addRangeQuery(b, "acousticness", profile.getAcousticness());
            addRangeQuery(b, "instrumentalness", profile.getInstrumentalness());
            addRangeQuery(b, "liveness", profile.getLiveness());
            addRangeQuery(b, "valence", profile.getValence());
            addRangeQuery(b, "tempo", profile.getTempo());

            return b;
        })._toQuery();
    }

    private static void addRangeQuery(BoolQuery.Builder b, String field, DoubleRange range) {
        if (range == null) return;

        b.must(queryBuilder -> queryBuilder // queryBuilder는 co.elastic.clients.elasticsearch._types.query_dsl.Query.Builder 입니다.
                // Range 쿼리를 생성합니다.
                .range(rangeQueryBuilder -> rangeQueryBuilder // rangeQueryBuilder는 co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery.Builder 입니다.
                        // 쿼리를 적용할 필드를 설정합니다.
                        .field(field)
                        // 최소값 (Greater Than or Equal to)을 설정합니다.
                        // JsonData.of()를 사용하여 Double 값을 Elasticsearch가 이해하는 JSON 데이터로 변환합니다.
                        .gte(JsonData.of(range.getMin()))
                        // 최대값 (Less Than or Equal to)을 설정합니다.
                        .lte(JsonData.of(range.getMax()))
                )
        );
    }

}
