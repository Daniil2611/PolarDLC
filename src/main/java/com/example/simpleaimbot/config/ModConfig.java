package com.example.simpleaimbot.config;

import org.lwjgl.glfw.GLFW;

public class ModConfig {
    public static final int CURRENT_CONFIG_VERSION = 5;

    public int configVersion = CURRENT_CONFIG_VERSION;

    // Combat modules
    public boolean triggerbotEnabled = false;
    public boolean killauraEnabled = false;

    // Visual modules
    public boolean jumpCircleEnabled = false;
    public boolean arrowsEnabled = false;
    public boolean fullbrightEnabled = false;
    public boolean playerEspEnabled = false;
    public boolean playerEspFilled = false;
    public boolean lowFireEnabled = false;
    public boolean targetEspEnabled = false;
    public boolean targetHudEnabled = false;
    public boolean attackLinesEnabled = false;
    public boolean watermarkEnabled = false;

    // Keybinds
    public int triggerbotKeyCode = -1;
    public int killauraKeyCode = -1;

    // Colors
    public int jumpCircleColor = 0xFFFFFFFF;
    public int arrowsColor = 0xFF00FF00;
    public int espColor = 0x50FF0000;
    public int guiPrimaryColor = 0xFF4F7DFF;
    public int targetEspColor = 0xFFFFFFFF;
    public int targetHudGlowColor = 0xFFFF0000;
    public int watermarkX = -1;
    public int watermarkY = 8;
    public boolean watermarkCustomPosition = false;

    // Combat settings
    public double aimbotRange = 3.0;
    public boolean wallCheckEnabled = true;
    public int extraRange = 0;

    public enum TargetSort { DISTANCE, HEALTH, FOV }
    public TargetSort targetSort = TargetSort.DISTANCE;
    public boolean criticals = false;

    // FreeLook
    public int freeLookKeyCode = GLFW.GLFW_KEY_LEFT_ALT;
    public boolean freeLookToggle = false;

    // ArrayList
    public boolean arrayListEnabled = false;
    public boolean arrayListRight = false;
    public int arrayListGlowColor = 0xFFFF0000;

    // Swing animation
    public boolean swingAnimationEnabled = false;
    public int swingAnimationStyle = 2;

    // Rotation mode
    public enum RotationMode { LEGIT, RAGE, FUNTIME_SNAP }
    public RotationMode rotationMode = RotationMode.LEGIT;

    // Jump circle radius
    public int jumpCircleRadius = 2;
    public enum JumpCircleMode { DISC, RING, WAVE, DOUBLE_RING }
    public JumpCircleMode jumpCircleMode = JumpCircleMode.DISC;

    public enum TargetEspMode { SPIRAL, RING_STACK, CYLINDER, SPIRITS }
    public TargetEspMode targetEspMode = TargetEspMode.SPIRAL;

    // Movement modules
    public boolean flyEnabled = false;
    public boolean autoSprintEnabled = false;
    public boolean inventoryMoveEnabled = false;

    // AutoTotem
    public boolean autoTotemEnabled = false;
    public int autoTotemThreshold = 5;
    public int autoTotemDelay = 3;

    public void sanitize() {
        aimbotRange = clamp(aimbotRange, 1.0, 6.0);
        extraRange = clamp(extraRange, 0, 3);
        jumpCircleRadius = clamp(jumpCircleRadius, 2, 7);
        if (configVersion < 4) {
            swingAnimationStyle = migrateLegacySwingStyle(swingAnimationStyle);
        }
        if (configVersion < 5) {
            guiPrimaryColor = 0xFF4F7DFF;
        }
        swingAnimationStyle = normalizeSwingStyle(swingAnimationStyle);
        autoTotemThreshold = clamp(autoTotemThreshold, 1, 20);
        autoTotemDelay = clamp(autoTotemDelay, 0, 20);
        watermarkY = clamp(watermarkY, 0, 2000);
        if (watermarkX < -1) {
            watermarkX = -1;
        }
        configVersion = CURRENT_CONFIG_VERSION;

        if (targetSort == null) {
            targetSort = TargetSort.DISTANCE;
        }
        if (rotationMode == null) {
            rotationMode = RotationMode.LEGIT;
        }
        if (jumpCircleMode == null) {
            jumpCircleMode = JumpCircleMode.DISC;
        }
        if (targetEspMode == null) {
            targetEspMode = TargetEspMode.SPIRAL;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int normalizeSwingStyle(int style) {
        return switch (style) {
            case 1, 2, 3 -> style;
            default -> 2;
        };
    }

    private static int migrateLegacySwingStyle(int style) {
        return switch (style) {
            case 6 -> 1;
            case 3 -> 2;
            case 2 -> 3;
            default -> style;
        };
    }
}
