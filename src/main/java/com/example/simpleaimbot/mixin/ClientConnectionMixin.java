package com.example.simpleaimbot.mixin;

import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.utils.InventoryMoveHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(method = "send", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (ConfigManager.getConfig().inventoryMoveEnabled) {
            if (client.currentScreen != null && packet instanceof ClickSlotC2SPacket) {
                boolean isMoving = player.input.movementForward != 0 || player.input.movementSideways != 0;
                if (isMoving) {
                    InventoryMoveHelper.storePacket((ClickSlotC2SPacket) packet);
                    ci.cancel();
                }
            }

            if (packet instanceof CloseHandledScreenC2SPacket) {
                if (!InventoryMoveHelper.pendingClicks.isEmpty()) {
                    InventoryMoveHelper.scheduleClose();
                    ci.cancel();
                }
            }
        }
    }
}