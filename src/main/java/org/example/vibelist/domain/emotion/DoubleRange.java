package org.example.vibelist.domain.emotion;

import lombok.AllArgsConstructor;
import lombok.Getter;
/**
 * 실수형 값의 범위(min, max)를 표현하는 클래스
 */
@Getter
@AllArgsConstructor
public class DoubleRange {
    private double min;
    private double max;

    public static DoubleRange of(double center, double delta) {
        return new DoubleRange(center - delta, center + delta);
    }

    public static DoubleRange fixed(double min, double max) {
        return new DoubleRange(min, max);
    }
}
