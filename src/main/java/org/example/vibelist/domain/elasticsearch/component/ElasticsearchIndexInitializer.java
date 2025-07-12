package org.example.vibelist.domain.elasticsearch.component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.elasticsearch.dto.EsDoc;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

//@Component
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer {

    private final ElasticsearchOperations elasticsearchOperations;

    @PostConstruct
    public void init() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(EsDoc.class);
        if (!indexOps.exists()) {
            indexOps.create(); // → createIndex=true 가 붙어 있어야 mapping 자동 적용됨
            indexOps.putMapping(indexOps.createMapping());
        }
    }
}