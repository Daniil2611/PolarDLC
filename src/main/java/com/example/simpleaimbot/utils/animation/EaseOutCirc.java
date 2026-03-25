package com.example.simpleaimbot.utils.animation;

public class EaseOutCirc {
    public static float easeOut(float x) {
        return (float) Math.sqrt(1 - Math.pow(x - 1, 2));
    }
}