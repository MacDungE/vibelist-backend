package org.example.vibelist.domain.explore.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PostESRepository extends ElasticsearchRepository<PostDocument, Long> {

}
