package com.example.simpleaimbot.modules;

import com.example.simpleaimbot.client.SprintResetController;
import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import com.example.simpleaimbot.utils.RotationUtils;
import com.example.simpleaimbot.utils.TargetUtils;
import com.example.simpleaimbot.utils.animation.EaseOutCirc;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class KillAura {
    private static final Random RANDOM = new Random();
    private static Entity target = null;
    private static float currentSpeedYaw = 0;
    private static float currentSpeedPitch = 0;
    private static int postHitTicks = 0;
    private static float postHitYawOffset = 0;
    private static float postHitPitchOffset = 0;
    private static float currentAimSpeed = 0.5f;

    private static final float MAX_YAW_DELTA = 120.0f;
    private static final float MAX_PITCH_DELTA = 80.0f;
    private static final float MOUSE_ACCEL = 0.3f;
    private static final float MOUSE_FRICTION = 0.1f;
    private static final float POST_HIT_JITTER_STRENGTH = 5.0f;
    private static final float BASE_PREDICTION = 0.1f;
    private static final float MAX_PREDICTION = 0.2f;
    private static final float FULL_CHARGE_COOLDOWN = 0.92f;
    private static final float SNAP_PREPARE_COOLDOWN = 0.88f;

    private static ModConfig.TargetSort lastSort = null;

    private static long lastAttackTime = 0;
    private static long lastSprintResetTime = 0;

    // === LEGIT-режим: параметры ===
    private static final float LEGIT_FOV = 240.0f;

    // === ПОСТ-ХИТ КОЛЕБАНИЯ ===
    private static int postHitWobbleTicks = 0;
    private static final int WOBBLE_DURATION = 10;
    private static final float WOBBLE_YAW_AMPLITUDE = 20.0f;
    private static final float WOBBLE_PITCH_AMPLITUDE = 5.0f;
    private static final float WOBBLE_FREQ = 0.15f;
    private static long wobbleStartTime = 0;

    private static int reactionDelayTicks = 0;
    private static Entity pendingTarget = null;

    // === Микро-паузы (для всех режимов, но в FUNTIME_SNAP используются отдельно) ===
    private static final int MICRO_PAUSE_CHANCE = 5;

    // === Имитация ошибки (только для LEGIT/RAGE) ===
    private static int attackCounter = 0;
    private static int nextErrorAttack = 0;

    // === Отслеживание падения для критов ===
    private static boolean wasOnGroundLastTick = true;
    private static int fallTicks = 0;
    private static double fallStartY = 0;

    // === Переменные для FUNTIME_SNAP (круговое движение без следования) ===
    private static double circlePhase = 0.0;
    private static final double BASE_CIRCLE_SPEED = 0.2;
    private static final float CIRCLE_YAW_AMPLITUDE = 18.0f;
    private static final float CIRCLE_PITCH_AMPLITUDE = 9.0f;

    // Запомненные базовые углы (центр круга) – фиксируются при появлении цели
    private static float baseYaw = 0;
    private static float basePitch = 0;
    private static Entity lastTargetForBase = null;

    // Флаг, указывающий, что в этом тике нужно атаковать (устанавливается при выполнении условий)
    private static boolean shouldAttack = false;

    // === Переменные для плавного рывка ===
    private static boolean isSnapping = false;
    private static int snapTicks = 0;
    private static final int SNAP_DURATION = 2;       // рывок длится 2 тика
    private static float snapStartYaw = 0, snapStartPitch = 0;
    private static float snapTargetYaw = 0, snapTargetPitch = 0;

    // Траектории движений (круг и восьмёрка)
    private enum Trajectory { CIRCLE, FIGURE_EIGHT }
    private static Trajectory currentTrajectory = Trajectory.CIRCLE;
    private static int trajectoryChangeTimer = 0;

    // ===== Публичные методы =====
    public static long getLastAttackTime() { return lastAttackTime; }
    public static long getLastSprintResetTime() { return lastSprintResetTime; }
    public static Entity getTarget() { return target; }
    public static void reset() {
        target = null;
        currentSpeedYaw = 0;
        currentSpeedPitch = 0;
        postHitTicks = 0;
        postHitYawOffset = 0;
        postHitPitchOffset = 0;
        lastSort = null;
        lastAttackTime = 0;
        lastSprintResetTime = 0;
        pendingTarget = null;
        reactionDelayTicks = 0;
        postHitWobbleTicks = 0;
        attackCounter = 0;
        nextErrorAttack = 0;
        wasOnGroundLastTick = true;
        fallTicks = 0;
        fallStartY = 0;
        circlePhase = 0.0;
        lastTargetForBase = null;
        shouldAttack = false;
        isSnapping = false;
        snapTicks = 0;
        currentTrajectory = Trajectory.CIRCLE;
        trajectoryChangeTimer = 0;
    }

    public static boolean hasTargetInRange() {
        if (target == null) return false;
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;
        ModConfig config = ConfigManager.getConfig();
        // Запас 0.2 для стабильности на дальних дистанциях
        return getTargetDistance(player, target) <= getTrackRange(config);
    }

    public static void snapToTarget(PlayerEntity player) {
        if (target != null && player != null) {
            float[] rot = RotationUtils.getRotationsToEntity(player, target, BASE_PREDICTION, MAX_PREDICTION, RANDOM);
            player.setYaw(rot[0]);
            player.setPitch(MathHelper.clamp(rot[1], -90f, 90f));
        }
    }

    // ===== Основной тик =====
    public static void tick(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;
        if (client.world == null) return;

        ModConfig config = ConfigManager.getConfig();
        if (!config.killauraEnabled) return;

        ModConfig.RotationMode mode = config.rotationMode;

        boolean sortChanged = lastSort != config.targetSort;
        if (sortChanged) {
            target = null;
            lastSort = config.targetSort;
        }

        if (target != null) {
            double loseDistance = getLoseRange(config);
            if (!TargetUtils.isValidTarget(player, target) ||
                    getTargetDistance(player, target) > loseDistance) {
                target = null;
                isSnapping = false;
                shouldAttack = false;
            }
        }

        if (mode == ModConfig.RotationMode.LEGIT) {
            handleReactionDelay(client, player, config);
        } else {
            if (target == null) {
                findNewTarget(client, player, config);
            }
        }

        if (target == null) {
            lastTargetForBase = null;
            shouldAttack = false;
            isSnapping = false;
            return;
        }

        if (mode == ModConfig.RotationMode.LEGIT) {
            float fov = getFOVAngle(player, target);
            if (fov > LEGIT_FOV) {
                target = null;
                return;
            }
        }

        double distance = getTargetDistance(player, target);
        boolean inAttackRange = distance <= config.aimbotRange;

        updateFallingState(player);

        // Сбрасываем флаг атаки перед обработкой поворота
        shouldAttack = false;

        if (mode == ModConfig.RotationMode.FUNTIME_SNAP) {
            handleFuntimeSnapRotation(player, target, config, inAttackRange);
        } else {
            boolean microPause = RANDOM.nextInt(100) < MICRO_PAUSE_CHANCE;
            // Проверка видимости с учётом wallCheckEnabled
            boolean canSee = config.wallCheckEnabled ? TargetUtils.canSee(player, target, true) : true;
            if (canSee && !microPause) {
                handleLegitRageRotation(player, target, config, distance, mode);
            }
        }

        if (!inAttackRange) return;

        float cooldown = player.getAttackCooldownProgress(1.0f);
        if (cooldown < FULL_CHARGE_COOLDOWN) return;

        if (config.criticals) {
            if (!isCriticalWindow(player)) return;
        }

        // В FUNTIME_SNAP атакуем только если shouldAttack == true
        if (mode == ModConfig.RotationMode.FUNTIME_SNAP && !shouldAttack) {
            return;
        }

        if (mode != ModConfig.RotationMode.FUNTIME_SNAP) {
            if (nextErrorAttack == 0) {
                nextErrorAttack = RANDOM.nextInt(8, 11);
            }
            attackCounter++;
            if (attackCounter >= nextErrorAttack) {
                attackCounter = 0;
                nextErrorAttack = RANDOM.nextInt(8, 11);
                return;
            }
        }

        // Атакуем
        boolean shouldRestoreSprint = preAttackSprintReset(client, player);
        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
        postAttackSprintReset(client, player, shouldRestoreSprint);
        lastAttackTime = System.currentTimeMillis();
        triggerPostHitWobble();

        // После успешной атаки меняем траекторию (для FUNTIME_SNAP)
        if (mode == ModConfig.RotationMode.FUNTIME_SNAP) {
            changeTrajectory();
        }
    }

    // ===== Вспомогательные методы =====
    private static void updateFallingState(PlayerEntity player) {
        boolean onGroundNow = player.isOnGround();
        if (wasOnGroundLastTick && !onGroundNow) {
            fallStartY = player.getY();
            fallTicks = 0;
        }
        if (!onGroundNow && player.getVelocity().y < 0) {
            fallTicks++;
        } else {
            fallTicks = 0;
        }
        wasOnGroundLastTick = onGroundNow;
    }

    // ========== FUNTIME_SNAP с новыми траекториями и плавным рывком ==========
    private static void handleFuntimeSnapRotation(PlayerEntity player, Entity target, ModConfig config, boolean inAttackRange) {
        // Если цель сменилась, запоминаем новую базовую точку (центр цели)
        if (lastTargetForBase != target) {
            float[] baseRot = RotationUtils.getRotationsToEntityCenter(player, target, BASE_PREDICTION, MAX_PREDICTION, RANDOM);
            baseYaw = baseRot[0];
            basePitch = baseRot[1];
            lastTargetForBase = target;
            circlePhase = 0.0;
            changeTrajectory();
            trajectoryChangeTimer = RANDOM.nextInt(30, 60);
        } else {
            if (--trajectoryChangeTimer <= 0) {
                changeTrajectory();
                trajectoryChangeTimer = RANDOM.nextInt(30, 60);
            }
        }

        // Микро-пауза: иногда пропускаем тик кружения (камера не двигается)
        boolean skipCircle = RANDOM.nextInt(100) < MICRO_PAUSE_CHANCE;

        // Если не идёт рывок – применяем текущую траекторию
        if (!isSnapping && !skipCircle) {
            double speed = BASE_CIRCLE_SPEED + (RANDOM.nextDouble() - 0.5) * 0.05;
            circlePhase += speed;

            float yawOffset = 0, pitchOffset = 0;
            double phase = circlePhase;
            switch (currentTrajectory) {
                case CIRCLE:
                    yawOffset = (float) (Math.sin(phase) * CIRCLE_YAW_AMPLITUDE);
                    pitchOffset = (float) (Math.cos(phase) * CIRCLE_PITCH_AMPLITUDE);
                    break;
                case FIGURE_EIGHT:
                    yawOffset = (float) (Math.sin(phase) * CIRCLE_YAW_AMPLITUDE);
                    pitchOffset = (float) (Math.sin(phase * 2) * CIRCLE_PITCH_AMPLITUDE);
                    break;
            }

            player.setYaw(baseYaw + yawOffset);
            player.setPitch(MathHelper.clamp(basePitch + pitchOffset, -90f, 90f));
        }

        float cooldown = player.getAttackCooldownProgress(1.0f);
        boolean critOk = !config.criticals || isCriticalWindow(player);

        // Начинаем рывок при кулдауне >= 0.90
        if (!isSnapping && inAttackRange && cooldown >= SNAP_PREPARE_COOLDOWN && critOk) {
            // Шанс промаха (5%)
            if (RANDOM.nextInt(100) < 5) return;

            // Запускаем плавный рывок
            isSnapping = true;
            snapTicks = 0;

            snapStartYaw = player.getYaw();
            snapStartPitch = player.getPitch();

            float[] targetRot = RotationUtils.getRotationsToEntity(player, target, BASE_PREDICTION, MAX_PREDICTION, RANDOM);
            snapTargetYaw = targetRot[0];
            snapTargetPitch = targetRot[1];
        }

        // Обработка плавного рывка
        if (isSnapping) {
            snapTicks++;
            float progress = (float) snapTicks / SNAP_DURATION;
            if (progress >= 1.0f) {
                // Рывок завершён
                player.setYaw(snapTargetYaw);
                player.setPitch(MathHelper.clamp(snapTargetPitch, -90f, 90f));
                isSnapping = false;

                // Проверяем, можно ли атаковать
                float finalCooldown = player.getAttackCooldownProgress(1.0f);
                boolean finalCritOk = !config.criticals || isCriticalWindow(player);
                if (finalCooldown >= FULL_CHARGE_COOLDOWN && finalCritOk) {
                    shouldAttack = true;
                    // Обновляем базовые углы: центр цели
                    float[] centerRot = RotationUtils.getRotationsToEntityCenter(player, target, BASE_PREDICTION, MAX_PREDICTION, RANDOM);
                    baseYaw = centerRot[0];
                    basePitch = centerRot[1];
                    circlePhase = 0.0;
                    changeTrajectory();
                    trajectoryChangeTimer = RANDOM.nextInt(30, 60);
                }
            } else {
                // Линейная интерполяция
                float newYaw = snapStartYaw + (snapTargetYaw - snapStartYaw) * progress;
                float newPitch = snapStartPitch + (snapTargetPitch - snapStartPitch) * progress;
                player.setYaw(newYaw);
                player.setPitch(MathHelper.clamp(newPitch, -90f, 90f));
            }
        }
    }

    // Смена траектории (вызывается после удара)
    private static void changeTrajectory() {
        if (currentTrajectory == Trajectory.CIRCLE) {
            currentTrajectory = Trajectory.FIGURE_EIGHT;
        } else {
            currentTrajectory = Trajectory.CIRCLE;
        }
    }

    private static boolean preAttackSprintReset(MinecraftClient client, PlayerEntity player) {
        if (!SprintResetController.disableSprint(client, player)) {
            return false;
        }

        lastSprintResetTime = System.currentTimeMillis();
        return true;
    }

    private static void postAttackSprintReset(MinecraftClient client, PlayerEntity player, boolean shouldRestoreSprint) {
        SprintResetController.enableSprint(client, player, shouldRestoreSprint);
    }

    private static boolean isCriticalWindow(PlayerEntity player) {
        if (player.isOnGround() || player.isTouchingWater() || player.isSubmergedInWater() || player.isClimbing()) {
            return false;
        }
        return player.fallDistance > 0.05f || player.getVelocity().y < -0.008;
    }

    // ===== Остальные методы (без изменений) =====
    private static void handleLegitRageRotation(PlayerEntity player, Entity target, ModConfig config, double distance, ModConfig.RotationMode mode) {
        float[] targetRot = RotationUtils.getRotationsToBody(player, target, BASE_PREDICTION, MAX_PREDICTION, RANDOM);
        rotateToTarget(player, targetRot[0], targetRot[1], config, distance, mode);
    }

    private static void rotateToTarget(PlayerEntity player, float targetYaw, float targetPitch, ModConfig config, double distance, ModConfig.RotationMode mode) {
        if (postHitTicks > 0) {
            postHitTicks--;
            if (postHitTicks == 0) {
                postHitYawOffset = 0;
                postHitPitchOffset = 0;
            }
        }

        float currentYaw = player.getYaw() + postHitYawOffset;
        float currentPitch = player.getPitch() + postHitPitchOffset;

        float angleDifferenceYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float angleDifferencePitch = MathHelper.wrapDegrees(targetPitch - currentPitch);

        float distanceFactor = 1.0f;
        if (mode == ModConfig.RotationMode.LEGIT && distance > config.aimbotRange) {
            float extra = (float)(distance - config.aimbotRange);
            distanceFactor = MathHelper.clamp(1.0f - extra / config.extraRange, 0.0f, 1.0f);
        }

        float speedFactor = 1.0f;
        if (mode == ModConfig.RotationMode.LEGIT) {
            float progress = Math.min(1.0f, (Math.abs(angleDifferenceYaw) + Math.abs(angleDifferencePitch)) / 180f);
            speedFactor = EaseOutCirc.easeOut(progress);
        }

        float targetSpeedYaw = MathHelper.clamp(Math.abs(angleDifferenceYaw) * currentAimSpeed * speedFactor * distanceFactor, 0.3f, 1.0f);
        float targetSpeedPitch = MathHelper.clamp(Math.abs(angleDifferencePitch) * currentAimSpeed * speedFactor * distanceFactor, 0.3f, 1.0f);

        currentSpeedYaw += (targetSpeedYaw - currentSpeedYaw) * MOUSE_ACCEL;
        currentSpeedPitch += (targetSpeedPitch - currentSpeedPitch) * MOUSE_ACCEL;

        currentSpeedYaw *= (1 - MOUSE_FRICTION);
        currentSpeedPitch *= (1 - MOUSE_FRICTION);

        currentSpeedYaw = MathHelper.clamp(currentSpeedYaw, 0.0f, 1.0f);
        currentSpeedPitch = MathHelper.clamp(currentSpeedPitch, 0.0f, 1.0f);

        float wobbleYaw = 0f;
        float wobblePitch = 0f;
        if (mode == ModConfig.RotationMode.LEGIT && postHitWobbleTicks > 0) {
            if (postHitWobbleTicks == WOBBLE_DURATION) {
                postHitWobbleTicks--;
            } else {
                float progress = 1.0f - postHitWobbleTicks / (float) WOBBLE_DURATION;
                float strength = (float) Math.sin(progress * Math.PI);
                long elapsed = System.currentTimeMillis() - wobbleStartTime;
                wobbleYaw = (float) (Math.sin(elapsed * WOBBLE_FREQ) * WOBBLE_YAW_AMPLITUDE * strength);
                wobblePitch = (float) (Math.cos(elapsed * WOBBLE_FREQ) * WOBBLE_PITCH_AMPLITUDE * strength);
                postHitWobbleTicks--;
            }
        }

        float deltaYaw = angleDifferenceYaw * currentSpeedYaw + wobbleYaw * distanceFactor;
        float deltaPitch = angleDifferencePitch * currentSpeedPitch + wobblePitch * distanceFactor;

        float mouseSensitivity = (float) MinecraftClient.getInstance().options.getMouseSensitivity().getValue().doubleValue();
        float gcd = (float) (Math.pow(mouseSensitivity * 0.6 + 0.2, 3) * 1.2);
        deltaYaw = deltaYaw - (deltaYaw % gcd);
        deltaPitch = deltaPitch - (deltaPitch % gcd);

        float maxYaw = MAX_YAW_DELTA + (RANDOM.nextFloat() - 0.5f) * 5.0f;
        float maxPitch = MAX_PITCH_DELTA + (RANDOM.nextFloat() - 0.5f) * 3.0f;
        deltaYaw = MathHelper.clamp(deltaYaw, -maxYaw, maxYaw);
        deltaPitch = MathHelper.clamp(deltaPitch, -maxPitch, maxPitch);

        player.setYaw(player.getYaw() + deltaYaw);
        player.setPitch(MathHelper.clamp(player.getPitch() + deltaPitch, -90.0f, 90.0f));

        postHitTicks = 2;
        postHitYawOffset = (RANDOM.nextFloat() - 0.5f) * POST_HIT_JITTER_STRENGTH;
        postHitPitchOffset = (RANDOM.nextFloat() - 0.5f) * (POST_HIT_JITTER_STRENGTH / 2);
    }

    public static void triggerPostHitWobble() {
        ModConfig config = ConfigManager.getConfig();
        if (config.rotationMode == ModConfig.RotationMode.LEGIT) {
            postHitWobbleTicks = WOBBLE_DURATION;
            wobbleStartTime = System.currentTimeMillis();
        }
    }

    private static void handleReactionDelay(MinecraftClient client, PlayerEntity player, ModConfig config) {
        if (client.world == null) return;
        if (target == null && pendingTarget == null) {
            List<Entity> entities = client.world.getOtherEntities(player,
                    player.getBoundingBox().expand(getTrackRange(config)),
                    entity -> TargetUtils.isValidTarget(player, entity) && getTargetDistance(player, entity) <= getTrackRange(config));
            if (!entities.isEmpty()) {
                Entity best = selectBestTarget(entities, player, config);
                if (best != null) {
                    float fov = getFOVAngle(player, best);
                    if (fov <= LEGIT_FOV) {
                        pendingTarget = best;
                        reactionDelayTicks = RANDOM.nextInt(1, 3);
                    }
                }
            }
        }

        if (pendingTarget != null) {
            if (reactionDelayTicks > 0) {
                reactionDelayTicks--;
            } else {
                target = pendingTarget;
                pendingTarget = null;
                currentSpeedYaw = 0;
                currentSpeedPitch = 0;
                if (config.rotationMode == ModConfig.RotationMode.LEGIT) {
                    currentAimSpeed = 0.55f;
                }
            }
        }
    }

    private static Entity selectBestTarget(List<Entity> entities, PlayerEntity player, ModConfig config) {
        if (entities.isEmpty()) return null;
        switch (config.targetSort) {
            case HEALTH:
                entities.sort(Comparator.comparingDouble(e -> {
                    if (e instanceof LivingEntity le) return le.getHealth() + le.getAbsorptionAmount();
                    return Double.MAX_VALUE;
                }));
                break;
            case FOV:
                entities.sort(Comparator.comparingDouble(e -> getFOVAngle(player, e)));
                break;
            case DISTANCE:
            default:
                entities.sort(Comparator.comparingDouble(e -> TargetUtils.distanceToBoundingBox(player, e)));
                break;
        }
        return entities.getFirst();
    }

    private static void findNewTarget(MinecraftClient client, PlayerEntity player, ModConfig config) {
        if (client.world == null) return;
        List<Entity> entities = client.world.getOtherEntities(player,
                player.getBoundingBox().expand(getTrackRange(config)),
                entity -> TargetUtils.isValidTarget(player, entity) && getTargetDistance(player, entity) <= getTrackRange(config));
        if (!entities.isEmpty()) {
            target = selectBestTarget(entities, player, config);
            currentSpeedYaw = 0;
            currentSpeedPitch = 0;
        } else {
            target = null;
        }
    }

    private static float getFOVAngle(PlayerEntity player, Entity target) {
        Vec3d lookVec = player.getRotationVec(1.0f);
        Vec3d toTarget = target.getBoundingBox().getCenter().subtract(player.getEyePos()).normalize();
        return (float) Math.toDegrees(Math.acos(lookVec.dotProduct(toTarget)));
    }

    private static double getTrackRange(ModConfig config) {
        return config.aimbotRange + config.extraRange;
    }

    private static double getLoseRange(ModConfig config) {
        return getTrackRange(config) + 0.05;
    }

    private static double getTargetDistance(PlayerEntity player, Entity target) {
        return player.distanceTo(target);
    }

}
