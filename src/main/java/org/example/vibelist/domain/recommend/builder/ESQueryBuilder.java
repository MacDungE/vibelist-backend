package org.example.vibelist.domain.recommend.builder;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery.*;
import co.elastic.clients.elasticsearch._types.query_dsl.RandomScoreFunction.*;
import co.elastic.clients.json.JsonData;
import org.example.vibelist.domain.emotion.DoubleRange;
import org.example.vibelist.domain.emotion.EmotionFeatureProfile;

import java.util.List;


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
                                                .field("_seq_no") // 또는 "_id"
                                        )
                                )
                        ))
                )
        );
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
