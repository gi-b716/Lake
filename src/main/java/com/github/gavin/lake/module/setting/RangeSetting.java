package com.github.gavin.lake.module.setting;

import java.util.Locale;

/** A numeric interval whose two endpoints share one slider and one saved value. */
public final class RangeSetting extends Setting<RangeSetting.Range> {
    public static final class Range {
        private final double lower;
        private final double upper;

        public Range(double lower, double upper) { this.lower = lower; this.upper = upper; }
        public double lower() { return lower; }
        public double upper() { return upper; }

        @Override public boolean equals(Object other) {
            if (!(other instanceof Range)) return false;
            Range range = (Range) other;
            return Double.compare(lower, range.lower) == 0 && Double.compare(upper, range.upper) == 0;
        }

        @Override public int hashCode() { return 31 * Double.hashCode(lower) + Double.hashCode(upper); }
    }

    private final double minimum;
    private final double maximum;
    private final double step;
    private final String unit;

    public RangeSetting(String id, String name, String description, double lower, double upper,
                        double minimum, double maximum, double step, String unit) {
        super(id, name, description, Range.class, new Range(lower, upper));
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || !Double.isFinite(step)
                || maximum <= minimum || step <= 0) throw new IllegalArgumentException("Invalid setting range");
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.unit = unit;
        set(new Range(lower, upper));
    }

    @Override protected Range normalize(Range value) {
        double lower = normalizeEndpoint(value.lower());
        double upper = normalizeEndpoint(value.upper());
        return new Range(Math.min(lower, upper), Math.max(lower, upper));
    }

    private double normalizeEndpoint(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Setting value must be finite");
        double rounded = minimum + Math.round((Math.max(minimum, Math.min(maximum, value)) - minimum) / step) * step;
        return Math.max(minimum, Math.min(maximum, rounded));
    }

    public double lower() { return value().lower(); }
    public double upper() { return value().upper(); }
    public double lowerFraction() { return (lower() - minimum) / (maximum - minimum); }
    public double upperFraction() { return (upper() - minimum) / (maximum - minimum); }

    public void fraction(boolean upperHandle, double fraction) {
        double endpoint = normalizeEndpoint(minimum + Math.max(0, Math.min(1, fraction)) * (maximum - minimum));
        set(upperHandle ? new Range(lower(), Math.max(lower(), endpoint))
                : new Range(Math.min(upper(), endpoint), upper()));
    }

    public String displayValue() {
        return String.format(Locale.ROOT, step >= 1 ? "%.0f – %.0f%s" : "%.1f – %.1f%s", lower(), upper(), unit);
    }
}
