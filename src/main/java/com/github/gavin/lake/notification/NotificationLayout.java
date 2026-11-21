package com.github.gavin.lake.notification;

/** Viewport-relative geometry, independent of Minecraft's GUI scale. */
final class NotificationLayout {
    final float width;
    final float height;
    final float marginX;
    final float marginY;
    final float gap;
    final float slide;

    NotificationLayout(double screenWidth, double screenHeight) {
        width = (float) (screenWidth * 0.24);
        height = (float) (screenHeight * 0.09);
        marginX = (float) (screenWidth * 0.018);
        marginY = (float) (screenHeight * 0.02);
        gap = (float) (screenHeight * 0.012);
        slide = (float) (screenWidth * 0.035);
    }
}
