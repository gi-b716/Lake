package com.github.gavin.lake.module.modules.combat.autoclicker;

import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

public class ClickTimingTest {
    @Test public void maintainsCadenceAcrossPollingRatesIncludingClientTicks() {
        for (int fps : new int[] { 20, 30, 60, 144, 240 }) {
            ClickTiming timing = new ClickTiming(new Random(42));
            long frame = 1_000_000_000L / fps;
            long end = 120_000_000_000L;
            int clicks = 0;
            for (long now = 0; now < end; now += frame) {
                if (timing.shouldClick(now, 10, 14)) clicks++;
            }
            double rate = clicks / 120.0;
            assertTrue("Unexpected CPS at " + fps + " FPS: " + rate, rate > 10 && rate < 14);
        }
    }

    @Test public void dropsStalledClicksAndResetsOnRelease() {
        ClickTiming timing = new ClickTiming(new Random(42));
        assertTrue(timing.shouldClick(0, 20, 20));
        assertFalse(timing.shouldClick(1, 20, 20));
        assertTrue(timing.shouldClick(5_000_000_000L, 20, 20));
        assertFalse(timing.shouldClick(5_000_000_001L, 20, 20));
        timing.reset();
        assertTrue(timing.shouldClick(5_000_000_002L, 20, 20));
    }

    @Test public void rangeChangesTakeEffectImmediately() {
        ClickTiming timing = new ClickTiming(new Random(42));
        assertTrue(timing.shouldClick(0, 1, 1));
        assertTrue(timing.shouldClick(10_000_000L, 20, 20));
        assertTrue(timing.shouldClick(60_000_000L, 20, 20));
    }

    @Test public void variesDelaysAndHandlesBothRangeLimits() {
        for (double[] range : new double[][] { { 1, 1 }, { 20, 20 }, { 1, 20 }, { 10, 14 } }) {
            ClickTiming timing = new ClickTiming(new Random(42));
            Set<Long> intervals = new HashSet<>();
            long last = -1;
            for (long now = 0; now < 60_000_000_000L; now += 1_000_000L) {
                if (!timing.shouldClick(now, range[0], range[1])) continue;
                if (last >= 0) {
                    long interval = now - last;
                    assertTrue(interval > 0 && interval <= 1_400_000_000L);
                    intervals.add(interval);
                }
                last = now;
            }
            assertTrue("Timing did not vary", intervals.size() > 2);
        }
    }
}
