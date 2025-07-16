package org.example.vibelist.domain.playlist.emotion.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.playlist.emotion.profile.AudioFeatureRange;
import org.example.vibelist.domain.playlist.emotion.type.EmotionModeType;
import org.example.vibelist.domain.playlist.emotion.type.EmotionType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmotionTextManager {

    private final EmotionLLMClient llmClient;

    public AudioFeatureRange getAudioFeatureRange(String userText, EmotionModeType mode) throws JsonProcessingException {
        String prompt = EmotionPromptBuilder.build(userText, mode);
        AudioFeatureRange response = llmClient.requestEmotionAnalysis(prompt).block();

        return response;
    }
}
