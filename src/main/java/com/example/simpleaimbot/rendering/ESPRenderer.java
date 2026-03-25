package com.example.simpleaimbot.rendering;

import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class ESPRenderer {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    public static void renderPlayerESP(WorldRenderContext context) {
        ModConfig config = ConfigManager.getConfig();
        if (!config.playerEspEnabled || CLIENT.world == null || CLIENT.player == null) {
            return;
        }

        List<AbstractClientPlayerEntity> players = new ArrayList<>(CLIENT.world.getPlayers());
        float tickDelta = context.tickCounter().getTickDelta(true);
        float time = System.currentTimeMillis() / 1000.0f;

        for (AbstractClientPlayerEntity player : players) {
            if (player == CLIENT.player || !player.isAlive()) {
                continue;
            }

            double x = MathHelper.lerp(tickDelta, player.prevX, player.getX());
            double y = MathHelper.lerp(tickDelta, player.prevY, player.getY());
            double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());
            float radius = Math.max(0.32f, player.getWidth() * 0.72f);
            float height = player.getHeight();

            renderCapsule(context, x, y, z, radius, height, config.espColor, config.playerEspFilled, time + player.getId() * 0.21f);
        }
    }

    private static void renderCapsule(WorldRenderContext context, double x, double y, double z,
                                      float radius, float height, int color, boolean filled, float time) {
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }

        Vec3d cameraPos = context.camera().getPos();
        matrices.push();
        matrices.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        float pulse = 0.02f * (float) Math.sin(time * 3.0f);
        float outer = radius + pulse;
        float inner = Math.max(0.05f, outer - 0.08f);
        int topColor = withAlpha(color, 180);
        int bottomColor = withAlpha(color, 84);

        if (filled) {
            BufferBuilder shell = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= 56; i++) {
                double angle = Math.PI * 2 * i / 56.0;
                float px = (float) Math.cos(angle) * inner;
                float pz = (float) Math.sin(angle) * inner;
                shell.vertex(matrices.peek().getPositionMatrix(), px, 0.03f, pz).color(withAlpha(color, 22));
                shell.vertex(matrices.peek().getPositionMatrix(), px, height, pz).color(withAlpha(color, 54));
            }
            BufferRenderer.drawWithGlobalProgram(shell.end());
        }

        drawRingBand(matrices.peek().getPositionMatrix(), inner, outer, 0.05f, bottomColor, withAlpha(color, 0), 56);
        drawRingBand(matrices.peek().getPositionMatrix(), inner, outer, height, topColor, withAlpha(color, 0), 56);

        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI / 4 + i * Math.PI / 2;
            float px = (float) Math.cos(angle) * inner;
            float pz = (float) Math.sin(angle) * inner;
            lines.vertex(matrices.peek().getPositionMatrix(), px, 0.05f, pz).color(bottomColor);
            lines.vertex(matrices.peek().getPositionMatrix(), px, height, pz).color(topColor);
        }
        BufferRenderer.drawWithGlobalProgram(lines.end());

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        matrices.pop();
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
