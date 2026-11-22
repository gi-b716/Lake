package com.github.gavin.lake.notification;

/** Compact text-sized cards with a uniform height and shared spacing. */
final class NotificationLayout {
    static final float HEIGHT = 44;
    final float scale, width, height, margin, gap;

    NotificationLayout(double screenWidth, double screenHeight, float naturalWidth) {
        scale = (float) Math.max(0.001, Math.min(1, Math.min((screenWidth - 16) / 43, (screenHeight - 16) / HEIGHT)));
        margin = 8 * scale; gap = 3 * scale;
        width = (float) Math.min(Math.max(43, naturalWidth) * scale, Math.max(0.001, screenWidth - 2 * margin));
        height = HEIGHT * scale;
    }
}
