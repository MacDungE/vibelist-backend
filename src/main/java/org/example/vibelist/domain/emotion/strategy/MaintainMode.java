package org.example.vibelist.domain.emotion.strategy;

import org.example.vibelist.domain.emotion.DoubleRange;
import org.example.vibelist.domain.emotion.EmotionFeatureProfile;
import org.example.vibelist.domain.emotion.EmotionType;
import org.springframework.stereotype.Component;

@Component
public class MaintainMode implements EmotionModeStrategy {

    // 현재 감정 유지

    @Override
    public EmotionFeatureProfile generateProfile(EmotionType type, double userValence, double userEnergy) {
        return EmotionFeatureProfile.builder()
                .valence(DoubleRange.of(userValence, 0.05))
                .energy(DoubleRange.of(userEnergy, 0.05))
                .danceability(type.getDanceability())
                .tempo(type.getTempo())
                .acousticness(type.getAcousticness())
                .instrumentalness(type.getInstrumentalness())
                .loudness(type.getLoudness())
                .speechiness(type.getSpeechiness())
                .liveness(type.getLiveness())
                .build();
    }
}
