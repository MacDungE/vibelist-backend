package org.example.vibelist.domain.playlist.emotion.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmotionTextManager {
    private final EmotionLLMClient llmClient;
    private final ObjectMapper objectMapper;

    public EmotionCoordinate getEmotionCoordinates(String userText) throws JsonProcessingException {
        String prompt = EmotionPromptBuilder.build(userText);
        String response = llmClient.requestEmotionAnalysis(prompt);

        JsonNode json = objectMapper.readTree(response);
        return new EmotionCoordinate(
                json.get("valence").asDouble(),
                json.get("energy").asDouble()
        );
    }
}
