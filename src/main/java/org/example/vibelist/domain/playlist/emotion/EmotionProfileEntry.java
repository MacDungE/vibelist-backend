package org.example.vibelist.domain.playlist.emotion;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class EmotionProfileEntry {
    private EmotionFeatureProfile range;
    private Map<EmotionModeType, EmotionType> transitions;
}
