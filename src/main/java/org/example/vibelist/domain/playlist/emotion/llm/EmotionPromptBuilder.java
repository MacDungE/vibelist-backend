package org.example.vibelist.domain.playlist.emotion.llm;

public class EmotionPromptBuilder {
    public static String build(String userText) {
        return """
        아래는 사용자의 감정 묘사입니다.
        이 감정을 숫자로 표현해주세요:
        - valence (0.0 ~ 1.0): 감정의 긍정성
        - energy (0.0 ~ 1.0): 감정의 활성도

        사용자가 입력한 감정: "%s"

        출력 형식:
        {
          "valence": 0.2,
          "energy": 0.3
        }
        """.formatted(userText);
    }
}
