package org.example.vibelist.domain.emotion.strategy;

import org.example.vibelist.domain.emotion.DoubleRange;
import org.example.vibelist.domain.emotion.EmotionFeatureProfile;
import org.example.vibelist.domain.emotion.EmotionType;
import org.springframework.stereotype.Component;

@Component
public class ReverseMode implements EmotionModeStrategy {

    // 기분 전환하기 -> 반대 기분

    @Override
    public EmotionFeatureProfile generateProfile(EmotionType type, double userValence, double userEnergy) {
        double reversedValence = 1.0 - userValence;
        double reversedEnergy = 1.0 - userValence;

        return EmotionFeatureProfile.builder()
                .valence(DoubleRange.of(reversedValence, 0.1))
                .energy(DoubleRange.of(reversedEnergy, 0.1))
                .danceability(DoubleRange.fixed(0.3, 0.7))
                .tempo(DoubleRange.fixed(80, 130))
                .acousticness(DoubleRange.fixed(0.3, 0.7))
                .instrumentalness(DoubleRange.fixed(0.3, 0.7))
                .loudness(DoubleRange.fixed(-12, -3))
                .speechiness(DoubleRange.fixed(0.1, 0.4))
                .liveness(DoubleRange.fixed(0.3, 0.6))
                .build();
    }
}
