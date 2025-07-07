package org.example.vibelist.domain.audiofeature.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class BatchRepository {

    JdbcTemplate template = new JdbcTemplate();
}
