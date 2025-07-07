package org.example.vibelist.domain.youtube.service;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.youtube.repository.YoutubeRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class YoutubeService {

    private final YoutubeRepository youtubeRepository;
}
