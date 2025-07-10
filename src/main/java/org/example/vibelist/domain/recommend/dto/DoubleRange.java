package org.example.vibelist.domain.recommend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DoubleRange {
    private double min;
    private double max;

    public static DoubleRange around(double center, double delta) {
        return new DoubleRange(center - delta, center + delta);
    }

    public static DoubleRange fixed(double min, double max) {
        return new DoubleRange(min, max);
    }
}
