package com.example.simpleaimbot;

import com.example.simpleaimbot.client.KeybindController;
import com.example.simpleaimbot.client.MovementController;
import com.example.simpleaimbot.client.RenderStateTracker;
import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import com.example.simpleaimbot.freelook.freelook.FreeLookMod;
import com.example.simpleaimbot.modules.KillAura;
import com.example.simpleaimbot.modules.Triggerbot;
import com.example.simpleaimbot.rendering.ArrayListRenderer;
import com.example.simpleaimbot.rendering.AttackLinesRenderer;
import com.example.simpleaimbot.rendering.ESPRenderer;
import com.example.simpleaimbot.rendering.JumpCircleRenderer;
import com.example.simpleaimbot.rendering.NametagRenderer;
import com.example.simpleaimbot.rendering.TargetESPRenderer;
import com.example.simpleaimbot.rendering.TargetHudRenderer;
import com.example.simpleaimbot.rendering.WatermarkRenderer;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.session.Session;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class SimpleAimbotMod implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleAimbotMod.class);

    private static long lastAttackTime;

    private final KeybindController keybindController = new KeybindController();
    private final MovementController movementController = new MovementController();

    private KeyBinding openSettingsKey;

    public static Matrix4f getLastProjectionMatrix() {
        return RenderStateTracker.getLastProjectionMatrix();
    }

    public static Matrix4f getLastViewMatrix() {
        return RenderStateTracker.getLastViewMatrix();
    }

    public static Vec3d getLastCameraPos() {
        return RenderStateTracker.getLastCameraPos();
    }

    public static void updateLastAttackTime() {
        lastAttackTime = System.currentTimeMillis();
    }

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        registerKeybinds();
        registerCommands();
        registerTickHandlers();
        registerRenderHandlers();
    }

    private void registerKeybinds() {
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.polardlc.settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.polardlc"
        ));
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("nick")
                        .then(ClientCommandManager.argument("newname", StringArgumentType.word())
                                .executes(context -> {
                                    setNickname(StringArgumentType.getString(context, "newname"));
                                    return 1;
                                })))
        );
    }

    private void registerTickHandlers() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                return;
            }

            JumpCircleRenderer.tick(client);
            keybindController.handle(client, openSettingsKey);
            FreeLookMod.tick(client);

            ModConfig config = ConfigManager.getConfig();
            if (config.killauraEnabled) {
                KillAura.tick(client);
            }
            if (config.triggerbotEnabled) {
                Triggerbot.tick(client);
            }

            long lastCombatAttackTime = Math.max(KillAura.getLastAttackTime(), Triggerbot.getLastAttackTime());
            long lastSprintResetTime = Math.max(KillAura.getLastSprintResetTime(), Triggerbot.getLastSprintResetTime());
            movementController.tick(client, config, lastCombatAttackTime, lastSprintResetTime, LOGGER);
            handleAutoFreeLook(client, config);
        });
    }

    private void registerRenderHandlers() {
        WorldRenderEvents.LAST.register(this::renderWorld);
        HudRenderCallback.EVENT.register(TargetHudRenderer::render);
        HudRenderCallback.EVENT.register(NametagRenderer::render);
        HudRenderCallback.EVENT.register(WatermarkRenderer::render);
        HudRenderCallback.EVENT.register((context, tickCounter) -> ArrayListRenderer.render(context));
    }

    private void handleAutoFreeLook(MinecraftClient client, ModConfig config) {
        boolean hasTarget = config.killauraEnabled && KillAura.getTarget() != null;
        boolean hasTargetInRange = config.killauraEnabled && KillAura.hasTargetInRange();

        if ((hasTarget || hasTargetInRange) && !FreeLookMod.isFreeLooking) {
            FreeLookMod.startAutoFreeLooking(client);
        } else if (!hasTarget && !hasTargetInRange && FreeLookMod.isFreeLooking && !FreeLookMod.isManualFreeLooking) {
            FreeLookMod.stopAutoFreeLooking(client);
        }
    }

    private void renderWorld(WorldRenderContext context) {
        RenderStateTracker.capture(context);

        ModConfig config = ConfigManager.getConfig();
        JumpCircleRenderer.render(context);
        if (config.playerEspEnabled) {
            ESPRenderer.renderPlayerESP(context);
        }
        if (config.targetEspEnabled) {
            Entity targetForEsp = KillAura.getTarget();
            if (targetForEsp != null && targetForEsp.isAlive()) {
                TargetESPRenderer.render(context, targetForEsp);
            }
        }
        AttackLinesRenderer.render(context);
    }

    private void setNickname(String newNickname) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        try {
            Field sessionField = MinecraftClient.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + newNickname).getBytes(StandardCharsets.UTF_8));
            Session newSession = new Session(newNickname, offlineUuid, "null", Optional.empty(), Optional.empty(), Session.AccountType.MSA);
            sessionField.set(client, newSession);

            if (client.player != null) {
                client.player.sendMessage(Text.literal("Nickname changed to " + newNickname), false);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to change nickname", e);
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Failed to change nickname"), false);
            }
        }
    }
}
