package org.example.vibelist.domain.audiofeature.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

//@Component
public class CsvImportRunner implements CommandLineRunner {

    private final AudioFeatureService importer;

    public CsvImportRunner(AudioFeatureService importer) {
        this.importer = importer;
    }

    @Override
    public void run(String... args) throws Exception {
        importer.importFromCsv("src/main/resources/audio_features.csv");
    }
}