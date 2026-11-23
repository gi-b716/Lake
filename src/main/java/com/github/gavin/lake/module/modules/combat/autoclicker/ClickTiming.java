package com.github.gavin.lake.module.modules.combat.autoclicker;

import java.util.Random;

/** Stateful drift and short slow phases, inspired by Extra+ and OpenB4's timing. */
public final class ClickTiming {
    private final Random random;
    private double minimum, maximum, currentCps, targetCps, slowFactor = 1;
    private long nextDrift, nextTarget, nextPhase, nextPause;
    private long nextClick;
    private boolean initialized;

    public ClickTiming(Random random) { this.random = random; }

    public void reset() { initialized = false; }

    public boolean shouldClick(long now, double lower, double upper) {
        if (!Double.isFinite(lower) || !Double.isFinite(upper) || lower <= 0 || upper < lower) {
            throw new IllegalArgumentException("Invalid CPS interval");
        }
        if (!initialized || minimum != lower || maximum != upper) nextClick = now;
        if (now < nextClick) return false;
        long delay = nextDelayNanos(now, lower, upper);
        nextClick += delay;
        // Drop overdue clicks after a stall; never replay a backlog of attacks.
        if (nextClick <= now) nextClick = now + delay;
        return true;
    }

    long nextDelayNanos(long now, double lower, double upper) {
        if (!initialized || minimum != lower || maximum != upper) {
            initialized = true;
            minimum = lower;
            maximum = upper;
            currentCps = targetCps = sampleCps();
            slowFactor = 1;
            nextDrift = now;
            nextTarget = now + duration(700, 2200);
            nextPhase = now + duration(700, 2200);
            nextPause = now + duration(700, 2200);
        }
        if (now >= nextTarget) {
            targetCps = sampleCps();
            nextTarget = now + duration(700, 2200);
        }
        if (now >= nextDrift) {
            currentCps = clamp(currentCps + (targetCps - currentCps) * 0.28
                    + random.nextGaussian() * (maximum - minimum) * 0.06);
            nextDrift = now + duration(80, 200);
        }
        if (now >= nextPhase) {
            slowFactor = slowFactor > 1 || random.nextDouble() >= 0.2 ? 1 : 1.1 + random.nextDouble() * 0.15;
            nextPhase = now + duration(700, 2200);
        }
        double cps = clamp(currentCps + random.nextGaussian() * (maximum - minimum) * 0.1);
        double delay = 1_000_000_000.0 / cps * slowFactor;
        if (now >= nextPause) {
            if (random.nextDouble() < 0.25) delay += duration(50, 90);
            nextPause = now + duration(700, 2200);
        }
        return Math.max(1L, Math.round(delay));
    }

    private double sampleCps() { return minimum + random.nextDouble() * (maximum - minimum); }
    private double clamp(double cps) { return Math.max(minimum, Math.min(maximum, cps)); }
    private long duration(int minimumMillis, int maximumMillis) {
        return (minimumMillis + random.nextInt(maximumMillis - minimumMillis + 1)) * 1_000_000L;
    }
}
