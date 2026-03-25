package com.example.simpleaimbot.utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

public class TargetUtils {
    public static boolean isValidTarget(PlayerEntity player, Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (living == player) return false;
        if (!living.isAlive()) return false;
        // Если хотите разрешить мобов, замените следующую строку на return true;
        return living instanceof PlayerEntity;
    }

    public static boolean canSee(PlayerEntity player, Entity target, boolean wallCheckEnabled) {
        if (!wallCheckEnabled) return true;
        return player.canSee(target);
    }

    /**
     * Расстояние от игрока до ближайшей точки хитбокса цели.
     */
    public static double distanceToBoundingBox(PlayerEntity player, Entity target) {
        Box box = target.getBoundingBox();
        double eyeY = player.getY() + player.getEyeHeight(player.getPose());
        double closestX = MathHelper.clamp(player.getX(), box.minX, box.maxX);
        double closestY = MathHelper.clamp(eyeY, box.minY, box.maxY);
        double closestZ = MathHelper.clamp(player.getZ(), box.minZ, box.maxZ);
        double dx = closestX - player.getX();
        double dy = closestY - eyeY;
        double dz = closestZ - player.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}