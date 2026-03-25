package com.example.simpleaimbot.utils.animation;

public class AnimationHelper {
    private float currentValue;
    private float targetValue;
    private float speed = 0.1f;

    public AnimationHelper() {
        this.currentValue = 0f;
        this.targetValue = 0f;
    }

    public void update(boolean increasing) {
        targetValue = increasing ? 1f : 0f;
    }

    public void tick() {
        if (currentValue < targetValue) {
            currentValue += speed;
            if (currentValue > targetValue) currentValue = targetValue;
        } else if (currentValue > targetValue) {
            currentValue -= speed;
            if (currentValue < targetValue) currentValue = targetValue;
        }
    }

    public float getValue() {
        return currentValue;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setInstant(float value) {
        this.currentValue = value;
        this.targetValue = value;
    }
}