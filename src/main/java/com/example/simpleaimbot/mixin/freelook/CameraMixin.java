package com.example.simpleaimbot.mixin.freelook;

import com.example.simpleaimbot.freelook.freelook.CameraOverriddenEntity;
import com.example.simpleaimbot.freelook.freelook.FreeLookMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Unique
    private boolean firstTime = true;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", shift = At.Shift.AFTER))
    private void onUpdate(BlockView blockView, Entity entity, boolean bl, boolean bl2, float f, CallbackInfo ci) {
        if (FreeLookMod.isFreeLooking && entity instanceof ClientPlayerEntity) {
            if (entity instanceof CameraOverriddenEntity cameraOverriddenEntity) {
                if (firstTime && MinecraftClient.getInstance().player != null) {
                    cameraOverriddenEntity.freelook$setCameraPitch(MinecraftClient.getInstance().player.getPitch());
                    cameraOverriddenEntity.freelook$setCameraYaw(MinecraftClient.getInstance().player.getYaw());
                    firstTime = false;
                }
                this.setRotation(cameraOverriddenEntity.freelook$getCameraYaw(), cameraOverriddenEntity.freelook$getCameraPitch());
            }
        }
        if (!FreeLookMod.isFreeLooking && entity instanceof ClientPlayerEntity) {
            firstTime = true;
        }
    }
}