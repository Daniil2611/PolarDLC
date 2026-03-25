package com.example.simpleaimbot.utils.animation;

import java.awt.*;

public class ColorUtils {
    private static final float DARKEN_FACTOR = 0.95f;

    // === Оригинальные методы (пульсация, градиенты) ===
    public static int getFlowingRedBlack(long offset, int alpha) {
        long time = System.currentTimeMillis() / 50;
        float sin = (float) (Math.sin((time + offset) * 0.1) * 0.5 + 0.5);
        int red = (int) (200 + 55 * sin);
        return (alpha << 24) | (red << 16);
    }

    public static int getFlowingRedBlack() {
        return getFlowingRedBlack(0, 255);
    }

    public static int getFlowingRedBlack(int alpha) {
        return getFlowingRedBlack(0, alpha);
    }

    public static int getFlowingRedBlackWithOffset(long offset) {
        return getFlowingRedBlack(offset, 255);
    }

    public static int getFlowingBackground() {
        long time = System.currentTimeMillis() / 30;
        float sin = (float) (Math.sin(time * 0.1) * 0.5 + 0.5);
        int alpha = 0x90;
        int red = (int) (20 + 30 * sin);
        return (alpha << 24) | (red << 16);
    }

    public static int getPulsingColor(int baseColor, long offset, int alpha) {
        long time = System.currentTimeMillis() / 50;
        float sin = (float) (Math.sin((time + offset) * 0.1) * 0.5 + 0.5);
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        float factor = DARKEN_FACTOR * (0.6f + 0.4f * sin);
        int pr = (int) (r * factor);
        int pg = (int) (g * factor);
        int pb = (int) (b * factor);
        return (alpha << 24) | (pr << 16) | (pg << 8) | pb;
    }

    public static int getPulsingColor(int baseColor) {
        return getPulsingColor(baseColor, 0, 255);
    }

    public static int getPulsingColorWithOffset(int baseColor, long offset) {
        return getPulsingColor(baseColor, offset, 255);
    }

    public static int getMovingGlowBackground(int screenY, long timeOffset, int baseColor) {
        long time = System.currentTimeMillis() / 30;
        int waveHeight = 300;
        int centerY = (int) (Math.sin((time + timeOffset) * 0.05) * waveHeight + 200);
        int distance = Math.abs(screenY - centerY);
        int maxDistance = 200;
        int alpha;
        if (distance < maxDistance) {
            float factor = 1.0f - (float) distance / maxDistance;
            alpha = (int) (180 * factor);
        } else {
            alpha = 0;
        }
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        r = (int) (r * DARKEN_FACTOR);
        g = (int) (g * DARKEN_FACTOR);
        b = (int) (b * DARKEN_FACTOR);
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    // === Новый метод для инъекции альфа-канала (используется в анимациях) ===
    public static int injectAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    // === Вспомогательные методы для работы с Color ===
    public static Color injectAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color interpolateColor(Color color1, Color color2, float progress) {
        int r = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * progress);
        int g = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * progress);
        int b = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * progress);
        int a = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * progress);
        return new Color(r, g, b, a);
    }
}