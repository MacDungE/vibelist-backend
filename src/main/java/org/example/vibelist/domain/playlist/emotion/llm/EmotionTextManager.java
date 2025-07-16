package org.example.vibelist.domain.playlist.emotion.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.playlist.emotion.type.EmotionType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmotionTextManager {

    private final EmotionLLMClient llmClient;

    public EmotionType getEmotionType(String userText) throws JsonProcessingException {
        String prompt = EmotionPromptBuilder.build(userText);
        String response = llmClient.requestEmotionAnalysis(prompt).block();

       return EmotionType.valueOf(response.trim().toUpperCase());
    }
}
