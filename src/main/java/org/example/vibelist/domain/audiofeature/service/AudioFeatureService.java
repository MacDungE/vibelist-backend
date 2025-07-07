package org.example.vibelist.domain.audiofeature.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.example.vibelist.domain.audiofeature.entity.AudioFeature;
import org.example.vibelist.domain.audiofeature.repository.AudioFeatureRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AudioFeatureService {

    private final AudioFeatureRepository audioFeatureRepository;

    public void importFromCsv(String filePath) throws IOException {
        Reader reader = Files.newBufferedReader(Paths.get(filePath));
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim());

        List<AudioFeature> features = new ArrayList<>();
        int i = 0;
        for (CSVRecord record : parser) {
            AudioFeature af = new AudioFeature();
            af.setDanceability(Double.parseDouble(record.get("danceability")));
            af.setEnergy(Double.parseDouble(record.get("energy")));
            af.setKey(Integer.parseInt(record.get("key")));
            af.setLoudness(Double.parseDouble(record.get("loudness")));
            af.setMode(Integer.parseInt(record.get("mode")));
            af.setSpeechiness(Double.parseDouble(record.get("speechiness")));
            af.setAcousticness(Double.parseDouble(record.get("acousticness")));
            af.setInstrumentalness(Double.parseDouble(record.get("instrumentalness")));
            af.setLiveness(Double.parseDouble(record.get("liveness")));
            af.setValence(Double.parseDouble(record.get("valence")));
            af.setTempo(Double.parseDouble(record.get("tempo")));
            af.setDurationMs(Integer.parseInt(record.get("duration_ms")));
            af.setTimeSignature(Integer.parseInt(record.get("time_signature")));
            af.setSpotifyId(record.get("id"));
            af.setGenres(record.get("genres"));

            features.add(af);
            i++;
            if(i==100) break;
        }

        audioFeatureRepository.saveAll(features);
    }
}
