package org.example.vibelist.domain.playlist.es.builder;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;
import org.example.vibelist.domain.playlist.emotion.profile.AudioFeatureRange;
import org.example.vibelist.domain.playlist.util.DoubleRange;
import org.example.vibelist.domain.playlist.emotion.profile.EmotionFeatureProfile;

import java.util.List;

// 감정 범위(valence, energy)를 기반으로 Elasticsearch 검색 쿼리를 생성하는 클래스
// 랜덤 추천을 위해 function_score + random_score 쿼리를 사용
public class ESQueryBuilder {

    // 기존 EmotionFeatureProfile용
    public static Query build(EmotionFeatureProfile profile) {
        return build(profile.getEnergy(), profile.getValence());
    }

    // 기존 방식도 유지 (valence/energy만)
    public static Query build(DoubleRange energy, DoubleRange valence) {
        Query innerQuery = BoolQuery.of(b -> {
            addRangeQuery(b, "energy", energy);
            addRangeQuery(b, "valence", valence);
            b.must(queryBuilder -> queryBuilder
                    .range(rq -> rq
                            .field("trackMetrics.popularity")
//                            .gte(JsonData.of(10))
                    )
            );
            return b;
        })._toQuery();

        return wrapFunctionScore(innerQuery);
    }

    // 새로운 AudioFeatureRange(여러 feature) 지원
    public static Query build(AudioFeatureRange range) {
        Query innerQuery = BoolQuery.of(b -> {
            addRangeQuery(b, "danceability", range.getDanceability());
            addRangeQuery(b, "energy", range.getEnergy());
            addRangeQuery(b, "speechiness", range.getSpeechiness());
            addRangeQuery(b, "acousticness", range.getAcousticness());
            addRangeQuery(b, "liveness", range.getLiveness());
            addRangeQuery(b, "valence", range.getValence());
            addRangeQuery(b, "loudness", range.getLoudness());
            addRangeQuery(b, "tempo", range.getTempo());
            b.must(queryBuilder -> queryBuilder
                    .range(rq -> rq
                            .field("trackMetrics.popularity")
//                            .gte(JsonData.of(10))
                    )
            );
            return b;
        })._toQuery();

        return wrapFunctionScore(innerQuery);
    }

    private static Query wrapFunctionScore(Query innerQuery) {
        return Query.of(q -> q
                .functionScore(fs -> fs
                        .query(innerQuery)
                        .functions(List.of(
                                FunctionScore.of(fn -> fn
                                        .randomScore(rs -> rs
                                                .seed(String.valueOf(System.currentTimeMillis()))
                                                .field("_seq_no")
                                        )
                                )
                        ))
                )
        );
    }

    private static void addRangeQuery(BoolQuery.Builder b, String field, DoubleRange range) {
        if (range == null) return;
        b.must(queryBuilder -> queryBuilder
                .range(rangeQueryBuilder -> rangeQueryBuilder
                        .field(field)
                        .gte(JsonData.of(range.getMin()))
                        .lte(JsonData.of(range.getMax()))
                )
        );
    }

}
