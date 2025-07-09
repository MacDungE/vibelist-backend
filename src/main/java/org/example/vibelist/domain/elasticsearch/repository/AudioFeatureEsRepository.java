package org.example.vibelist.domain.elasticsearch.repository;

import org.example.vibelist.domain.elasticsearch.dto.AudioFeatureEsDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioFeatureEsRepository extends ElasticsearchRepository <AudioFeatureEsDoc,String>{
}
