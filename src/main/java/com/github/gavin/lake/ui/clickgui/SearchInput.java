package com.github.gavin.lake.ui.clickgui;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/** Editing state only; the search field is rendered using the reference typeface. */
final class SearchInput {
    private String text = "";
    private int cursor, anchor;
    boolean focused;

    String text() { return text; }
    int cursor() { return cursor; }
    int start() { return Math.min(cursor, anchor); }
    int end() { return Math.max(cursor, anchor); }

    void focus() { focused = true; cursor = anchor = text.length(); }
    void clear() { text = ""; cursor = anchor = 0; }

    boolean key(char character, int key) {
        if (!focused) return false;
        String previous = text;
        boolean control = Keyboard.isCreated() && GuiScreen.isCtrlKeyDown();
        boolean shift = Keyboard.isCreated() && GuiScreen.isShiftKeyDown();
        if (control && key == Keyboard.KEY_A) { anchor = 0; cursor = text.length(); }
        else if (control && (key == Keyboard.KEY_C || key == Keyboard.KEY_X)) {
            if (start() != end()) GuiScreen.setClipboardString(text.substring(start(), end()));
            if (key == Keyboard.KEY_X) replace("");
        } else if (control && key == Keyboard.KEY_V) replace(GuiScreen.getClipboardString());
        else if (key == Keyboard.KEY_LEFT || key == Keyboard.KEY_RIGHT) {
            if (!shift && start() != end()) cursor = key == Keyboard.KEY_LEFT ? start() : end();
            else if (key == Keyboard.KEY_LEFT && cursor > 0) cursor = text.offsetByCodePoints(cursor, -1);
            else if (key == Keyboard.KEY_RIGHT && cursor < text.length()) cursor = text.offsetByCodePoints(cursor, 1);
            if (!shift) anchor = cursor;
        } else if (key == Keyboard.KEY_HOME || key == Keyboard.KEY_END) {
            cursor = key == Keyboard.KEY_HOME ? 0 : text.length();
            if (!shift) anchor = cursor;
        } else if (key == Keyboard.KEY_BACK) {
            if (start() != end()) replace("");
            else if (cursor > 0) { anchor = text.offsetByCodePoints(cursor, -1); replace(""); }
        } else if (key == Keyboard.KEY_DELETE) {
            if (start() != end()) replace("");
            else if (cursor < text.length()) { anchor = text.offsetByCodePoints(cursor, 1); replace(""); }
        } else if (!control && character >= 32 && character != 127 && character != '§') replace(String.valueOf(character));
        return !previous.equals(text);
    }

    private void replace(String value) {
        value = value.replaceAll("[\\p{Cntrl}§]", "");
        int start = start(), end = end();
        int available = 96 - text.length() + end - start;
        if (value.length() > available) {
            int limit = Math.max(0, available);
            if (limit > 0 && Character.isHighSurrogate(value.charAt(limit - 1))) limit--;
            value = value.substring(0, limit);
        }
        text = text.substring(0, start) + value + text.substring(end);
        cursor = anchor = start + value.length();
    }
}
