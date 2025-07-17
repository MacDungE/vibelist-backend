package org.example.vibelist.domain.batch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.audiofeature.entity.AudioFeature;
import org.example.vibelist.domain.audiofeature.repository.AudioFeatureRepository;
import org.example.vibelist.domain.elasticsearch.dto.EsDoc;
import org.example.vibelist.domain.elasticsearch.dto.TrackMetrics;
import org.example.vibelist.domain.elasticsearch.repository.EsRepository;
import org.example.vibelist.domain.track.entity.Track;
import org.example.vibelist.domain.track.repository.TrackRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EsBatchService implements BatchService{


    private final AudioFeatureRepository audioFeatureRepository;
    private final TrackRepository trackRepository;
    private final EsRepository esRepository;


    @Override
    public void executeBatch() {
        // 1. ES에 저장된 가장 큰 trackId 가져오기
        PageRequest firstPage = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("trackMetrics.trackId")));
        EsDoc esDoc = esRepository.findAll(firstPage)
                .stream()
                .findFirst()
                .orElse(null);

        long lastTrackId = esDoc != null && esDoc.getTrackMetrics() != null
                ? esDoc.getTrackMetrics().getTrackId()
                : 0L;

        log.info("📌 Elasticsearch 마지막 trackId: {}", lastTrackId);

        final int CHUNK_SIZE = 1000;
        boolean hasMore = true;

        while (hasMore) {
            PageRequest pageRequest = PageRequest.of(0, CHUNK_SIZE, Sort.by("id").ascending());
            List<Track> tracks = trackRepository.findByTrackIdAfter(lastTrackId, pageRequest);

            if (tracks.isEmpty()) {
                hasMore = false;
                break;
            }

            List<EsDoc> docsToSave = new ArrayList<>();

            for (Track track : tracks) {
                AudioFeature af = track.getAudioFeature();
                if (af == null) {
                    log.warn("🔍 AudioFeature 없음 - trackId: {}", track.getId());
                    continue;
                }

                docsToSave.add(convertToEs(af, track));
            }

            esRepository.saveAll(docsToSave);
            log.info("📦 인덱싱 완료 - {}건 저장", docsToSave.size());

            // 다음 커서 포인트로 이동
            lastTrackId = tracks.get(tracks.size() - 1).getId();
        }

        log.info("🎉 Elasticsearch 인덱싱 전체 완료");
    }


    private EsDoc convertToEs(AudioFeature audioFeature, Track track) {
        /*
        Rds에 저장되어있는 genre는 하나의 String 값입니다.
        ; 기준으로 Split 했습니다.
         */
        List<String> generList = Arrays.stream(audioFeature.getGenres()
                        .split(";"))
                .map(String::trim)
                .toList();

        //elasticSearch insert을 위한 변환
        EsDoc esDoc = new EsDoc();

        esDoc.setId(audioFeature.getId().toString());
        esDoc.setDanceability(audioFeature.getDanceability());
        esDoc.setEnergy(audioFeature.getEnergy());
        esDoc.setKey(audioFeature.getKey());
        esDoc.setLoudness(audioFeature.getLoudness());
        esDoc.setMode(audioFeature.getMode());
        esDoc.setSpeechiness(audioFeature.getSpeechiness());
        esDoc.setAcousticness(audioFeature.getAcousticness());
        esDoc.setInstrumentalness(audioFeature.getInstrumentalness());
        esDoc.setLiveness(audioFeature.getLiveness());
        esDoc.setValence(audioFeature.getValence());
        esDoc.setTempo(audioFeature.getTempo());
        esDoc.setDurationMs(audioFeature.getDurationMs());
        esDoc.setTimeSignature(audioFeature.getTimeSignature());
        esDoc.setGenres(generList);
        esDoc.setSpotifyId(audioFeature.getSpotifyId());

        TrackMetrics trackMetrics = new TrackMetrics();
        trackMetrics.setTrackId(track.getId());
        trackMetrics.setAlbum(track.getAlbum());
        trackMetrics.setArtist(track.getArtist());
        trackMetrics.setTitle(track.getTitle());
        trackMetrics.setPopularity(track.getPopularity());
        trackMetrics.setExplicit(track.isExplicit());
        trackMetrics.setImageUrl(track.getImageUrl());
        esDoc.setTrackMetrics(trackMetrics);


        return esDoc;
    }

}
