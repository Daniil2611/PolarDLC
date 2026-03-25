package com.example.simpleaimbot.mixin;

import com.example.simpleaimbot.freelook.freelook.CameraOverriddenEntity;
import com.example.simpleaimbot.freelook.freelook.FreeLookMod;
import com.example.simpleaimbot.modules.KillAura;
import com.example.simpleaimbot.utils.InventoryMoveHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void onTickMovement(CallbackInfo ci) {
        if (InventoryMoveHelper.freezeMovement) {
            ci.cancel();
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        if (!FreeLookMod.isAutoFreeLooking()
                || player.isRiding()
                || client == null
                || !KillAura.hasTargetInRange()
                || !(player instanceof CameraOverriddenEntity cameraOverriddenEntity)) {
            return;
        }

        float movementForward = player.input.movementForward;
        float movementSideways = player.input.movementSideways;
        if (movementForward == 0.0f && movementSideways == 0.0f) {
            return;
        }

        float delta = MathHelper.wrapDegrees(player.getYaw() - cameraOverriddenEntity.freelook$getCameraYaw()) * MathHelper.RADIANS_PER_DEGREE;
        float cos = MathHelper.cos(delta);
        float sin = MathHelper.sin(delta);

        player.input.movementForward = movementForward * cos - movementSideways * sin;
        player.input.movementSideways = movementForward * sin + movementSideways * cos;
    }
}