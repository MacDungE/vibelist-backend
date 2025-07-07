package org.example.vibelist.domain.youtube.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.batch.BatchService;
import org.example.vibelist.domain.track.entity.Track;
import org.example.vibelist.domain.track.repository.TrackRepository;
import org.example.vibelist.domain.youtube.client.YoutubeApiClient;
import org.example.vibelist.domain.youtube.dto.YoutubeVideoMetaDto;
import org.example.vibelist.domain.youtube.entity.Youtube;
import org.example.vibelist.domain.youtube.repository.YoutubeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeBatchService implements BatchService {

    private final TrackRepository trackRepository;
    private final YoutubeApiClient youtubeApiClient;
    private final YoutubeRepository youtubeRepository;

    @Override
    public void executeBatch() {
        List<Track> tracks = trackRepository.findByYoutubeIsNull();

        for (Track track : tracks) {
            String query = track.getTitle() + " " + track.getArtist();

            try {
                YoutubeVideoMetaDto videoMeta = youtubeApiClient.getYoutubeVideo(query);

                Youtube youtube = Youtube.builder()
                        .url(videoMeta.getUrl())
                        .durationSeconds(videoMeta.getDuration())
                        .build();

                youtube.setTrack(track);
                youtubeRepository.save(youtube);

                log.info("✅ 유튜브 정보 저장 완료: {} - {}", videoMeta.getUrl());

            } catch (Exception e) {
                log.warn("❌ 유튜브 처리 실패: {} - {}", query, e.getMessage());
            }

        }

    }
}
