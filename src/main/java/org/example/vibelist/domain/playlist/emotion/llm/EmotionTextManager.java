package org.example.vibelist.domain.playlist.emotion.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.playlist.emotion.profile.EmotionAnalysis;
import org.example.vibelist.domain.playlist.emotion.type.EmotionModeType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmotionTextManager {

    private final EmotionLLMClient llmClient;

    public EmotionAnalysis getEmotionAnalysis(String userText, EmotionModeType mode) throws JsonProcessingException {
        String prompt = EmotionPromptBuilder.build(userText, mode);
        EmotionAnalysis response = llmClient.requestEmotionAnalysis(prompt).block();

        return response;
    }
}
