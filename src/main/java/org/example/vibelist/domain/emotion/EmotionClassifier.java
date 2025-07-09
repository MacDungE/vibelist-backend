package org.example.vibelist.domain.emotion;

/**
 * valence와 energy 값을 기반으로 감정 타입을 분류하는 클래스
 */
public class EmotionClassifier {

    public static EmotionType classify(double valence, double energy) {
        if (valence < 0.281) {
            if (energy < 0.415) {
                return EmotionType.DEPRESSED;
            } else if (energy < 0.722) {
                return EmotionType.SLEEPY;
            } else {
                return EmotionType.TENSE;
            }
        } else if (valence < 0.586) {
            if (energy < 0.415) {
                return EmotionType.SAD;
            } else if (energy < 0.722) {
                return EmotionType.NEUTRAL;
            } else {
                return EmotionType.FEAR;
            }
        } else {
            if (energy < 0.415) {
                return EmotionType.CALM;
            } else if (energy < 0.722) {
                return EmotionType.JOY;
            } else {
                return EmotionType.EXCITED;
            }
        }
    }
}
