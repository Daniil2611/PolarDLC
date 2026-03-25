package com.example.simpleaimbot.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class RenderStateTracker {
    private static Matrix4f lastProjectionMatrix;
    private static Matrix4f lastViewMatrix;
    private static Vec3d lastCameraPos;

    private RenderStateTracker() {
    }

    public static void capture(WorldRenderContext context) {
        lastProjectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());
        lastViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());
        lastCameraPos = context.camera().getPos();
    }

    public static Matrix4f getLastProjectionMatrix() {
        return lastProjectionMatrix;
    }

    public static Matrix4f getLastViewMatrix() {
        return lastViewMatrix;
    }

    public static Vec3d getLastCameraPos() {
        return lastCameraPos;
    }
}
