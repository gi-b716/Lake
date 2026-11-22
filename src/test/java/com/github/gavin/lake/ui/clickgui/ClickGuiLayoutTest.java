package com.github.gavin.lake.ui.clickgui;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClickGuiLayoutTest {
    @Test public void centersBothSidebarStatesAndKeepsThemWithinTheViewport() {
        for (double[] size : new double[][]{{427, 240}, {640, 360}, {1024, 768}, {320, 240}}) {
          for (float expansion : new float[]{0, 0.5f, 1}) {
            ClickGuiLayout layout = new ClickGuiLayout(size[0], size[1], expansion);
            float width = layout.width * layout.scale;
            float height = ClickGuiLayout.HEIGHT * layout.scale;
            assertTrue(width <= size[0] - 14 + 0.001);
            assertTrue(height <= size[1] - 14 + 0.001);
            assertTrue(layout.scale <= 1);
            assertEquals(size[0] / 2, layout.x + width / 2, 0.001);
            assertEquals(size[1] / 2, layout.y + height / 2, 0.001);
            assertEquals(130, layout.localX(layout.x + 130 * layout.scale), 0.001);
            assertEquals(70, layout.localY(layout.y + 70 * layout.scale), 0.001);
            assertEquals(new ClickGuiLayout(size[0], size[1]).scale, layout.scale, 0);
          }
        }
    }

    @Test public void physicalSizeFollowsMinecraftGuiScaleInsteadOfViewportFractions() {
        ClickGuiLayout normal = new ClickGuiLayout(2560.0 / 2, 1440.0 / 2);
        ClickGuiLayout large = new ClickGuiLayout(2560.0 / 4, 1440.0 / 4);
        assertEquals(1, normal.scale, 0);
        assertEquals(1, large.scale, 0);
        double normalWidth = ClickGuiLayout.WIDTH * normal.scale * 2;
        double largeWidth = ClickGuiLayout.WIDTH * large.scale * 4;
        assertEquals(normalWidth * 2, largeWidth, 0.001);
    }
}
