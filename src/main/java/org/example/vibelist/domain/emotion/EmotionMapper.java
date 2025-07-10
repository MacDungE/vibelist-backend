package org.example.vibelist.domain.emotion;

/**
 * 감정 타입에 따라 valence, energy의 검색 범위를 매핑하는 클래스
 */
public class EmotionMapper {

    public static EmotionFeatureProfile map(EmotionType type) {
        switch (type) {
            case DEPRESSED:
                return profile(0.000, 0.281, 0.000, 0.415);
            case SLEEPY:
                return profile(0.000, 0.281, 0.415, 0.722);
            case TENSE:
                return profile(0.000, 0.281, 0.722, 1.000);
            case SAD:
                return profile(0.281, 0.586, 0.000, 0.415);
            case NEUTRAL:
                return profile(0.281, 0.586, 0.415, 0.722);
            case FEAR:
                return profile(0.281, 0.586, 0.722, 1.000);
            case CALM:
                return profile(0.586, 1.000, 0.000, 0.415);
            case JOY:
                return profile(0.586, 1.000, 0.415, 0.722);
            case EXCITED:
                return profile(0.586, 1.000, 0.722, 1.000);
            default:
                throw new IllegalArgumentException("Unknown label: " + type);
        }
    }

    private static EmotionFeatureProfile profile(
            double valMin, double valMax,
            double enMin, double enMax) {
        return EmotionFeatureProfile.builder()
                .valence(new DoubleRange(valMin, valMax))
                .energy(new DoubleRange(enMin, enMax))
                .build();
    }
}
