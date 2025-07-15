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

        // 1. ES에 존재하는 모든 id를 가져오기
        Iterable<EsDoc> existingDocs = esRepository.findAll();
        Set<Long> existingEsIds = new HashSet<>();
        for (EsDoc doc : existingDocs) {
            existingEsIds.add(doc.getTrackMetrics().getTrackId()); // es에 TrackMetrics.trackId 를 추출
        }

        // 2. RDB에서 AudioFeature + Track이 연결된 전체 트랙을 가져오기 (fetch join)
        List<Track> tracks = trackRepository.findAllWithAudioFeature();

        List<EsDoc> missingDocs = new ArrayList<>();

        // 3. ES에 없는 것만 추출
        for (Track track : tracks) {
            String afId = String.valueOf(track.getId());
            if (existingEsIds.contains(afId)) {
                continue; // 이미 인덱싱됨
            }

            AudioFeature af = track.getAudioFeature();
            if (af == null) {
                log.warn("🔍 AudioFeature 없음 - trackId: {}", track.getId());
                continue;
            }

            missingDocs.add(convertToEs(af, track));
        }

        // 4. 저장
        esRepository.saveAll(missingDocs);
        log.info("✅ 누락된 트랙 {}개 저장 완료", missingDocs.size());
        System.exit(0);
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
