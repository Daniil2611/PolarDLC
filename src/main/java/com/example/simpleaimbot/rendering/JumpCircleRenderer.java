package com.example.simpleaimbot.rendering;

import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class JumpCircleRenderer {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final List<Circle> CIRCLES = new ArrayList<>();
    private static final long CIRCLE_DURATION = 1000L;
    private static final int CIRCLE_SEGMENTS = 64;

    private static boolean wasOnGround = true;

    private static class Circle {
        final Vec3d center;
        final long startTime;
        final float radius;
        final int segments;
        final long duration;
        final int baseColor;
        final ModConfig.JumpCircleMode mode;

        Circle(Vec3d center, long startTime, float radius, int segments, long duration, int baseColor, ModConfig.JumpCircleMode mode) {
            this.center = center;
            this.startTime = startTime;
            this.radius = radius;
            this.segments = segments;
            this.duration = duration;
            this.baseColor = baseColor;
            this.mode = mode;
        }

        float progress() {
            return Math.min(1.0f, (System.currentTimeMillis() - startTime) / (float) duration);
        }

        boolean isExpired() {
            return System.currentTimeMillis() - startTime >= duration;
        }
    }

    public static void tick(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        boolean onGround = player.isOnGround();
        if (!onGround && wasOnGround && player.getVelocity().y > 0.1) {
            addJumpCircle(player.getPos());
        }
        wasOnGround = onGround;
    }

    private static void addJumpCircle(Vec3d center) {
        ModConfig config = ConfigManager.getConfig();
        if (!config.jumpCircleEnabled) {
            return;
        }

        CIRCLES.add(new Circle(
                center,
                System.currentTimeMillis(),
                config.jumpCircleRadius,
                CIRCLE_SEGMENTS,
                CIRCLE_DURATION,
                config.jumpCircleColor,
                config.jumpCircleMode
        ));
    }

    public static void render(WorldRenderContext context) {
        ModConfig config = ConfigManager.getConfig();
        if (!config.jumpCircleEnabled || CIRCLES.isEmpty()) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }

        Vec3d cameraPos = context.camera().getPos();
        CIRCLES.removeIf(Circle::isExpired);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        for (Circle circle : CIRCLES) {
            matrices.push();
            matrices.translate(circle.center.x - cameraPos.x, circle.center.y - cameraPos.y + 0.02, circle.center.z - cameraPos.z);

            float progress = circle.progress();
            float radius = circle.radius + progress * 0.65f;

            switch (circle.mode) {
                case RING -> drawRing(matrices.peek().getPositionMatrix(), circle, radius, progress);
                case WAVE -> drawWave(matrices.peek().getPositionMatrix(), circle, radius, progress);
                case DOUBLE_RING -> drawDoubleRing(matrices.peek().getPositionMatrix(), circle, radius, progress);
                case DISC -> drawDisc(matrices.peek().getPositionMatrix(), circle, radius, progress);
            }

            matrices.pop();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawDisc(Matrix4f matrix, Circle circle, float radius, float progress) {
        int centerColor = withAlpha(circle.baseColor, Math.round(120 * (1.0f - progress)));
        int edgeColor = withAlpha(circle.baseColor, Math.round(22 * (1.0f - progress)));

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < circle.segments; i++) {
            double angle1 = Math.PI * 2 * i / circle.segments;
            double angle2 = Math.PI * 2 * (i + 1) / circle.segments;
            buffer.vertex(matrix, 0, 0, 0).color(centerColor);
            buffer.vertex(matrix, radius * (float) Math.cos(angle1), 0, radius * (float) Math.sin(angle1)).color(edgeColor);
            buffer.vertex(matrix, radius * (float) Math.cos(angle2), 0, radius * (float) Math.sin(angle2)).color(edgeColor);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        drawRingBand(matrix, radius * 0.92f, radius, 0.0f, withAlpha(circle.baseColor, Math.round(180 * (1.0f - progress))), withAlpha(circle.baseColor, 0), circle.segments);
    }

    private static void drawRing(Matrix4f matrix, Circle circle, float radius, float progress) {
        float outer = radius + 0.08f + progress * 0.2f;
        float inner = Math.max(0.05f, outer - 0.12f);
        drawRingBand(matrix, inner, outer, 0.0f, withAlpha(circle.baseColor, Math.round(185 * (1.0f - progress))), withAlpha(circle.baseColor, 0), circle.segments);
    }

    private static void drawWave(Matrix4f matrix, Circle circle, float radius, float progress) {
        float outer = radius + 0.14f;
        float inner = Math.max(0.05f, outer - 0.14f);
        float amplitude = 0.12f * (1.0f - progress);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= circle.segments; i++) {
            double angle = Math.PI * 2 * i / circle.segments;
            float wave = (float) Math.sin(angle * 3.0 + progress * 10.0) * amplitude;
            float x = (float) Math.cos(angle);
            float z = (float) Math.sin(angle);
            int outerColor = withAlpha(circle.baseColor, Math.round(170 * (1.0f - progress)));
            int innerColor = withAlpha(circle.baseColor, Math.round(26 * (1.0f - progress)));
            buffer.vertex(matrix, x * outer, wave, z * outer).color(outerColor);
            buffer.vertex(matrix, x * inner, wave * 0.35f, z * inner).color(innerColor);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void drawDoubleRing(Matrix4f matrix, Circle circle, float radius, float progress) {
        drawRingBand(matrix, Math.max(0.05f, radius - 0.1f), radius + 0.02f, 0.0f,
                withAlpha(circle.baseColor, Math.round(160 * (1.0f - progress))), withAlpha(circle.baseColor, 0), circle.segments);
        drawRingBand(matrix, Math.max(0.05f, radius * 0.62f - 0.08f), radius * 0.62f + 0.02f, 0.02f,
                withAlpha(circle.baseColor, Math.round(105 * (1.0f - progress))), withAlpha(circle.baseColor, 0), circle.segments);
    }

    private static void drawRingBand(Matrix4f matrix, float innerRadius, float outerRadius, float yOffset,
                                     int outerColor, int innerColor, int segments) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            float x = (float) Math.cos(angle);
            float z = (float) Math.sin(angle);
            buffer.vertex(matrix, x * outerRadius, yOffset, z * outerRadius).color(outerColor);
            buffer.vertex(matrix, x * innerRadius, yOffset, z * innerRadius).color(innerColor);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }
}
