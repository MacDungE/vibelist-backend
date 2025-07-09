package org.example.vibelist.domain.emotion;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EmotionType {
    EXCITED(
            DoubleRange.fixed(0.85, 1.0),       // valence
            DoubleRange.fixed(0.9, 1.0),        // energy
            DoubleRange.fixed(0.85, 1.0),       // danceability
            DoubleRange.fixed(130, 160),        // tempo
            DoubleRange.fixed(0.0, 0.2),        // acousticness
            DoubleRange.fixed(0.0, 0.2),        // instrumentalness
            DoubleRange.fixed(-5, 0),           // loudness
            DoubleRange.fixed(0.1, 0.4),        // speechiness
            DoubleRange.fixed(0.4, 0.6)         // liveness
    ),

    JOY(
            DoubleRange.fixed(0.8, 1.0),
            DoubleRange.fixed(0.7, 0.9),
            DoubleRange.fixed(0.75, 0.95),
            DoubleRange.fixed(120, 140),
            DoubleRange.fixed(0.1, 0.3),
            DoubleRange.fixed(0.0, 0.2),
            DoubleRange.fixed(-8, -2),
            DoubleRange.fixed(0.2, 0.4),
            DoubleRange.fixed(0.3, 0.5)
    ),

    CALM(
            DoubleRange.fixed(0.6, 0.8),
            DoubleRange.fixed(0.2, 0.4),
            DoubleRange.fixed(0.5, 0.7),
            DoubleRange.fixed(80, 100),
            DoubleRange.fixed(0.7, 1.0),
            DoubleRange.fixed(0.3, 0.5),
            DoubleRange.fixed(-20, -10),
            DoubleRange.fixed(0.0, 0.2),
            DoubleRange.fixed(0.2, 0.4)
    ),

    SLEEPY(
            DoubleRange.fixed(0.3, 0.5),
            DoubleRange.fixed(0.1, 0.3),
            DoubleRange.fixed(0.3, 0.5),
            DoubleRange.fixed(60, 80),
            DoubleRange.fixed(0.85, 1.0),
            DoubleRange.fixed(0.4, 0.7),
            DoubleRange.fixed(-25, -15),
            DoubleRange.fixed(0.0, 0.1),
            DoubleRange.fixed(0.1, 0.3)
    ),

    TENSE(
            DoubleRange.fixed(0.2, 0.4),
            DoubleRange.fixed(0.8, 1.0),
            DoubleRange.fixed(0.2, 0.4),
            DoubleRange.fixed(120, 140),
            DoubleRange.fixed(0.1, 0.3),
            DoubleRange.fixed(0.2, 0.4),
            DoubleRange.fixed(-6, -2),
            DoubleRange.fixed(0.2, 0.4),
            DoubleRange.fixed(0.5, 0.7)
    ),

    FEAR(
            DoubleRange.fixed(0.1, 0.3),
            DoubleRange.fixed(0.7, 0.9),
            DoubleRange.fixed(0.2, 0.4),
            DoubleRange.fixed(100, 130),
            DoubleRange.fixed(0.2, 0.4),
            DoubleRange.fixed(0.4, 0.6),
            DoubleRange.fixed(-10, -5),
            DoubleRange.fixed(0.1, 0.3),
            DoubleRange.fixed(0.6, 0.8)
    ),

    SAD(
            DoubleRange.fixed(0.1, 0.3),
            DoubleRange.fixed(0.2, 0.4),
            DoubleRange.fixed(0.3, 0.5),
            DoubleRange.fixed(70, 90),
            DoubleRange.fixed(0.8, 1.0),
            DoubleRange.fixed(0.5, 0.7),
            DoubleRange.fixed(-20, -10),
            DoubleRange.fixed(0.1, 0.3),
            DoubleRange.fixed(0.2, 0.4)
    ),

    DEPRESSED(
            DoubleRange.fixed(0.0, 0.2),
            DoubleRange.fixed(0.1, 0.3),
            DoubleRange.fixed(0.2, 0.4),
            DoubleRange.fixed(50, 70),
            DoubleRange.fixed(0.9, 1.0),
            DoubleRange.fixed(0.6, 0.8),
            DoubleRange.fixed(-30, -15),
            DoubleRange.fixed(0.0, 0.1),
            DoubleRange.fixed(0.1, 0.3)
    );

    private final DoubleRange valence;
    private final DoubleRange energy;
    private final DoubleRange danceability;
    private final DoubleRange tempo;
    private final DoubleRange acousticness;
    private final DoubleRange instrumentalness;
    private final DoubleRange loudness;
    private final DoubleRange speechiness;
    private final DoubleRange liveness;

    public EmotionFeatureProfile toProfile() {
        return EmotionFeatureProfile.builder()
                .valence(valence)
                .energy(energy)
                .danceability(danceability)
                .tempo(tempo)
                .acousticness(acousticness)
                .instrumentalness(instrumentalness)
                .loudness(loudness)
                .speechiness(speechiness)
                .liveness(liveness)
                .build();
    }
}
