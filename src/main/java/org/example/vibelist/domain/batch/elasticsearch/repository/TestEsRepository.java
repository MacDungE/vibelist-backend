package org.example.vibelist.domain.batch.elasticsearch.repository;

import org.example.vibelist.domain.batch.elasticsearch.dto.TestEsDoc;
import org.springframework.stereotype.Repository;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
@Repository
public interface TestEsRepository extends ElasticsearchRepository<TestEsDoc,String>{

}
