package com.example.simpleaimbot.client;

import com.example.simpleaimbot.config.ModConfig;
import com.example.simpleaimbot.utils.InventoryMoveHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.slf4j.Logger;

import java.util.Random;

public final class MovementController {
    private static final Random RANDOM = new Random();
    private int sprintDelay;
    private int totemDelayTicks = 3;

    public void tick(MinecraftClient client, ModConfig config, long lastCombatAttackTime, long lastSprintResetTime, Logger logger) {
        if (client.player == null) {
            return;
        }

        handleFullbright(client, config);
        handleMovementModules(client, config);
        handleInventoryMove(client, config);
        InventoryMoveHelper.tick();
        handleAutoTotem(client, config, logger);
    }

    private void handleFullbright(MinecraftClient client, ModConfig config) {
        if (client.player == null) {
            return;
        }

        if (config.fullbrightEnabled) {
            client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 9999 * 20, 0, false, false, true));
        } else {
            client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    private void handleMovementModules(MinecraftClient client, ModConfig config) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        if (config.flyEnabled && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().allowFlying = true;
        } else if (!config.flyEnabled && !player.isCreative() && !player.isSpectator() && !player.getAbilities().flying) {
            player.getAbilities().allowFlying = false;
        }

        if (!config.autoSprintEnabled) {
            sprintDelay = 0;
            return;
        }

        boolean movingForward = client.options.forwardKey.isPressed();
        if (movingForward
                && !player.isSprinting()
                && !player.isSneaking()
                && player.getHungerManager().getFoodLevel() > 6) {
            if (sprintDelay == 0) {
                player.setSprinting(true);
                sprintDelay = RANDOM.nextInt(1, 4);
            }
        } else if (!movingForward && player.isSprinting()) {
            player.setSprinting(false);
            sprintDelay = 0;
        }

        if (sprintDelay > 0) {
            sprintDelay--;
        }
    }

    private void handleInventoryMove(MinecraftClient client, ModConfig config) {
        // Убрана проверка на одиночную игру
        if (!config.inventoryMoveEnabled
                || client.currentScreen == null
                || client.currentScreen instanceof ChatScreen
                || client.player == null) {
            return;
        }

        long windowHandle = client.getWindow().getHandle();
        int forwardCode = client.options.forwardKey.getDefaultKey().getCode();
        int backCode = client.options.backKey.getDefaultKey().getCode();
        int leftCode = client.options.leftKey.getDefaultKey().getCode();
        int rightCode = client.options.rightKey.getDefaultKey().getCode();
        int jumpCode = client.options.jumpKey.getDefaultKey().getCode();
        int sprintCode = client.options.sprintKey.getDefaultKey().getCode();

        client.options.forwardKey.setPressed(InputUtil.isKeyPressed(windowHandle, forwardCode));
        client.options.backKey.setPressed(InputUtil.isKeyPressed(windowHandle, backCode));
        client.options.leftKey.setPressed(InputUtil.isKeyPressed(windowHandle, leftCode));
        client.options.rightKey.setPressed(InputUtil.isKeyPressed(windowHandle, rightCode));
        client.options.jumpKey.setPressed(InputUtil.isKeyPressed(windowHandle, jumpCode));
        client.options.sprintKey.setPressed(InputUtil.isKeyPressed(windowHandle, sprintCode));

        int sneakCode = client.options.sneakKey.getDefaultKey().getCode();
        client.player.setSneaking(InputUtil.isKeyPressed(windowHandle, sneakCode));
    }

    private void handleAutoTotem(MinecraftClient client, ModConfig config, Logger logger) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        if (!config.autoTotemEnabled) {
            totemDelayTicks = 0;
            return;
        }

        if (totemDelayTicks > 0) {
            totemDelayTicks--;
            return;
        }

        if (player.getHealth() > config.autoTotemThreshold) {
            return;
        }

        ItemStack offhand = player.getOffHandStack();
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING || client.interactionManager == null) {
            return;
        }

        int slot = findTotemInInventory(player, logger);
        if (slot == -1) {
            return;
        }

        boolean wasScreenOpen = client.currentScreen != null;
        if (!wasScreenOpen) {
            client.setScreen(new InventoryScreen(player));
        }

        int screenSlot = slot < 9 ? slot + 36 : slot;
        int offhandScreenSlot = 45;
        int syncId = player.currentScreenHandler.syncId;
        client.interactionManager.clickSlot(syncId, screenSlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(syncId, offhandScreenSlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(syncId, screenSlot, 0, SlotActionType.PICKUP, player);
        totemDelayTicks = config.autoTotemDelay;
        logger.info("[AutoTotem] Swapped totem from slot {} to offhand", slot);

        if (!wasScreenOpen) {
            client.setScreen(null);
        }
    }

    private static int findTotemInInventory(ClientPlayerEntity player, Logger logger) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING && stack.getEnchantments().isEmpty()) {
                logger.info("Found unenchanted totem at slot {}", i);
                return i;
            }
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                logger.info("Found enchanted totem at slot {}", i);
                return i;
            }
        }

        logger.info("No totem found in inventory.");
        return -1;
    }
}