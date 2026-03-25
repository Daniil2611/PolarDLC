package com.example.simpleaimbot.rendering;

import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import com.example.simpleaimbot.utils.TargetUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

public class AttackLinesRenderer {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final int RED = 0xFFFF0000;
    private static final int GREEN = 0xFF00FF00;

    @SuppressWarnings({"ConstantConditions", "deprecation"})
    public static void render(WorldRenderContext context) {
        ModConfig config = ConfigManager.getConfig();
        if (!config.attackLinesEnabled) return;

        PlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        double radius = config.aimbotRange;
        float tickDelta = context.tickCounter().getTickDelta(true);

        double centerX = MathHelper.lerp(tickDelta, player.prevX, player.getX());
        double centerY = MathHelper.lerp(tickDelta, player.prevY, player.getY());
        double centerZ = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());

        boolean enemyInRange = isEnemyInRange(player, radius, centerX, centerY, centerZ, tickDelta);
        int color = enemyInRange ? GREEN : RED;

        renderCircle(context, centerX, centerY, centerZ, radius, color);
    }

    @SuppressWarnings("ConstantConditions")
    private static boolean isEnemyInRange(PlayerEntity player, double radius, double centerX, double centerY, double centerZ, float tickDelta) {
        if (client.world == null) return false;

        Box searchBox = new Box(centerX - radius, centerY - radius, centerZ - radius,
                centerX + radius, centerY + radius, centerZ + radius);
        List<Entity> entities = client.world.getOtherEntities(player, searchBox,
                entity -> TargetUtils.isValidTarget(player, entity));

        for (Entity entity : entities) {
            double ex = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
            double ey = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
            double ez = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());

            Box entityBox = entity.getBoundingBox().offset(ex - entity.getX(), ey - entity.getY(), ez - entity.getZ());

            double closestX = MathHelper.clamp(centerX, entityBox.minX, entityBox.maxX);
            double closestZ = MathHelper.clamp(centerZ, entityBox.minZ, entityBox.maxZ);

            double dx = closestX - centerX;
            double dz = closestZ - centerZ;
            double distSq = dx * dx + dz * dz;

            if (distSq <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    private static void renderCircle(WorldRenderContext context, double x, double y, double z, double radius, int color) {
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        Vec3d cameraPos = context.camera().getPos();
        matrices.push();
        matrices.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(3.0f);
        RenderSystem.disableDepthTest();

        Tessellator tessellator = Tessellator.getInstance();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int segments = 128;
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double dx = radius * Math.cos(angle);
            double dz = radius * Math.sin(angle);
            buffer.vertex(matrix, (float) dx, 0.0f, (float) dz).color(color);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(1.0f);
        RenderSystem.disableBlend();
        matrices.pop();
    }
}