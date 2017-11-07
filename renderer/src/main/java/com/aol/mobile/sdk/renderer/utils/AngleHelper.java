package com.aol.mobile.sdk.renderer.utils;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

public final class AngleHelper {
    public static double asPiMinusPiRange(double angle) {
        angle = toDegrees(angle);
        angle = angle % 360;
        angle = (angle + 360) % 360;

        if (angle >= 180)
            return toRadians(angle - 360);
        else
            return toRadians(angle);
    }

    public static double limitAngleToRange(double angle, double lowLimit, double highLimit) {
        angle = asPiMinusPiRange(angle);
        return min(toRadians(highLimit), max(toRadians(lowLimit), angle));
    }
}
