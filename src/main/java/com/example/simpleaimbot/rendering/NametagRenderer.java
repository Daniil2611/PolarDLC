package com.example.simpleaimbot.rendering;

import com.example.simpleaimbot.SimpleAimbotMod;
import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class NametagRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NametagRenderer.class);

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            ModConfig config = ConfigManager.getConfig();

            if (!config.playerEspEnabled) return;

            PlayerEntity player = client.player;
            if (player == null || client.world == null) return;

            // Получаем сохранённые из мира матрицы и позицию камеры
            Matrix4f projMatrix = SimpleAimbotMod.getLastProjectionMatrix();
            Matrix4f viewMatrix = SimpleAimbotMod.getLastViewMatrix();
            Vec3d cameraPos = SimpleAimbotMod.getLastCameraPos();

            if (projMatrix == null || viewMatrix == null || cameraPos == null) {
                LOGGER.debug("Матрицы ещё не готовы");
                return;
            }

            List<PlayerEntity> targets = new ArrayList<>();
            for (PlayerEntity target : client.world.getPlayers()) {
                if (target == player) continue;
                if (player.distanceTo(target) > 75) continue;
                targets.add(target);
            }

            if (targets.isEmpty()) return;

            TextRenderer textRenderer = client.textRenderer;

            // Создаём копии матриц и перемножаем (проекция * вид)
            Matrix4f projCopy = new Matrix4f(projMatrix);
            Matrix4f viewCopy = new Matrix4f(viewMatrix);
            Matrix4f modelViewMatrix = projCopy.mul(viewCopy);

            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            for (PlayerEntity target : targets) {
                Vec3d targetPos = target.getPos().add(0, target.getHeight() + 0.5, 0);
                Vector4f clipSpace = worldToScreen(targetPos, cameraPos, modelViewMatrix);

                if (clipSpace == null) continue;

                float invW = 1.0f / clipSpace.w;
                float ndcX = clipSpace.x * invW;
                float ndcY = clipSpace.y * invW;

                int screenX = (int) ((ndcX + 1.0f) / 2.0f * screenWidth);
                int screenY = (int) ((1.0f - ndcY) / 2.0f * screenHeight);

                if (screenX < 0 || screenX > screenWidth || screenY < 0 || screenY > screenHeight) continue;

                Text nameText = Text.literal(target.getName().getString());
                int textWidth = textRenderer.getWidth(nameText);
                context.drawTextWithShadow(textRenderer, nameText, screenX - textWidth / 2, screenY - 10, 0xFFFFFFFF);
            }
        } catch (Exception e) {
            LOGGER.error("Ошибка при рендеринге ников: {}", e.getMessage());
        }
    }

    private static Vector4f worldToScreen(Vec3d worldPos, Vec3d cameraPos, Matrix4f modelViewMatrix) {
        Vector4f vec = new Vector4f(
                (float) (worldPos.x - cameraPos.x),
                (float) (worldPos.y - cameraPos.y),
                (float) (worldPos.z - cameraPos.z),
                1.0f
        );
        vec.mul(modelViewMatrix);
        if (vec.w <= 0) return null;
        return vec;
    }
}