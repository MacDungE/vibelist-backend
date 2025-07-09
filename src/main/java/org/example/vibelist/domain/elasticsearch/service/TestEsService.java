package org.example.vibelist.domain.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.audiofeature.entity.AudioFeature;
import org.example.vibelist.domain.elasticsearch.dto.AudioFeatureEsDoc;
import org.example.vibelist.domain.elasticsearch.dto.TestEsDoc;
import org.example.vibelist.domain.elasticsearch.repository.TestEsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

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
