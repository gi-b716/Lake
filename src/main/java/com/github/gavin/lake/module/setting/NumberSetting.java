package com.github.gavin.lake.module.setting;

import java.util.Locale;

public final class NumberSetting extends Setting<Double> {
    private final double minimum;
    private final double maximum;
    private final double step;
    private final String unit;

    public NumberSetting(String id, String name, String description, double value,
                         double minimum, double maximum, double step, String unit) {
        super(id, name, description, Double.class, value);
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || !Double.isFinite(step)
                || maximum <= minimum || step <= 0) throw new IllegalArgumentException("Invalid setting range");
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.unit = unit;
        set(value);
    }

    @Override protected Double normalize(Double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Setting value must be finite");
        double rounded = minimum + Math.round((Math.max(minimum, Math.min(maximum, value)) - minimum) / step) * step;
        return Math.max(minimum, Math.min(maximum, rounded));
    }

    public double fraction() { return (value() - minimum) / (maximum - minimum); }
    public void fraction(double fraction) { set(minimum + Math.max(0, Math.min(1, fraction)) * (maximum - minimum)); }
    public String displayValue() {
        return (step >= 1 ? String.format(Locale.ROOT, "%.0f", value()) : String.format(Locale.ROOT, "%.1f", value())) + unit;
    }
}
