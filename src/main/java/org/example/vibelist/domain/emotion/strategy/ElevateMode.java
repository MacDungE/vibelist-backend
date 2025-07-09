package org.example.vibelist.domain.emotion.strategy;

import org.example.vibelist.domain.emotion.DoubleRange;
import org.example.vibelist.domain.emotion.EmotionFeatureProfile;
import org.example.vibelist.domain.emotion.EmotionType;
import org.springframework.stereotype.Component;

@Component
public class ElevateMode implements EmotionModeStrategy {

    // 기분 올리기 -> Valence, Energy 올리기

    @Override
    public EmotionFeatureProfile generateProfile(EmotionType type, double userValence, double userEnergy) {
        double raisedValence = Math.min(1.0, userValence + 0.2);
        double raisedEnergy = Math.min(1.0, userEnergy + 0.05);

        return EmotionFeatureProfile.builder()
                .valence(DoubleRange.of(raisedValence, 0.1))
                .energy(DoubleRange.of(raisedEnergy, 0.05))
                .danceability(DoubleRange.fixed(0.7, 1.0))
                .tempo(DoubleRange.fixed(120, 150))
                .acousticness(DoubleRange.fixed(0.0, 0.3))
                .instrumentalness(DoubleRange.fixed(0.0, 0.3))
                .loudness(DoubleRange.fixed(-5, 0))
                .speechiness(DoubleRange.fixed(0.2, 0.5))
                .liveness(DoubleRange.fixed(0.3, 0.6))
                .build();
    }
}
