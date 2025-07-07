package org.example.vibelist.domain.track.service;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.batch.service.BatchService;
import org.example.vibelist.domain.track.repository.TrackRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackBatchService implements BatchService {

    private final TrackRepository trackRepository;

    @Override
    public void executeBatch() {

    }
}
