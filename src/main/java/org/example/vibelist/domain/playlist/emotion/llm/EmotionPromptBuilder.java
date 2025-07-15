package org.example.vibelist.domain.playlist.emotion.llm;

public class EmotionPromptBuilder {
    public static String build(String userText) {
        return """
        아래 문장은 사용자의 감정 상태를 설명한 것입니다.
        이 감정을 다음 중 하나로 분류하세요 (정확히 하나만 선택해서 한 단어로 출력하세요):

        DEPRESSED, SLEEPY, TENSE, SAD, NEUTRAL, FEAR, CALM, JOY, EXCITED

        감정 설명: "%s"

        답변 형식: DEPRESSED
        """.formatted(userText);
    }
}
