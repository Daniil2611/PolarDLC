package com.example.simpleaimbot.mixin;

import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Overwrite
    private void swingArm(float swingProgress, float equipProgress, MatrixStack matrices, int direction, Arm arm) {
        ModConfig config = ConfigManager.getConfig();
        if (config.swingAnimationEnabled && arm == Arm.RIGHT) {
            float progress = MathHelper.clamp(swingProgress, 0.0F, 1.0F);
            switch (config.swingAnimationStyle) {
                case 1 -> styleLunar(progress, matrices, direction);
                case 3 -> styleVelocity(progress, matrices, direction);
                default -> styleEleven(progress, matrices, direction);
            }
            return;
        }

        applyEquipOffset(matrices, arm, equipProgress);
        float h = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        float j = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * ((float) Math.PI * 2F));
        float k = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
        matrices.translate((float) direction * h, j, k);
        applySwingOffset(matrices, arm, swingProgress);
    }

    @Unique
    private void styleVelocity(float progress, MatrixStack matrices, int direction) {
        float root = MathHelper.sin(MathHelper.sqrt(progress) * (float) Math.PI);
        float wave = MathHelper.sin(progress * (float) Math.PI);
        applyStyleBase(matrices, direction);
        matrices.translate(direction * 0.018F * root, 0.010F * wave, 0.022F * root);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(32.0F + root * 10.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-50.0F + wave * 6.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(70.0F + root * 10.0F));
    }

    @Unique
    private void styleEleven(float progress, MatrixStack matrices, int direction) {
        float root = MathHelper.sin(MathHelper.sqrt(progress) * (float) Math.PI);
        applyStyleBase(matrices, direction);
        matrices.translate(direction * 0.010F * root, 0.012F * root, 0.018F * root);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-44.0F + root * 3.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(86.0F + root * 10.0F));
    }

    @Unique
    private void styleLunar(float progress, MatrixStack matrices, int direction) {
        float root = MathHelper.sin(MathHelper.sqrt(progress) * (float) Math.PI);
        float wave = MathHelper.sin(progress * (float) Math.PI);
        applyStyleBase(matrices, direction);
        matrices.translate(direction * -0.015F * root, 0.020F * root, 0.014F * wave);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(34.0F + root * 10.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-58.0F - root * 6.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(96.0F + wave * 8.0F));
    }

    @Unique
    private void applyStyleBase(MatrixStack matrices, int direction) {
        matrices.translate(0.58F * direction, -0.44F, -0.92F);
    }

    @Unique
    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    @Unique
    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float g = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + g * -20.0F)));
        float h = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * h * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
    }
}
