package org.example.vibelist.domain.batch.elasticsearch.repository;

import org.example.vibelist.domain.batch.elasticsearch.dto.EsDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EsRepository extends ElasticsearchRepository <EsDoc,String>{
}
