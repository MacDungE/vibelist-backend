package org.example.vibelist.domain.recommend.builder;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;
import org.example.vibelist.domain.emotion.DoubleRange;
import org.example.vibelist.domain.emotion.EmotionFeatureProfile;

import java.util.List;

// 감정 범위(valence, energy)를 기반으로 Elasticsearch 검색 쿼리를 생성하는 클래스
// 랜덤 추천을 위해 function_score + random_score 쿼리를 사용
public class ESQueryBuilder {

    public static Query build(EmotionFeatureProfile profile) {
        Query innerQuery = BoolQuery.of(b -> {
            addRangeQuery(b, "energy", profile.getEnergy());
            addRangeQuery(b, "valence", profile.getValence());
            return b;
        })._toQuery();

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
