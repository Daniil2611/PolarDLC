package com.example.simpleaimbot.mixin.freelook;

import com.example.simpleaimbot.freelook.freelook.CameraOverriddenEntity;
import com.example.simpleaimbot.freelook.freelook.FreeLookMod;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin implements CameraOverriddenEntity {
    @Unique
    private float cameraPitch;

    @Unique
    private float cameraYaw;

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void onChangeLookDirection(double xDelta, double yDelta, CallbackInfo ci) {
        if ((Object) this instanceof ClientPlayerEntity) {
            if (FreeLookMod.isFreeLooking) {
                double pitchDelta = yDelta * 0.15;
                double yawDelta = xDelta * 0.15;

                this.cameraPitch = MathHelper.clamp(this.cameraPitch + (float) pitchDelta, -90.0f, 90.0f);
                this.cameraYaw += (float) yawDelta;

                ci.cancel(); // блокируем поворот тела мышью
            }
        }
    }

    @Override
    @Unique
    public float freelook$getCameraPitch() {
        return this.cameraPitch;
    }

    @Override
    @Unique
    public float freelook$getCameraYaw() {
        return this.cameraYaw;
    }

    @Override
    @Unique
    public void freelook$setCameraPitch(float pitch) {
        this.cameraPitch = pitch;
    }

    @Override
    @Unique
    public void freelook$setCameraYaw(float yaw) {
        this.cameraYaw = yaw;
    }
}