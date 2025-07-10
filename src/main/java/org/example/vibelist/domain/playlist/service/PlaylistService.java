package org.example.vibelist.domain.playlist.service;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.playlist.repository.PlaylistRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
}
