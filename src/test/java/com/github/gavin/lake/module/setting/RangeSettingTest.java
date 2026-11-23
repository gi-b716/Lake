package com.github.gavin.lake.module.setting;

import org.junit.Test;

import static org.junit.Assert.*;

public class RangeSettingTest {
    private RangeSetting range(double lower, double upper) {
        return new RangeSetting("cps", "CPS", "", lower, upper, 1, 20, 1, " CPS");
    }

    @Test public void normalizesSavedEndpointsTogether() {
        RangeSetting range = range(10, 14);
        range.set(new RangeSetting.Range(24, -5));
        assertEquals(1, range.lower(), 0);
        assertEquals(20, range.upper(), 0);
        range.set(new RangeSetting.Range(14.2, 9.8));
        assertEquals(10, range.lower(), 0);
        assertEquals(14, range.upper(), 0);
    }

    @Test public void draggingCannotCrossTheOtherHandle() {
        RangeSetting range = range(10, 14);
        range.fraction(false, 1);
        assertEquals(14, range.lower(), 0);
        range.fraction(true, 0);
        assertEquals(14, range.upper(), 0);
        // Either endpoint can move out of a collapsed interval.
        range.fraction(false, -1);
        range.fraction(true, 2);
        assertEquals(1, range.lower(), 0);
        assertEquals(20, range.upper(), 0);
    }

    @Test public void onlyChangedIntervalsNotify() {
        RangeSetting range = range(10, 14);
        int[] changes = { 0 };
        range.onChange(() -> changes[0]++);
        range.set(new RangeSetting.Range(14, 10));
        assertEquals(0, changes[0]);
        range.fraction(true, 1);
        assertEquals(1, changes[0]);
    }

    @Test public void invalidValuesLeaveTheIntervalIntact() {
        RangeSetting range = range(10, 14);
        for (double invalid : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            try {
                range.set(new RangeSetting.Range(invalid, 14));
                fail("Non-finite endpoint accepted");
            } catch (IllegalArgumentException expected) {
                assertEquals(10, range.lower(), 0);
                assertEquals(14, range.upper(), 0);
            }
        }
    }
}
