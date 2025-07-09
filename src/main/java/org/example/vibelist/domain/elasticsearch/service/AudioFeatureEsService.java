package org.example.vibelist.domain.elasticsearch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.audiofeature.entity.AudioFeature;
import org.example.vibelist.domain.audiofeature.repository.AudioFeatureRepository;
import org.example.vibelist.domain.elasticsearch.dto.AudioFeatureEsDoc;
import org.example.vibelist.domain.elasticsearch.repository.AudioFeatureEsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioFeatureEsService {
    private final AudioFeatureEsRepository audioFeatureEsRepository;//ElasticSearch 접근
    private final AudioFeatureRepository audioFeatureRepository; //Rds 접근

    @Transactional
    /*
    나중에 성능 비교를 위한 pureInsert 메소드입니다.
     */
    public void pureInsert() {
        long start, end;

        log.info("Pure Insert 작업 시작");
        log.info("Rds에서 데이터 불러오기 시작");
        start = System.currentTimeMillis();
        List<AudioFeature> features = audioFeatureRepository.findAll(PageRequest.of(0, 1000)).getContent();
        end  = System.currentTimeMillis();
        log.info("Rds에서 데이터 불러오기 종료 소요시간 {}", (end - start));

        log.info("Es에 삽입 시작");
        start = System.currentTimeMillis();
        for(AudioFeature feature: features){
        audioFeatureEsRepository.save(convertToEs(feature));
        }
        end = System.currentTimeMillis();
        log.info("Pure Insert 작업 종료, 소요 시간 :{}", (end - start));
    }


    public AudioFeatureEsDoc convertToEs(AudioFeature audioFeature) {
        /*
        Rds에 저장되어있는 genre는 하나의 String 값입니다.
        ; 기준으로 Split 했습니다.
         */
        List<String> generList = Arrays.stream(audioFeature.getGenres()
                        .split(","))
                .map(String::trim)
                .toList();

        //elasticSearch insert을 위한 변환
        AudioFeatureEsDoc esDoc = new AudioFeatureEsDoc();

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
        esDoc.setGenres(generList);

        return esDoc;


    }
}
