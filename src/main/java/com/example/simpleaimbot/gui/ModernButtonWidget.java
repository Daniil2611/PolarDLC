package com.example.simpleaimbot.gui;

import com.example.simpleaimbot.utils.render.Render2DEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public class ModernButtonWidget extends ButtonWidget {
    @FunctionalInterface
    public interface SecondaryPressAction {
        void onSecondaryPress(ModernButtonWidget button);
    }

    public enum Variant {
        TAB,
        PRIMARY,
        SECONDARY,
        UTILITY,
        COLOR
    }

    private @Nullable Text tooltip;
    private Variant variant = Variant.PRIMARY;
    private boolean visualActive;
    private float hoverProgress;
    private float activeProgress;
    private int accentColor = GuiTheme.accent();
    private @Nullable SecondaryPressAction secondaryPressAction;

    public ModernButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, button -> Text.empty());
    }

    public ModernButtonWidget withTooltip(Text tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public ModernButtonWidget withVariant(Variant variant) {
        this.variant = variant;
        return this;
    }

    public ModernButtonWidget withActive(boolean active) {
        this.visualActive = active;
        return this;
    }

    public ModernButtonWidget withAccentColor(int color) {
        this.accentColor = color;
        return this;
    }

    public void setVisualActive(boolean active) {
        this.visualActive = active;
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
    }

    public ModernButtonWidget withSecondaryAction(SecondaryPressAction action) {
        this.secondaryPressAction = action;
        return this;
    }

    @Nullable
    public Text getTooltipText() {
        return tooltip;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        hoverProgress = MathHelper.lerp(0.28f, hoverProgress, hovered ? 1.0f : 0.0f);
        activeProgress = MathHelper.lerp(0.24f, activeProgress, visualActive ? 1.0f : 0.0f);

        int baseColor = baseColor();
        int borderColor = borderColor();
        int shadowColor = GuiTheme.withAlpha(GuiTheme.mix(accentColor, GuiTheme.accentBright(), hoverProgress), (int) (8 + 16 * hoverProgress + 20 * activeProgress));
        int fillColor = GuiTheme.mix(baseColor, accentColor, 0.08f * hoverProgress + 0.18f * activeProgress);

        Render2DEngine.drawBlurredShadow(context.getMatrices(), getX(), getY(), width, height, 3, new Color(shadowColor, true));
        Render2DEngine.drawRound(context.getMatrices(), getX(), getY(), width, height, radius(), new Color(GuiTheme.withAlpha(borderColor, 128), true));
        Render2DEngine.drawRound(context.getMatrices(), getX() + 1, getY() + 1, width - 2.0f, height - 2.0f, Math.max(1, radius() - 1),
                new Color(fillColor, true));
        Render2DEngine.drawRound(context.getMatrices(), getX() + 2, getY() + 2, width - 4.0f, height - 4.0f, Math.max(1, radius() - 2),
                new Color(GuiTheme.mix(fillColor, 0xFFFFFFFF, 0.03f + hoverProgress * 0.05f), true));

        if (visualActive) {
            Render2DEngine.drawRound(context.getMatrices(), getX() + 4, getY() + height - 5, width - 8.0f, 2.0f, 1.0f,
                    new Color(GuiTheme.withAlpha(accentColor, 230), true));
        } else if (variant == Variant.TAB) {
            Render2DEngine.drawRound(context.getMatrices(), getX() + 5, getY() + height - 4, width - 10.0f, 1.0f, 1.0f,
                    new Color(GuiTheme.withAlpha(GuiTheme.text(), hovered ? 70 : 26), true));
        }

        if (variant == Variant.COLOR) {
            Render2DEngine.drawRound(context.getMatrices(), getX() + width - 19, getY() + height / 2.0f - 6, 12.0f, 12.0f, 6.0f,
                    new Color(GuiTheme.withAlpha(0xFFFFFFFF, 90), true));
            Render2DEngine.drawRound(context.getMatrices(), getX() + width - 18, getY() + height / 2.0f - 5, 10.0f, 10.0f, 5.0f,
                    new Color(accentColor, true));
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int maxTextWidth = Math.max(24, width - 20 - (variant == Variant.COLOR ? 16 : 0));
        String label = client.textRenderer.trimToWidth(getMessage().getString(), maxTextWidth);
        Text buttonText = GuiTheme.uniform(label);
        int textColor = GuiTheme.mix(GuiTheme.mutedText(), GuiTheme.text(), 0.45f + hoverProgress * 0.25f + activeProgress * 0.35f);
        int textX = getX() + width / 2 - (variant == Variant.COLOR ? 8 : 0);
        context.drawCenteredTextWithShadow(client.textRenderer, buttonText, textX, getY() + (height - 8) / 2, textColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && active && visible && isMouseOver(mouseX, mouseY) && secondaryPressAction != null) {
            playDownSound(MinecraftClient.getInstance().getSoundManager());
            secondaryPressAction.onSecondaryPress(this);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int baseColor() {
        return switch (variant) {
            case TAB -> GuiTheme.mix(GuiTheme.panelAlt(), 0xFF090B10, 0.32f);
            case SECONDARY -> GuiTheme.mix(GuiTheme.panel(), 0xFF0E1219, 0.36f);
            case UTILITY -> GuiTheme.mix(GuiTheme.panel(), 0xFF11151C, 0.30f);
            case COLOR -> GuiTheme.mix(GuiTheme.panelAlt(), 0xFF0D1017, 0.34f);
            case PRIMARY -> GuiTheme.mix(GuiTheme.panel(), 0xFF10141C, 0.24f);
        };
    }

    private int borderColor() {
        int borderBase = switch (variant) {
            case TAB -> GuiTheme.withAlpha(GuiTheme.text(), 26);
            case SECONDARY -> GuiTheme.withAlpha(GuiTheme.text(), 20);
            case UTILITY -> GuiTheme.withAlpha(GuiTheme.danger(), 46);
            case COLOR -> GuiTheme.withAlpha(accentColor, 96);
            case PRIMARY -> GuiTheme.withAlpha(GuiTheme.accent(), 34);
        };
        return GuiTheme.mix(borderBase, GuiTheme.withAlpha(accentColor, 170), 0.32f * hoverProgress + 0.50f * activeProgress);
    }

    private int radius() {
        return variant == Variant.TAB ? 10 : 12;
    }
}
