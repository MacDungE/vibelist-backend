package org.example.vibelist.domain.playlist.emotion.profile;

import lombok.Data;

import org.example.vibelist.domain.playlist.util.DoubleRange;

@Data
public class AudioFeatureRange {
    private DoubleRange danceability;
    private DoubleRange energy;
    private DoubleRange speechiness;
    private DoubleRange acousticness;
    private DoubleRange liveness;
    private DoubleRange valence;
    private DoubleRange loudness;
    private DoubleRange tempo;
}
