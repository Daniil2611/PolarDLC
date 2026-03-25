package com.example.simpleaimbot.freelook.freelook;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class FreeLookMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Freelook");
    public static boolean isFreeLooking = false;
    public static boolean isManualFreeLooking = false;
    private static Perspective lastPerspective;

    private static boolean interpolating = false;
    private static float startYaw, startPitch;
    private static float targetYaw, targetPitch;
    private static int interpolationTicks = 2;
    private static int currentInterpTick = 0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("FreeLook initialized");
    }

    public static void startManualFreeLooking(MinecraftClient client) {
        if (isManualFreeLooking) return;
        lastPerspective = client.options.getPerspective();
        if (lastPerspective == Perspective.FIRST_PERSON) {
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
        isManualFreeLooking = true;
        isFreeLooking = true;
    }

    public static boolean isAutoFreeLooking() {
        return isFreeLooking && !isManualFreeLooking;
    }

    public static void stopManualFreeLooking(MinecraftClient client) {
        if (!isManualFreeLooking) return;
        client.options.setPerspective(lastPerspective);
        isManualFreeLooking = false;
        isFreeLooking = false;
        // НЕ синхронизируем углы – камера остаётся в последнем положении
    }

    public static void startAutoFreeLooking(MinecraftClient client) {
        if (isManualFreeLooking) return;
        isFreeLooking = true;
    }

    public static void stopAutoFreeLooking(MinecraftClient client) {
        if (isManualFreeLooking) return;
        if (client.player != null && isFreeLooking) {
            startYaw = client.player.getYaw();
            startPitch = client.player.getPitch();
            if (client.player instanceof CameraOverriddenEntity cameraEntity) {
                targetYaw = cameraEntity.freelook$getCameraYaw();
                targetPitch = cameraEntity.freelook$getCameraPitch();
                interpolating = true;
                currentInterpTick = 0;
            }
        }
        isFreeLooking = false;
    }

    public static void tick(MinecraftClient client) {
        if (!interpolating) return;
        if (client.player == null) {
            interpolating = false;
            return;
        }
        currentInterpTick++;
        float progress = (float) currentInterpTick / interpolationTicks;
        if (progress >= 1.0f) {
            client.player.setYaw(targetYaw);
            client.player.setPitch(targetPitch);
            interpolating = false;
            return;
        }
        float newYaw = startYaw + (targetYaw - startYaw) * progress;
        float newPitch = startPitch + (targetPitch - startPitch) * progress;
        client.player.setYaw(newYaw);
        client.player.setPitch(MathHelper.clamp(newPitch, -90.0f, 90.0f));
    }
}
