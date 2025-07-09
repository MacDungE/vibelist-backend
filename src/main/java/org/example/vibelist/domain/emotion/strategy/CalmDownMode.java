package org.example.vibelist.domain.emotion.strategy;

import org.example.vibelist.domain.emotion.DoubleRange;
import org.example.vibelist.domain.emotion.EmotionFeatureProfile;
import org.example.vibelist.domain.emotion.EmotionType;
import org.springframework.stereotype.Component;

@Component
public class CalmDownMode implements EmotionModeStrategy {

    // 차분해지기 -> energy 내리고, acousticness 올리기

    @Override
    public EmotionFeatureProfile generateProfile(EmotionType type, double userValence, double userEnergy) {
        double reducedEnergy = Math.max(0.0, userEnergy * 0.5);

        return EmotionFeatureProfile.builder()
                .valence(DoubleRange.of(userValence, 0.05))  // 기분은 그대로
                .energy(DoubleRange.of(reducedEnergy, 0.05))
                .danceability(DoubleRange.fixed(0.3, 0.6))
                .tempo(DoubleRange.fixed(60, 90))
                .acousticness(DoubleRange.fixed(0.7, 1.0))
                .instrumentalness(DoubleRange.fixed(0.4, 0.8))
                .loudness(DoubleRange.fixed(-20, -10))
                .speechiness(DoubleRange.fixed(0.0, 0.2))
                .liveness(DoubleRange.fixed(0.1, 0.3))
                .build();
    }
}
