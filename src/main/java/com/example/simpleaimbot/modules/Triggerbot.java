package com.example.simpleaimbot.modules;

import com.example.simpleaimbot.client.SprintResetController;
import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import com.example.simpleaimbot.utils.TargetUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class Triggerbot {
    private static int delay = 0;
    private static final double FIXED_RANGE = 3.0;
    private static final int ATTACK_DELAY_TICKS = 3; // изменено с 5 на 3
    private static long lastAttackTime = 0;
    private static long lastSprintResetTime = 0;

    public static long getLastAttackTime() { return lastAttackTime; }
    public static long getLastSprintResetTime() { return lastSprintResetTime; }

    @SuppressWarnings("ConstantConditions")
    public static void tick(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        ModConfig config = ConfigManager.getConfig();
        if (!config.triggerbotEnabled) return;

        if (player.isUsingItem()) return;

        if (delay > 0) {
            delay--;
            return;
        }

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult entityHit)) return;

        Entity target = entityHit.getEntity();
        if (!TargetUtils.isValidTarget(player, target)) return;
        if (player.distanceTo(target) > FIXED_RANGE) return;

        // Используем getAttackCooldownProgress(1.0f) для точной проверки
        float cooldown = player.getAttackCooldownProgress(1.0f);
        if (cooldown < 0.99f) return; // порог 0.99

        if (config.criticals) {
            if (player.isOnGround()) return;
            if (player.getVelocity().y >= -0.1) return;
            // Упрощённая проверка падения – без fallTicks/fallStartY
        }

        performAttack(client, player, target);
    }

    private static void performAttack(MinecraftClient client, PlayerEntity player, Entity target) {
        boolean shouldRestoreSprint = preAttackSprintReset(client, player);
        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
        postAttackSprintReset(client, player, shouldRestoreSprint);
        delay = ATTACK_DELAY_TICKS;
        lastAttackTime = System.currentTimeMillis();
    }

    private static boolean preAttackSprintReset(MinecraftClient client, PlayerEntity player) {
        if (!SprintResetController.disableSprint(client, player)) {
            return false;
        }

        lastSprintResetTime = System.currentTimeMillis();
        return true;
    }

    private static void postAttackSprintReset(MinecraftClient client, PlayerEntity player, boolean shouldRestoreSprint) {
        SprintResetController.enableSprint(client, player, shouldRestoreSprint);
    }
}
