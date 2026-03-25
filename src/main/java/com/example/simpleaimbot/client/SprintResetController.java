package com.example.simpleaimbot.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public final class SprintResetController {
    private SprintResetController() {
    }

    public static boolean disableSprint(MinecraftClient client, PlayerEntity player) {
        if (player == null || !player.isSprinting()) {
            return false;
        }

        player.setSprinting(false);
        client.options.sprintKey.setPressed(false);
        return true;
    }

    public static void enableSprint(MinecraftClient client, PlayerEntity player, boolean shouldRestore) {
        if (!shouldRestore || player == null) {
            return;
        }

        player.setSprinting(true);
        client.options.sprintKey.setPressed(true);
    }
}
