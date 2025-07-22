package org.example.vibelist.domain.playlist.emotion.profile;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.vibelist.domain.playlist.emotion.type.EmotionModeType;
import org.example.vibelist.domain.playlist.emotion.type.EmotionType;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class EmotionProfileEntry {
    private EmotionFeatureProfile range;
    private Map<EmotionModeType, EmotionType> transitions;
}
