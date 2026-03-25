package com.example.simpleaimbot.rendering;

import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import com.example.simpleaimbot.gui.GuiTheme;
import com.example.simpleaimbot.utils.animation.AnimationHelper;
import com.example.simpleaimbot.utils.animation.ColorUtils;
import com.example.simpleaimbot.utils.render.Render2DEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrayListRenderer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final int PADDING_X = 8;
    private static final int PADDING_Y = 3;
    private static final int SPACING = 0;
    private static final Map<String, AnimationHelper> MODULE_ANIMATIONS = new HashMap<>();

    public static void render(DrawContext context) {
        ModConfig config = ConfigManager.getConfig();
        if (!config.arrayListEnabled || (mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen))) {
            return;
        }

        List<String> enabledModules = getEnabledModules();
        List<String> allModules = getAllModules();
        for (String module : allModules) {
            AnimationHelper animation = MODULE_ANIMATIONS.computeIfAbsent(module, key -> new AnimationHelper());
            animation.update(enabledModules.contains(module));
            animation.tick();
        }

        enabledModules.sort((left, right) -> Integer.compare(textWidth(right), textWidth(left)));

        boolean rightAligned = config.arrayListRight;
        int screenWidth = mc.getWindow().getScaledWidth();
        int xBase = rightAligned ? screenWidth - 8 : 8;
        int y = config.watermarkEnabled && rightAligned ? 56 : 8;

        MatrixStack matrices = context.getMatrices();
        matrices.push();

        for (String module : enabledModules) {
            AnimationHelper animation = MODULE_ANIMATIONS.get(module);
            float alpha = animation.getValue();
            if (alpha <= 0.02f) {
                continue;
            }

            Text text = GuiTheme.uniform(module);
            int textWidth = mc.textRenderer.getWidth(text);
            int width = textWidth + PADDING_X * 2 + 6;
            int height = mc.textRenderer.fontHeight + PADDING_Y * 2;
            int slide = Math.round((1.0f - alpha) * 18);
            int x = rightAligned ? xBase - width + slide : xBase - slide;

            int pulse = ColorUtils.getPulsingColorWithOffset(config.arrayListGlowColor, System.currentTimeMillis() / 40 + y * 2L);
            int fill = GuiTheme.withAlpha(GuiTheme.mix(GuiTheme.panel(), 0xFF0F1725, 0.24f), Math.round(164 + 46 * alpha));
            int edge = GuiTheme.withAlpha(GuiTheme.mix(pulse, GuiTheme.accentBright(), 0.22f), Math.round(64 + 42 * alpha));

            Render2DEngine.drawRound(matrices, x, y, width, height, 8, new Color(GuiTheme.withAlpha(edge, 82), true));
            Render2DEngine.drawRound(matrices, x + 1, y + 1, width - 2.0f, height - 2.0f, 7, new Color(fill, true));

            int barX = rightAligned ? x + width - 3 : x;
            Render2DEngine.drawRound(matrices, barX, y + 2, 3.0f, height - 4.0f, 2.0f, new Color(GuiTheme.withAlpha(pulse, 168), true));

            context.drawText(mc.textRenderer, text, x + PADDING_X, y + PADDING_Y, ColorUtils.injectAlpha(GuiTheme.text(), Math.round(alpha * 255)), false);
            y += height + SPACING;
        }

        matrices.pop();
    }

    private static int textWidth(String module) {
        return mc.textRenderer.getWidth(GuiTheme.uniform(module));
    }

    private static List<String> getEnabledModules() {
        List<String> list = new ArrayList<>();
        ModConfig config = ConfigManager.getConfig();

        if (config.triggerbotEnabled) list.add("Triggerbot");
        if (config.killauraEnabled) list.add("KillAura");
        if (config.jumpCircleEnabled) list.add("JumpCircle");
        if (config.fullbrightEnabled) list.add("Fullbright");
        if (config.playerEspEnabled) list.add("PlayerESP");
        if (config.targetEspEnabled) list.add("TargetESP");
        if (config.targetHudEnabled) list.add("TargetHUD");
        if (config.lowFireEnabled) list.add("LowFire");
        if (config.attackLinesEnabled) list.add("AttackLines");
        if (config.watermarkEnabled) list.add("Watermark");
        if (config.flyEnabled) list.add("Fly");
        if (config.autoSprintEnabled) list.add("AutoSprint");
        if (config.inventoryMoveEnabled) list.add("InventoryMove");

        return list;
    }

    private static List<String> getAllModules() {
        List<String> list = new ArrayList<>();
        list.add("Triggerbot");
        list.add("KillAura");
        list.add("JumpCircle");
        list.add("Fullbright");
        list.add("PlayerESP");
        list.add("TargetESP");
        list.add("TargetHUD");
        list.add("LowFire");
        list.add("AttackLines");
        list.add("Watermark");
        list.add("Fly");
        list.add("AutoSprint");
        list.add("InventoryMove");
        return list;
    }
}
