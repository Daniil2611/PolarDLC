package com.example.simpleaimbot.gui;

import com.example.simpleaimbot.utils.render.Render2DEngine;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.Color;

public final class GuiTheme {
    private static final Identifier UNIFORM_FONT = Identifier.of("minecraft:uniform");
    private static final Color BACKDROP_TOP = new Color(5, 7, 11, 255);
    private static final Color BACKDROP_BOTTOM = new Color(11, 14, 21, 255);
    private static final Color PANEL = new Color(11, 13, 18, 222);
    private static final Color PANEL_ALT = new Color(17, 20, 27, 232);
    private static final Color PANEL_EDGE = new Color(255, 255, 255, 24);
    private static final Color TEXT = new Color(245, 248, 255, 255);
    private static final Color MUTED_TEXT = new Color(174, 182, 198, 255);
    private static final Color SUCCESS = new Color(160, 182, 255, 255);
    private static final Color DANGER = new Color(234, 110, 110, 255);

    private GuiTheme() {
    }

    public static Identifier uniformFont() {
        return UNIFORM_FONT;
    }

    public static Text uniform(String value) {
        return Text.literal(value).setStyle(Style.EMPTY.withFont(UNIFORM_FONT));
    }

    public static int accent() {
        return mix(0xFF8FA7D8, 0xFF6F84B2, 0.52f);
    }

    public static int accentSoft() {
        return mix(accent(), 0xFF090A0D, 0.80f);
    }

    public static int accentBright() {
        return mix(accent(), 0xFFFFFFFF, 0.42f);
    }

    public static int panel() {
        return PANEL.getRGB();
    }

    public static int panelAlt() {
        return PANEL_ALT.getRGB();
    }

    public static int panelEdge() {
        return PANEL_EDGE.getRGB();
    }

    public static int text() {
        return TEXT.getRGB();
    }

    public static int mutedText() {
        return MUTED_TEXT.getRGB();
    }

    public static int success() {
        return SUCCESS.getRGB();
    }

    public static int danger() {
        return DANGER.getRGB();
    }

    public static void drawBackdrop(DrawContext context, int width, int height) {
        context.fillGradient(0, 0, width, height, BACKDROP_TOP.getRGB(), BACKDROP_BOTTOM.getRGB());

        drawOrb(context, width - 180, 110, 190, withAlpha(accentBright(), 26));
        drawOrb(context, 132, 118, 96, withAlpha(accent(), 12));
        drawOrb(context, 110, height - 120, 130, withAlpha(accentSoft(), 16));
        drawOrb(context, width / 2 - 90, height - 80, 170, withAlpha(accentSoft(), 10));
        drawOrb(context, width / 2, height / 2 + 30, 220, withAlpha(0xFFFFFFFF, 8));
    }

    public static void drawOrb(DrawContext context, int centerX, int centerY, int radius, int color) {
        int alpha = (color >>> 24) & 0xFF;
        for (int i = 0; i < 4; i++) {
            int currentRadius = radius - i * 18;
            if (currentRadius <= 0) {
                break;
            }
            int currentAlpha = Math.max(6, alpha / (4 + i));
            Render2DEngine.drawRound(context.getMatrices(), centerX - currentRadius, centerY - currentRadius,
                    currentRadius * 2.0f, currentRadius * 2.0f, currentRadius,
                    new Color(withAlpha(color, currentAlpha), true));
        }
    }

    public static void drawCard(DrawContext context, int x, int y, int width, int height, int fill, int edge, int radius) {
        Render2DEngine.drawBlurredShadow(context.getMatrices(), x, y, width, height, 3, new Color(withAlpha(edge, 20), true));
        Render2DEngine.drawRound(context.getMatrices(), x, y, width, height, radius, new Color(withAlpha(edge, 126), true));
        Render2DEngine.drawRound(context.getMatrices(), x + 1, y + 1, width - 2.0f, height - 2.0f, Math.max(1, radius - 1),
                new Color(fill, true));
        Render2DEngine.drawRound(context.getMatrices(), x + 2, y + 2, width - 4.0f, height - 4.0f, Math.max(1, radius - 2),
                new Color(withAlpha(0xFFFFFFFF, 4), true));
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    public static int mix(int first, int second, float progress) {
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        int a1 = (first >>> 24) & 0xFF;
        int r1 = (first >>> 16) & 0xFF;
        int g1 = (first >>> 8) & 0xFF;
        int b1 = first & 0xFF;

        int a2 = (second >>> 24) & 0xFF;
        int r2 = (second >>> 16) & 0xFF;
        int g2 = (second >>> 8) & 0xFF;
        int b2 = second & 0xFF;

        int a = (int) (a1 + (a2 - a1) * progress);
        int r = (int) (r1 + (r2 - r1) * progress);
        int g = (int) (g1 + (g2 - g1) * progress);
        int b = (int) (b1 + (b2 - b1) * progress);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
