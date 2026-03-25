package com.example.simpleaimbot.rendering;

import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import com.example.simpleaimbot.gui.GuiTheme;
import com.example.simpleaimbot.modules.KillAura;
import com.example.simpleaimbot.utils.render.Render2DEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardScore;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class TargetHudRenderer {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final int WIDTH = 182;
    private static final int HEIGHT = 78;
    private static final int MARGIN_BOTTOM = 88;

    private static final Map<String, DamageRecord> DAMAGE_RECORDS = new HashMap<>();

    private static class DamageRecord {
        float damageValue;
        long timestamp;
        float lastHealth;
    }

    @SuppressWarnings("unused")
    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (client.currentScreen != null && !(client.currentScreen instanceof ChatScreen)) {
            return;
        }

        ModConfig config = ConfigManager.getConfig();
        if (!config.targetHudEnabled || !config.killauraEnabled) {
            return;
        }

        Entity target = KillAura.getTarget();
        if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int x = screenWidth / 2 - WIDTH / 2;
        int y = screenHeight - HEIGHT - MARGIN_BOTTOM;

        int accent = config.targetHudGlowColor;
        int fill = GuiTheme.withAlpha(GuiTheme.mix(GuiTheme.panel(), 0xFF0B1019, 0.22f), 214);
        int edge = GuiTheme.withAlpha(GuiTheme.mix(accent, GuiTheme.accentBright(), 0.28f), 180);

        Render2DEngine.drawBlurredShadow(context.getMatrices(), x, y, WIDTH, HEIGHT, 6, new Color(GuiTheme.withAlpha(accent, 30), true));
        Render2DEngine.drawRound(context.getMatrices(), x, y, WIDTH, HEIGHT, 14, new Color(GuiTheme.withAlpha(edge, 150), true));
        Render2DEngine.drawRound(context.getMatrices(), x + 1, y + 1, WIDTH - 2.0f, HEIGHT - 2.0f, 13, new Color(fill, true));
        Render2DEngine.drawRound(context.getMatrices(), x + 2, y + 2, WIDTH - 4.0f, HEIGHT - 4.0f, 12, new Color(GuiTheme.withAlpha(0xFFFFFFFF, 8), true));

        int faceSize = 42;
        int faceX = x + 10;
        int faceY = y + 10;

        if (livingTarget instanceof AbstractClientPlayerEntity abstractPlayer) {
            SkinTextures skinTextures = abstractPlayer.getSkinTextures();
            PlayerSkinDrawer.draw(context, skinTextures, faceX, faceY, faceSize);
        } else {
            context.fill(faceX, faceY, faceX + faceSize, faceY + faceSize, GuiTheme.withAlpha(GuiTheme.panelAlt(), 255));
        }

        DamageRecord record = DAMAGE_RECORDS.get(livingTarget.getUuidAsString());
        if (record != null && record.timestamp > 0) {
            long elapsed = System.currentTimeMillis() - record.timestamp;
            if (elapsed < 220) {
                float intensity = 1.0f - (float) elapsed / 220.0f;
                int overlay = GuiTheme.withAlpha(0xFFFF4D4D, Math.round(90 * intensity));
                context.fill(faceX, faceY, faceX + faceSize, faceY + faceSize, overlay);
            }
        }

        int textX = faceX + faceSize + 10;
        int lineY = y + 10;
        int textWidth = WIDTH - (textX - x) - 12;

        context.drawTextWithShadow(client.textRenderer, GuiTheme.uniform(livingTarget.getName().getString()), textX, lineY, GuiTheme.text());
        context.drawText(client.textRenderer, GuiTheme.uniform("Target HUD"), textX, lineY + 12, GuiTheme.withAlpha(GuiTheme.accentBright(), 228), false);

        float[] healthData = getHealthFromScoreboard(livingTarget);
        float health = healthData[0];
        float maxHealth = Math.max(healthData[1], 1.0f);
        updateDamageRecord(livingTarget, health, maxHealth);
        float healthPercent = MathHelper.clamp(health / maxHealth, 0.0f, 1.0f);

        int barY = y + 38;
        int barHeight = 8;
        GuiTheme.drawCard(context, textX, barY, textWidth, barHeight,
                GuiTheme.withAlpha(GuiTheme.panelAlt(), 220), GuiTheme.withAlpha(GuiTheme.text(), 24), 5);
        int filledWidth = Math.max(4, Math.round(textWidth * healthPercent));
        Render2DEngine.drawRound(context.getMatrices(), textX + 1, barY + 1, Math.max(1, filledWidth - 2), barHeight - 2, 4,
                new Color(GuiTheme.mix(0xFFED6A5E, 0xFF68E89B, healthPercent), true));

        if (record != null && record.damageValue > 0) {
            long elapsed = System.currentTimeMillis() - record.timestamp;
            if (elapsed < 500) {
                float fade = 1.0f - elapsed / 500.0f;
                float damagePercent = Math.min(record.damageValue / maxHealth, Math.max(0.0f, 1.0f - healthPercent));
                int damageWidth = Math.round(textWidth * damagePercent);
                if (damageWidth > 0) {
                    context.fill(textX + filledWidth, barY + 1, Math.min(textX + textWidth - 1, textX + filledWidth + damageWidth),
                            barY + barHeight - 1, GuiTheme.withAlpha(0xFFFF5858, Math.round(160 * fade)));
                }
            } else {
                record.damageValue = 0;
            }
        }

        String healthLine = Math.round(health) + " / " + Math.round(maxHealth) + " HP";
        context.drawText(client.textRenderer, GuiTheme.uniform(healthLine), textX, barY + 12, GuiTheme.text(), false);

        String distanceLine = "Dist: " + String.format("%.1f", client.player != null ? client.player.distanceTo(livingTarget) : 0.0f);
        context.drawText(client.textRenderer, GuiTheme.uniform(distanceLine), textX + textWidth - client.textRenderer.getWidth(distanceLine), barY + 12,
                GuiTheme.mutedText(), false);

        ItemStack mainHand = livingTarget.getMainHandStack();
        if (!mainHand.isEmpty()) {
            context.drawText(client.textRenderer, GuiTheme.uniform("Item"), textX, y + 59, GuiTheme.mutedText(), false);
            context.drawItem(mainHand, textX + 28, y + 55);
        }
    }

    private static void updateDamageRecord(LivingEntity target, float currentHealth, float maxHealth) {
        String uuid = target.getUuidAsString();
        DamageRecord record = DAMAGE_RECORDS.get(uuid);
        if (record == null) {
            record = new DamageRecord();
            record.lastHealth = currentHealth;
            DAMAGE_RECORDS.put(uuid, record);
            return;
        }
        if (currentHealth < record.lastHealth) {
            float damage = record.lastHealth - currentHealth;
            if (damage > 0) {
                record.damageValue = Math.min(damage, maxHealth);
                record.timestamp = System.currentTimeMillis();
            }
        }
        record.lastHealth = currentHealth;
    }

    private static float[] getHealthFromScoreboard(LivingEntity target) {
        float defaultHealth = target.getHealth() + target.getAbsorptionAmount();
        float maxHealth = target.getMaxHealth();

        Scoreboard scoreboard = client.world != null ? client.world.getScoreboard() : null;
        if (scoreboard == null) {
            return new float[] { defaultHealth, maxHealth };
        }

        String targetName = target.getName().getString();
        String cleanName = targetName.replaceAll("§[0-9a-fk-or]", "");
        String[] possibleObjectives = { "health", "hp", "❤", "здоровье", "healthbar", "player_health" };

        for (String objectiveName : possibleObjectives) {
            ScoreboardObjective objective = scoreboard.getNullableObjective(objectiveName);
            if (objective != null) {
                float[] resolved = resolveScore(scoreboard, objective, targetName, cleanName, maxHealth);
                if (resolved != null) {
                    return resolved;
                }
            }
        }

        for (ScoreboardObjective objective : scoreboard.getObjectives()) {
            float[] resolved = resolveScore(scoreboard, objective, targetName, cleanName, maxHealth);
            if (resolved != null) {
                return resolved;
            }
        }

        return new float[] { defaultHealth, maxHealth };
    }

    private static float[] resolveScore(Scoreboard scoreboard, ScoreboardObjective objective, String targetName, String cleanName, float maxHealth) {
        int rawScore = readScore(scoreboard, objective, targetName);
        if (rawScore > 0 && rawScore < 1000) {
            return new float[] { rawScore, maxHealth };
        }

        rawScore = readScore(scoreboard, objective, cleanName);
        if (rawScore > 0 && rawScore < 1000) {
            return new float[] { rawScore, maxHealth };
        }

        return null;
    }

    private static int readScore(Scoreboard scoreboard, ScoreboardObjective objective, String name) {
        ScoreboardScore score = (ScoreboardScore) scoreboard.getScore(ScoreHolder.fromName(name), objective);
        return score != null ? score.getScore() : -1;
    }
}
