package com.example.simpleaimbot.client;

import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import com.example.simpleaimbot.freelook.freelook.FreeLookMod;
import com.example.simpleaimbot.gui.imgui.ImGuiSettingsScreen;
import com.example.simpleaimbot.modules.KillAura;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public final class KeybindController {
    private boolean wasTriggerKeyPressed;
    private boolean wasKillauraKeyPressed;
    private boolean wasFreeLookPressed;

    public void handle(MinecraftClient client, KeyBinding openSettingsKey) {
        while (openSettingsKey.wasPressed()) {
            client.setScreen(new ImGuiSettingsScreen());
        }

        if (client.currentScreen != null) {
            return;
        }

        long windowHandle = client.getWindow().getHandle();
        ModConfig config = ConfigManager.getConfig();
        handleTriggerbotToggle(client, config, windowHandle);
        handleKillAuraToggle(client, config, windowHandle);
        handleFreeLookToggle(client, config, windowHandle);
    }

    private void handleTriggerbotToggle(MinecraftClient client, ModConfig config, long windowHandle) {
        if (config.triggerbotKeyCode == -1) {
            wasTriggerKeyPressed = false;
            return;
        }

        boolean triggerPressed = InputUtil.isKeyPressed(windowHandle, config.triggerbotKeyCode);
        if (triggerPressed && !wasTriggerKeyPressed) {
            boolean wasEnabled = config.triggerbotEnabled;
            if (config.killauraEnabled) {
                config.killauraEnabled = false;
            }
            config.triggerbotEnabled = !wasEnabled;
            ConfigManager.save();
            sendStatus(client, "Triggerbot", config.triggerbotEnabled);
        }
        wasTriggerKeyPressed = triggerPressed;
    }

    private void handleKillAuraToggle(MinecraftClient client, ModConfig config, long windowHandle) {
        if (config.killauraKeyCode == -1) {
            wasKillauraKeyPressed = false;
            return;
        }

        boolean killAuraPressed = InputUtil.isKeyPressed(windowHandle, config.killauraKeyCode);
        if (killAuraPressed && !wasKillauraKeyPressed) {
            boolean wasEnabled = config.killauraEnabled;
            if (!wasEnabled) {
                config.triggerbotEnabled = false;
            }
            config.killauraEnabled = !wasEnabled;
            ConfigManager.save();
            if (!config.killauraEnabled) {
                KillAura.reset();
            }
            sendStatus(client, "KillAura", config.killauraEnabled);
        }
        wasKillauraKeyPressed = killAuraPressed;
    }

    private void handleFreeLookToggle(MinecraftClient client, ModConfig config, long windowHandle) {
        boolean freeLookPressed = InputUtil.isKeyPressed(windowHandle, config.freeLookKeyCode);
        if (config.freeLookToggle) {
            if (freeLookPressed && !wasFreeLookPressed) {
                if (FreeLookMod.isManualFreeLooking) {
                    FreeLookMod.stopManualFreeLooking(client);
                } else {
                    FreeLookMod.startManualFreeLooking(client);
                }
            }
        } else {
            if (freeLookPressed && !FreeLookMod.isManualFreeLooking) {
                FreeLookMod.startManualFreeLooking(client);
            } else if (!freeLookPressed && FreeLookMod.isManualFreeLooking) {
                FreeLookMod.stopManualFreeLooking(client);
            }
        }
        wasFreeLookPressed = freeLookPressed;
    }

    private void sendStatus(MinecraftClient client, String moduleName, boolean enabled) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(moduleName + " " + (enabled ? "enabled" : "disabled")), true);
        }
    }
}
