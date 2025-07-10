package org.example.vibelist.domain.recommend.emotion;

import org.example.vibelist.domain.recommend.dto.DoubleRange;
import org.example.vibelist.domain.recommend.dto.EmotionFeatureProfile;
import org.example.vibelist.domain.recommend.dto.EmotionMode;
import org.example.vibelist.domain.recommend.dto.EmotionType;
import org.springframework.stereotype.Component;

@Component
public class EmotionMapper {

    public EmotionFeatureProfile map(EmotionType emotion, EmotionMode mode) {

        EmotionFeatureProfile profile1 = EmotionFeatureProfile.builder()
                .danceability(new DoubleRange(0.5, 0.75))
                .energy(new DoubleRange(0.5, 1.0))
                .loudness(new DoubleRange(-13.0, 0))
                .speechiness(new DoubleRange(0.0, 0.6))
                .acousticness(new DoubleRange(0.0, 0.3))
                .instrumentalness(new DoubleRange(0.0, 0.9))
                .liveness(new DoubleRange(0.05, 0.35))
                .valence(new DoubleRange(0.4, 0.7))
                .tempo(new DoubleRange(100.0, 130.0))
                .build();

// 예시 2
        EmotionFeatureProfile profile2 = EmotionFeatureProfile.builder()
                .danceability(new DoubleRange(0.7, 0.85))
                .energy(new DoubleRange(0.6, 0.9))
                .loudness(new DoubleRange(-10.0, -4.0))
                .speechiness(new DoubleRange(0.02, 0.08))
                .acousticness(new DoubleRange(0.6, 0.9))
                .instrumentalness(new DoubleRange(0.0, 0.1))
                .liveness(new DoubleRange(0.05, 0.3))
                .valence(new DoubleRange(0.7, 0.95))
                .tempo(new DoubleRange(120.0, 140.0))
                .build();

// 예시 3
        EmotionFeatureProfile profile3 = EmotionFeatureProfile.builder()
                .danceability(new DoubleRange(0.3, 0.55))
                .energy(new DoubleRange(0.2, 0.5))
                .loudness(new DoubleRange(-20.0, -13.0))
                .speechiness(new DoubleRange(0.03, 0.06))
                .acousticness(new DoubleRange(0.8, 1.0))
                .instrumentalness(new DoubleRange(0.8, 1.0))
                .liveness(new DoubleRange(0.1, 0.4))
                .valence(new DoubleRange(0.2, 0.4))
                .tempo(new DoubleRange(75.0, 110.0))
                .build();

        EmotionFeatureProfile profile4 = EmotionFeatureProfile.builder()
                .valence(DoubleRange.fixed(0.875, 1.0))
                .energy(DoubleRange.fixed(0.875, 1.0))
                .danceability(DoubleRange.fixed(0.8, 1.0))
                .tempo(DoubleRange.fixed(218.75, 250))
                .acousticness(DoubleRange.fixed(0.0, 0.15))
                .instrumentalness(DoubleRange.fixed(0.0, 0.15))
                .loudness(DoubleRange.fixed(-2.25, 6.031))
                .speechiness(DoubleRange.fixed(0.2, 0.5))
                .liveness(DoubleRange.fixed(0.5, 0.7))
                .build();

        return profile1;
    }

}
