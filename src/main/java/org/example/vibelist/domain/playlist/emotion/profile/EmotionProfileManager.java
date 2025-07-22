package org.example.vibelist.domain.playlist.emotion.profile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.vibelist.domain.playlist.emotion.type.EmotionModeType;
import org.example.vibelist.domain.playlist.emotion.type.EmotionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EmotionProfileManager {

    private final Map<EmotionType, EmotionProfileEntry> profileMap;

    public EmotionProfileManager(@Value("${emotion.profile.path}") String jsonPath) throws IOException {
        File file = new File(jsonPath);
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<Map<String, EmotionProfileEntry>> typeRef = new TypeReference<>() {};
        Map<String, EmotionProfileEntry> raw = mapper.readValue(file, typeRef);

        this.profileMap = raw.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> EmotionType.valueOf(e.getKey()),
                        Map.Entry::getValue
                ));
    }

    public EmotionType classify(double valence, double energy) {
        return profileMap.entrySet().stream()
                .filter(entry -> isInRange(entry.getValue().getRange(), valence, energy))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(EmotionType.NEUTRAL);
    }

    public EmotionType getTransition(EmotionType current, EmotionModeType mode) {
        return profileMap.get(current).getTransitions().getOrDefault(mode, current);
    }

    public EmotionFeatureProfile getProfile(EmotionType emotion) {
        EmotionFeatureProfile feature = profileMap.get(emotion).getRange();
        return EmotionFeatureProfile.builder()
                .valence(feature.getValence())
                .energy(feature.getEnergy())
                .build();
    }

    private boolean isInRange(EmotionFeatureProfile feature, double val, double eng) {
        return val >= feature.getValence().getMin() && val <= feature.getValence().getMax()
                && eng >= feature.getEnergy().getMin() && eng <= feature.getEnergy().getMax();
    }
}
