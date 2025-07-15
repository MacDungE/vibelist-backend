package org.example.vibelist.domain.batch.elasticsearch.dto;

import jakarta.persistence.Id;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Document(indexName = "test_index")
public class TestEsDoc {
    @Id
    private String id;
    private String title;
    private String content;
    private String username;
}
