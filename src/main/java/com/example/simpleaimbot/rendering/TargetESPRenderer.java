package com.example.simpleaimbot.rendering;

import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import com.example.simpleaimbot.utils.render.TextureStorage;
import com.mojang.blaze3d.platform.GlStateManager;
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
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class TargetESPRenderer {
    private static final long INIT_TIME = System.currentTimeMillis();
    private static final int THUNDER_SPIRIT_LENGTH = 14;
    private static final int THUNDER_SPIRIT_FACTOR = 8;
    private static final float THUNDER_SPIRIT_SHAKING = 1.8f;
    private static final float THUNDER_SPIRIT_AMPLITUDE = 3.0f;

    public static void render(WorldRenderContext context, Entity target) {
        ModConfig config = ConfigManager.getConfig();
        if (!config.targetEspEnabled || target == null || !target.isAlive()) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }

        float tickDelta = context.tickCounter().getTickDelta(true);
        Vec3d cameraPos = context.camera().getPos();
        double x = MathHelper.lerp(tickDelta, target.prevX, target.getX()) - cameraPos.x;
        double y = MathHelper.lerp(tickDelta, target.prevY, target.getY()) - cameraPos.y;
        double z = MathHelper.lerp(tickDelta, target.prevZ, target.getZ()) - cameraPos.z;
        float height = target.getHeight();
        float radius = Math.max(0.42f, target.getWidth() * 0.75f);
        float time = (System.currentTimeMillis() - INIT_TIME) / 1000.0f;

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        if (config.targetEspMode == ModConfig.TargetEspMode.SPIRITS) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            drawSpirits(context, target, tickDelta, config.targetEspColor);
        } else {
            matrices.push();
            matrices.translate(x, y, z);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            switch (config.targetEspMode) {
                case RING_STACK -> drawRingStack(matrices.peek().getPositionMatrix(), radius, height, config.targetEspColor, time);
                case CYLINDER -> drawCylinder(matrices.peek().getPositionMatrix(), radius, height, config.targetEspColor, time);
                case SPIRAL -> drawSpiral(matrices.peek().getPositionMatrix(), radius, height, config.targetEspColor, time);
                case SPIRITS -> { }
            }
            matrices.pop();
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawSpiral(Matrix4f matrix, float radius, float height, int baseColor, float time) {
        float animatedRadius = radius + 0.02f * (float) Math.sin(time * 1.8f);
        int outerColor = withAlpha(baseColor, 122);
        int innerColor = withAlpha(mixColor(baseColor, 0xFFFFFFFF, 0.18f), 34);
        drawCylinderShell(matrix, animatedRadius * 0.72f, height, withAlpha(baseColor, 6), withAlpha(baseColor, 28), 64);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 140; i++) {
            float progress = i / 140.0f;
            double angle = time * 2.3 + progress * Math.PI * 3.9;
            float y = height * (0.08f + progress * 0.84f);
            float localRadius = animatedRadius + 0.02f * (float) Math.sin(time * 1.5f + progress * 6.0f);
            float x = (float) Math.cos(angle) * localRadius;
            float z = (float) Math.sin(angle) * localRadius;
            int localOuter = withAlpha(outerColor, Math.round(34 + 88 * (1.0f - Math.abs(progress - 0.5f) * 1.5f)));
            int localInner = withAlpha(innerColor, Math.round(10 + 28 * (1.0f - Math.abs(progress - 0.5f) * 1.35f)));
            buffer.vertex(matrix, x * 1.02f, y, z * 1.02f).color(localOuter);
            buffer.vertex(matrix, x * 0.93f, y + 0.03f, z * 0.93f).color(localInner);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        drawRingBand(matrix, Math.max(0.04f, animatedRadius - 0.05f), animatedRadius + 0.03f, 0.06f, withAlpha(baseColor, 88), withAlpha(baseColor, 0), 72);
        drawRingBand(matrix, Math.max(0.04f, animatedRadius - 0.05f), animatedRadius + 0.03f, height, withAlpha(mixColor(baseColor, 0xFFFFFFFF, 0.12f), 118), withAlpha(baseColor, 0), 72);
    }

    private static void drawRingStack(Matrix4f matrix, float radius, float height, int baseColor, float time) {
        drawCylinderShell(matrix, radius * 0.88f, height, withAlpha(baseColor, 4), withAlpha(baseColor, 18), 64);

        for (int i = 0; i < 3; i++) {
            float layer = i / 2.0f;
            float y = height * (0.18f + 0.28f * i) + (float) Math.sin(time * 1.4f + i * 0.8f) * 0.03f;
            float localRadius = radius + 0.015f * i + (float) Math.sin(time * 1.9f + i) * 0.02f;
            int bright = withAlpha(mixColor(baseColor, 0xFFFFFFFF, 0.08f + layer * 0.10f), Math.round(94 + 28 * layer));
            drawRingBand(
                    matrix,
                    Math.max(0.04f, localRadius - 0.04f),
                    localRadius + 0.03f,
                    y,
                    bright,
                    withAlpha(baseColor, 0),
                    72
            );
        }
    }

    private static void drawCylinder(Matrix4f matrix, float radius, float height, int baseColor, float time) {
        float animatedRadius = radius + 0.02f * (float) Math.sin(time * 1.6f);
        drawCylinderShell(matrix, animatedRadius, height, withAlpha(baseColor, 14), withAlpha(baseColor, 72), 72);
        drawRingBand(matrix, Math.max(0.03f, animatedRadius - 0.05f), animatedRadius + 0.03f, 0.04f, withAlpha(baseColor, 94), withAlpha(baseColor, 0), 72);
        drawRingBand(matrix, Math.max(0.03f, animatedRadius - 0.05f), animatedRadius + 0.03f, height, withAlpha(mixColor(baseColor, 0xFFFFFFFF, 0.12f), 132), withAlpha(baseColor, 0), 72);

        float scan = height * (0.16f + 0.68f * ((float) Math.sin(time * 1.4f) * 0.5f + 0.5f));
        drawRingBand(matrix, Math.max(0.03f, animatedRadius - 0.07f), animatedRadius + 0.06f, scan, withAlpha(baseColor, 86), withAlpha(baseColor, 0), 72);
    }

    private static void drawCylinderShell(Matrix4f matrix, float radius, float height, int bottomColor, int topColor, int segments) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            buffer.vertex(matrix, x, 0.02f, z).color(bottomColor);
            buffer.vertex(matrix, x, height, z).color(topColor);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void drawVerticalBands(Matrix4f matrix, float radius, float height, float time, int baseColor, int count) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float tau = (float) (Math.PI * 2.0);
        for (int band = 0; band < count; band++) {
            float angle = time + band * (tau / count);
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            float tangentX = -(float) Math.sin(angle) * 0.03f;
            float tangentZ = (float) Math.cos(angle) * 0.03f;
            int low = withAlpha(baseColor, 22);
            int high = withAlpha(mixColor(baseColor, 0xFFFFFFFF, 0.12f), 118);
            buffer.vertex(matrix, x - tangentX, 0.02f, z - tangentZ).color(low);
            buffer.vertex(matrix, x + tangentX, 0.02f, z + tangentZ).color(low);
            buffer.vertex(matrix, x + tangentX * 0.55f, height, z + tangentZ * 0.55f).color(high);
            buffer.vertex(matrix, x - tangentX * 0.55f, height, z - tangentZ * 0.55f).color(high);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void drawOrbitAccents(Matrix4f matrix, float radius, float y, int count, float size, int color, float phase) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        for (int i = 0; i < count; i++) {
            float angle = phase + i * ((float) (Math.PI * 2.0) / count);
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            drawSpiritOrb(matrix, x, y, z, size, color);
        }
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
    }

    // Ported from ThunderHack Recode's Render3DEngine.renderGhosts (GPL-3.0) to match ThunderHackV2 target ESP.
    private static void drawSpirits(WorldRenderContext context, Entity target, float tickDelta, int baseColor) {
        MinecraftClient client = MinecraftClient.getInstance();
        MatrixStack matrices = context.matrixStack();
        if (client.player == null || matrices == null) {
            return;
        }

        Vec3d cameraPos = context.camera().getPos();
        double targetX = MathHelper.lerp(tickDelta, target.prevX, target.getX()) - cameraPos.x;
        double targetY = MathHelper.lerp(tickDelta, target.prevY, target.getY()) - cameraPos.y;
        double targetZ = MathHelper.lerp(tickDelta, target.prevZ, target.getZ()) - cameraPos.z;
        float interpolatedAge = (target.age - 1) + tickDelta;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, TextureStorage.SPIRIT_GHOST);

        boolean canSee = client.player.canSee(target);
        if (canSee) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int arm = 0; arm < 3; arm++) {
            for (int index = 0; index <= THUNDER_SPIRIT_LENGTH; index++) {
                double radians = Math.toRadians((((index / 1.5f) + interpolatedAge) * THUNDER_SPIRIT_FACTOR + (arm * 120)) % (THUNDER_SPIRIT_FACTOR * 360));
                double wave = Math.sin(Math.toRadians(interpolatedAge * 2.5f + index * (arm + 1)) * THUNDER_SPIRIT_AMPLITUDE) / THUNDER_SPIRIT_SHAKING;
                float offset = index / (float) THUNDER_SPIRIT_LENGTH;
                matrices.push();
                matrices.translate(
                        targetX + Math.cos(radians) * target.getWidth(),
                        targetY + 1.0 + wave,
                        targetZ + Math.sin(radians) * target.getWidth()
                );
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-context.camera().getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(context.camera().getPitch()));

                Matrix4f matrix = matrices.peek().getPositionMatrix();
                int color = applyOpacity(resolveSpiritParticleColor(baseColor, arm, offset), offset);
                float scale = Math.max(0.24f * offset, 0.2f);

                buffer.vertex(matrix, -scale, scale, 0.0f).texture(0.0f, 1.0f).color(color);
                buffer.vertex(matrix, scale, scale, 0.0f).texture(1.0f, 1.0f).color(color);
                buffer.vertex(matrix, scale, -scale, 0.0f).texture(1.0f, 0.0f).color(color);
                buffer.vertex(matrix, -scale, -scale, 0.0f).texture(0.0f, 0.0f).color(color);
                matrices.pop();
            }
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        if (canSee) {
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
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

    private static void drawSpiritWisp(Matrix4f matrix, float centerX, float centerY, float centerZ,
                                       float width, float height, int centerColor, int edgeColor) {
        RenderSystem.setShaderTexture(0, TextureStorage.SPIRIT_WISP);
        drawTexturedCross(matrix, centerX, centerY, centerZ, width, height, centerColor, edgeColor);
    }

    private static void drawSpiritOrb(Matrix4f matrix, float centerX, float centerY, float centerZ, float size, int color) {
        RenderSystem.setShaderTexture(0, TextureStorage.SPIRIT_WISP);
        drawTexturedCross(matrix, centerX, centerY, centerZ, size, size, color, withAlpha(color, 0));
    }

    private static void drawSpiritFlame(Matrix4f matrix, float centerX, float centerY, float centerZ,
                                        float width, float height, int color) {
        RenderSystem.setShaderTexture(0, TextureStorage.SPIRIT_FLAME);
        drawTexturedCross(matrix, centerX, centerY, centerZ, width, height, color, withAlpha(color, 0));
    }

    private static void drawTexturedCross(Matrix4f matrix, float centerX, float centerY, float centerZ,
                                          float width, float height, int centerColor, int edgeColor) {
        drawTexturedPlaneX(matrix, centerX, centerY, centerZ, width, height, centerColor, edgeColor);
        drawTexturedPlaneZ(matrix, centerX, centerY, centerZ, width, height, centerColor, edgeColor);
    }

    private static void drawTexturedPlaneX(Matrix4f matrix, float centerX, float centerY, float centerZ,
                                           float width, float height, int centerColor, int edgeColor) {
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, centerX - halfWidth, centerY - halfHeight, centerZ).texture(0.0f, 1.0f).color(centerColor);
        buffer.vertex(matrix, centerX - halfWidth, centerY + halfHeight, centerZ).texture(0.0f, 0.0f).color(centerColor);
        buffer.vertex(matrix, centerX + halfWidth, centerY + halfHeight, centerZ).texture(1.0f, 0.0f).color(centerColor);
        buffer.vertex(matrix, centerX + halfWidth, centerY - halfHeight, centerZ).texture(1.0f, 1.0f).color(centerColor);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void drawTexturedPlaneZ(Matrix4f matrix, float centerX, float centerY, float centerZ,
                                           float width, float height, int centerColor, int edgeColor) {
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, centerX, centerY - halfHeight, centerZ - halfWidth).texture(0.0f, 1.0f).color(centerColor);
        buffer.vertex(matrix, centerX, centerY + halfHeight, centerZ - halfWidth).texture(0.0f, 0.0f).color(centerColor);
        buffer.vertex(matrix, centerX, centerY + halfHeight, centerZ + halfWidth).texture(1.0f, 0.0f).color(centerColor);
        buffer.vertex(matrix, centerX, centerY - halfHeight, centerZ + halfWidth).texture(1.0f, 1.0f).color(centerColor);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static int applyOpacity(int color, float opacity) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * MathHelper.clamp(opacity, 0.0f, 1.0f));
        return withAlpha(color, alpha);
    }

    private static int resolveSpiritBaseColor(int baseColor) {
        int opaque = 0xFF000000 | (baseColor & 0x00FFFFFF);
        return isNearWhite(opaque) ? 0xFF7A34FF : opaque;
    }

    private static int resolveSpiritParticleColor(int baseColor, int arm, float offset) {
        int root = resolveSpiritBaseColor(baseColor);
        int armColor = switch (arm) {
            case 1 -> mixColor(root, 0xFFFF63F6, 0.18f);
            case 2 -> mixColor(root, 0xFF5D2FFF, 0.25f);
            default -> mixColor(root, 0xFF8E48FF, 0.12f);
        };
        return mixColor(armColor, 0xFFFFFFFF, 0.10f + offset * 0.16f);
    }

    private static boolean isNearWhite(int color) {
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        return r > 235 && g > 235 && b > 235;
    }

    private static int mixColor(int first, int second, float progress) {
        progress = MathHelper.clamp(progress, 0.0f, 1.0f);
        int r1 = (first >>> 16) & 0xFF;
        int g1 = (first >>> 8) & 0xFF;
        int b1 = first & 0xFF;
        int r2 = (second >>> 16) & 0xFF;
        int g2 = (second >>> 8) & 0xFF;
        int b2 = second & 0xFF;

        int r = Math.round(r1 + (r2 - r1) * progress);
        int g = Math.round(g1 + (g2 - g1) * progress);
        int b = Math.round(b1 + (b2 - b1) * progress);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
