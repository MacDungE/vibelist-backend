package org.example.vibelist.domain.emotion.strategy;

import org.example.vibelist.domain.emotion.EmotionFeatureProfile;
import org.example.vibelist.domain.emotion.EmotionType;

public interface EmotionModeStrategy {

    EmotionFeatureProfile generateProfile(EmotionType type, double userValence, double userEnergy);

}
