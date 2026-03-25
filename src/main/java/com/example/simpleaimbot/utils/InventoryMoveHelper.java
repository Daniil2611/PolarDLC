package com.example.simpleaimbot.utils;

import com.example.simpleaimbot.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

import java.util.LinkedList;
import java.util.Queue;

public class InventoryMoveHelper {
    public static final Queue<ClickSlotC2SPacket> pendingClicks = new LinkedList<>();
    private static int delayTicks = 0;
    public static boolean freezeMovement = false;
    private static boolean closeScheduled = false;

    public static void storePacket(ClickSlotC2SPacket packet) {
        pendingClicks.add(packet);
    }

    public static void scheduleClose() {
        if (!pendingClicks.isEmpty()) {
            closeScheduled = true;
        }
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (ConfigManager.getConfig().inventoryMoveEnabled) {
            // Убрана проверка на одиночную игру
            if (client == null) {
                resetState();
                return;
            }

            if (closeScheduled && delayTicks == 0 && !pendingClicks.isEmpty()) {
                delayTicks = 3;
                freezeMovement = true;
                closeScheduled = false;
            } else if (delayTicks > 0) {
                // Блокируем движение
                if (client.player != null) {
                    client.player.input.movementForward = 0;
                    client.player.input.movementSideways = 0;
                    client.player.setJumping(false);
                    client.player.setSneaking(false);
                    client.player.setSprinting(false);
                }
                // Отжимаем клавиши в настройках
                client.options.forwardKey.setPressed(false);
                client.options.backKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                client.options.sprintKey.setPressed(false);
                client.options.sneakKey.setPressed(false);

                delayTicks--;
                if (delayTicks == 0) {
                    // Отправляем накопленные клики
                    while (!pendingClicks.isEmpty()) {
                        client.getNetworkHandler().getConnection().send(pendingClicks.poll());
                    }
                    // Отправляем пакет закрытия инвентаря
                    client.getNetworkHandler().getConnection().send(new CloseHandledScreenC2SPacket(0));
                    freezeMovement = false;
                }
            } else {
                freezeMovement = false;
                closeScheduled = false;
            }
        } else {
            resetState();
        }
    }

    private static void resetState() {
        pendingClicks.clear();
        delayTicks = 0;
        freezeMovement = false;
        closeScheduled = false;
    }
}