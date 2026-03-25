package com.example.simpleaimbot.utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class RotationUtils {

    public static float[] getRotationsToEntity(PlayerEntity player, Entity target, float basePrediction, float maxPrediction, Random random) {
        Box box = target.getBoundingBox();
        Vec3d eyePos = player.getEyePos();

        double minY = box.minY;
        double maxY = box.maxY - 0.5;
        if (maxY < minY) maxY = minY;

        double targetY = minY + random.nextDouble() * (maxY - minY);
        double targetX = box.minX + random.nextDouble() * (box.maxX - box.minX);
        double targetZ = box.minZ + random.nextDouble() * (box.maxZ - box.minZ);

        Vec3d targetPos = new Vec3d(targetX, targetY, targetZ);

        Vec3d velocity = target.getVelocity();
        if (velocity.lengthSquared() > 0.01) {
            float predictionTime = basePrediction + random.nextFloat() * (maxPrediction - basePrediction);
            targetPos = targetPos.add(velocity.multiply(predictionTime));
        }

        Vec3d toTarget = targetPos.subtract(eyePos);
        double horizontalDistance = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        float targetPitch = (float) Math.toDegrees(Math.atan2(-toTarget.y, horizontalDistance));

        return new float[]{targetYaw, targetPitch};
    }

    public static float[] getRotationsToEntityCenter(PlayerEntity player, Entity target, float basePrediction, float maxPrediction, Random random) {
        Box box = target.getBoundingBox();
        Vec3d center = box.getCenter();

        Vec3d velocity = target.getVelocity();
        if (velocity.lengthSquared() > 0.01) {
            float predictionTime = basePrediction + random.nextFloat() * (maxPrediction - basePrediction);
            center = center.add(velocity.multiply(predictionTime));
        }

        Vec3d eyePos = player.getEyePos();
        Vec3d toTarget = center.subtract(eyePos);
        double horizontalDistance = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        float targetPitch = (float) Math.toDegrees(Math.atan2(-toTarget.y, horizontalDistance));

        return new float[]{targetYaw, targetPitch};
    }

    public static float[] getRotationsToBody(PlayerEntity player, Entity target, float basePrediction, float maxPrediction, Random random) {
        Box box = target.getBoundingBox();
        Vec3d eyePos = player.getEyePos();

        double bodyY = box.minY + (box.maxY - box.minY) * 0.55;
        double maxAllowedY = box.maxY - 0.5;
        if (bodyY > maxAllowedY) bodyY = maxAllowedY;

        double targetX = (box.minX + box.maxX) * 0.5;
        double targetZ = (box.minZ + box.maxZ) * 0.5;

        Vec3d targetPos = new Vec3d(targetX, bodyY, targetZ);

        Vec3d velocity = target.getVelocity();
        if (velocity.lengthSquared() > 0.01) {
            float predictionTime = basePrediction + random.nextFloat() * (maxPrediction - basePrediction);
            targetPos = targetPos.add(velocity.multiply(predictionTime));
        }

        Vec3d toTarget = targetPos.subtract(eyePos);
        double horizontalDistance = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        float targetPitch = (float) Math.toDegrees(Math.atan2(-toTarget.y, horizontalDistance));

        return new float[]{targetYaw, targetPitch};
    }

    public static float applyGcdFix(float delta, float mouseSensitivity) {
        float gcd = (float) (Math.pow(mouseSensitivity * 0.6 + 0.2, 3) * 1.2);
        return delta - (delta % gcd);
    }
}