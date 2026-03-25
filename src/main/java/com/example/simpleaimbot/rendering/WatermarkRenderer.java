package com.example.simpleaimbot.rendering;

import com.example.simpleaimbot.client.ClientBranding;
import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import com.example.simpleaimbot.gui.GuiTheme;
import com.example.simpleaimbot.utils.render.Render2DEngine;
import com.example.simpleaimbot.utils.render.TextureStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public final class WatermarkRenderer {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final int DEFAULT_Y = 8;
    private static final int HEIGHT = 48;
    private static final int ICON_BOX_SIZE = 40;
    private static final int ICON_DRAW_SIZE = 34;
    private static final int ICON_SOURCE_SIZE = 64;
    private static final int SCREEN_MARGIN = 8;
    private static boolean dragging = false;
    private static boolean previousMouseDown = false;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;

    private WatermarkRenderer() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        ModConfig config = ConfigManager.getConfig();
        if (!config.watermarkEnabled) {
            dragging = false;
            previousMouseDown = false;
            return;
        }
        if (CLIENT.currentScreen != null && !(CLIENT.currentScreen instanceof ChatScreen)) {
            dragging = false;
            previousMouseDown = false;
            return;
        }

        String versionText = "v" + ClientBranding.VERSION;
        String fpsText = CLIENT.getCurrentFps() + " FPS";
        String pingText = pingText();

        int chipGap = 6;
        int titleWidth = CLIENT.textRenderer.getWidth(GuiTheme.uniform(ClientBranding.NAME));
        int versionWidth = CLIENT.textRenderer.getWidth(GuiTheme.uniform(versionText));
        int firstChipWidth = chipWidth(fpsText);
        int secondChipWidth = chipWidth(pingText);
        int width = 24 + ICON_BOX_SIZE + 10 + Math.max(titleWidth + versionWidth + 18, firstChipWidth + secondChipWidth + chipGap);
        int screenWidth = CLIENT.getWindow().getScaledWidth();
        int screenHeight = CLIENT.getWindow().getScaledHeight();
        int defaultX = screenWidth - width - 12;
        int effectsOffsetY = statusEffectsOffset();
        int minY = DEFAULT_Y + effectsOffsetY;

        if (!config.watermarkCustomPosition) {
            config.watermarkX = defaultX;
            config.watermarkY = minY;
            config.watermarkCustomPosition = true;
            ConfigManager.save();
        }

        int x = MathHelper.clamp(config.watermarkX, SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenWidth - width - SCREEN_MARGIN));
        int y = MathHelper.clamp(config.watermarkY, minY, Math.max(minY, screenHeight - HEIGHT - SCREEN_MARGIN));
        if (x != config.watermarkX || y != config.watermarkY) {
            config.watermarkX = x;
            config.watermarkY = y;
            ConfigManager.save();
        }

        handleDrag(config, x, y, width, HEIGHT, screenWidth, screenHeight, minY);

        int accent = GuiTheme.mix(config.guiPrimaryColor, 0xFF2F6BFF, 0.32f);
        int fill = GuiTheme.withAlpha(GuiTheme.mix(GuiTheme.panel(), 0xFF050811, 0.16f), 220);
        int edge = GuiTheme.withAlpha(GuiTheme.mix(accent, 0xFFFFFFFF, 0.12f), 132);

        Render2DEngine.drawBlurredShadow(context.getMatrices(), x, y, width, HEIGHT, 2, new Color(GuiTheme.withAlpha(accent, 12), true));
        GuiTheme.drawCard(context, x, y, width, HEIGHT, fill, edge, 14);

        int iconInsetX = 8;
        int iconCardX = x + iconInsetX;
        int iconCardY = y + (HEIGHT - ICON_BOX_SIZE) / 2;
        Render2DEngine.drawRound(context.getMatrices(), iconCardX, iconCardY, ICON_BOX_SIZE, ICON_BOX_SIZE, 11,
                new Color(GuiTheme.withAlpha(GuiTheme.panelAlt(), 224), true));

        int iconX = iconCardX + (ICON_BOX_SIZE - ICON_DRAW_SIZE) / 2;
        int iconY = iconCardY + (ICON_BOX_SIZE - ICON_DRAW_SIZE) / 2;
        float iconScale = ICON_DRAW_SIZE / (float) ICON_SOURCE_SIZE;
        context.getMatrices().push();
        context.getMatrices().translate(iconX, iconY, 0.0f);
        context.getMatrices().scale(iconScale, iconScale, 1.0f);
        context.drawTexture(RenderLayer::getGuiTextured, TextureStorage.POLAR_WATERMARK, 0, 0,
                0.0f, 0.0f, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE);
        context.getMatrices().pop();

        int textX = x + iconInsetX + ICON_BOX_SIZE + 10;
        int titleY = y + 8;
        context.drawTextWithShadow(CLIENT.textRenderer, GuiTheme.uniform(ClientBranding.NAME), textX, titleY, GuiTheme.text());
        context.drawText(CLIENT.textRenderer, GuiTheme.uniform(versionText), textX + titleWidth + 8, titleY + 1,
                GuiTheme.withAlpha(GuiTheme.accentBright(), 236), false);

        int chipsY = y + 26;
        drawChip(context, textX, chipsY, fpsText, accent);
        drawChip(context, textX + firstChipWidth + chipGap, chipsY, pingText, GuiTheme.mix(accent, 0xFF86A9FF, 0.26f));
    }

    private static void handleDrag(ModConfig config, int x, int y, int width, int height, int screenWidth, int screenHeight, int minY) {
        if (!(CLIENT.currentScreen instanceof ChatScreen)) {
            dragging = false;
            previousMouseDown = false;
            return;
        }

        long handle = CLIENT.getWindow().getHandle();
        boolean mouseDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        double rawMouseX = CLIENT.mouse.getX() * screenWidth / (double) CLIENT.getWindow().getWidth();
        double rawMouseY = CLIENT.mouse.getY() * screenHeight / (double) CLIENT.getWindow().getHeight();
        int mouseX = (int) Math.round(rawMouseX);
        int mouseY = (int) Math.round(rawMouseY);

        if (mouseDown && !previousMouseDown && isHovering(mouseX, mouseY, x, y, width, height)) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
        } else if (!mouseDown) {
            dragging = false;
        }

        if (dragging) {
            int newX = MathHelper.clamp(mouseX - dragOffsetX, SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenWidth - width - SCREEN_MARGIN));
            int newY = MathHelper.clamp(mouseY - dragOffsetY, minY, Math.max(minY, screenHeight - height - SCREEN_MARGIN));
            if (newX != config.watermarkX || newY != config.watermarkY) {
                config.watermarkX = newX;
                config.watermarkY = newY;
                config.watermarkCustomPosition = true;
                ConfigManager.save();
            }
        }

        previousMouseDown = mouseDown;
    }

    private static boolean isHovering(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static int statusEffectsOffset() {
        if (CLIENT.player == null || CLIENT.player.getStatusEffects().isEmpty()) {
            return 0;
        }
        return 26;
    }

    private static void drawChip(DrawContext context, int x, int y, String text, int accent) {
        int width = chipWidth(text);
        GuiTheme.drawCard(context, x, y, width, 12,
                GuiTheme.withAlpha(GuiTheme.panelAlt(), 220),
                GuiTheme.withAlpha(accent, 124),
                6);
        context.drawCenteredTextWithShadow(CLIENT.textRenderer, GuiTheme.uniform(text), x + width / 2, y + 2, GuiTheme.text());
    }

    private static int chipWidth(String text) {
        return CLIENT.textRenderer.getWidth(GuiTheme.uniform(text)) + 14;
    }

    private static String pingText() {
        if (CLIENT.isInSingleplayer()) {
            return "local";
        }
        if (CLIENT.player == null || CLIENT.getNetworkHandler() == null) {
            return "-- ms";
        }

        PlayerListEntry entry = CLIENT.getNetworkHandler().getPlayerListEntry(CLIENT.player.getUuid());
        return entry != null ? entry.getLatency() + " ms" : "-- ms";
    }
}
