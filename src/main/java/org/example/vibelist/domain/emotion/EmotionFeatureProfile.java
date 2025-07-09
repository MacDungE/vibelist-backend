package org.example.vibelist.domain.emotion;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmotionFeatureProfile {
    private DoubleRange danceability;
    private DoubleRange energy;
    private DoubleRange loudness;
    private DoubleRange speechiness;
    private DoubleRange acousticness;
    private DoubleRange instrumentalness;
    private DoubleRange liveness;
    private DoubleRange valence;
    private DoubleRange tempo;
}
