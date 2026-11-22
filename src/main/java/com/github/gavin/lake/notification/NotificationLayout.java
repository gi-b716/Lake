package com.github.gavin.lake.notification;

/** Text determines the width; all toasts retain the same reference height and scale. */
final class NotificationLayout {
    static final float HEIGHT = 66;
    final float scale, width, height, margin, gap;

    NotificationLayout(double screenWidth, double screenHeight, float naturalWidth) {
        scale = (float) Math.max(0.001, Math.min(1, Math.min((screenWidth - 16) / 56, (screenHeight - 16) / HEIGHT)));
        margin = 8 * scale; gap = 5 * scale;
        width = (float) Math.min(Math.max(56, naturalWidth) * scale, Math.max(0.001, screenWidth - 2 * margin));
        height = HEIGHT * scale;
    }
}
