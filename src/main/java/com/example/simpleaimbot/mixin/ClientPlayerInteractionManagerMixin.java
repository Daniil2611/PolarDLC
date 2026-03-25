package com.example.simpleaimbot.mixin;

import com.example.simpleaimbot.SimpleAimbotMod;
import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import com.example.simpleaimbot.modules.KillAura;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        SimpleAimbotMod.updateLastAttackTime();

        ModConfig config = ConfigManager.getConfig();
        if (config.rotationMode == ModConfig.RotationMode.FUNTIME_SNAP) {
            if (config.killauraEnabled && KillAura.getTarget() == target) {
                KillAura.snapToTarget(player);
            }
        }
    }
}