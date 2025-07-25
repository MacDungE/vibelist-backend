package org.example.vibelist.domain.batch.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.batch.elasticsearch.dto.TestEsDoc;
import org.example.vibelist.domain.batch.elasticsearch.repository.TestEsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TestEsService {
    private final TestEsRepository testEsRepository;
    private final ElasticsearchClient elasticsearchClient;
    @Transactional
    public void insert(TestEsDoc testEsDoc) {
        testEsRepository.save(testEsDoc);
    }




}
