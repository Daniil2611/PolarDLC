package com.example.simpleaimbot.gui;

import com.example.simpleaimbot.utils.render.Render2DEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class ModuleCardWidget extends ClickableWidget {
    @FunctionalInterface
    public interface PressAction {
        void onPress(ModuleCardWidget widget);
    }

    private final String title;
    private final String description;
    private final Supplier<String> statusSupplier;
    private final BooleanSupplier activeSupplier;
    private final String actionHint;
    private final PressAction primaryAction;
    private final @Nullable PressAction secondaryAction;
    private final @Nullable Text tooltip;

    private boolean selected;
    private float hoverProgress;
    private float selectedProgress;

    public ModuleCardWidget(int x, int y, int width, int height, String title, String description,
                            Supplier<String> statusSupplier, BooleanSupplier activeSupplier, String actionHint,
                            PressAction primaryAction, @Nullable PressAction secondaryAction, @Nullable Text tooltip) {
        super(x, y, width, height, GuiTheme.uniform(title));
        this.title = title;
        this.description = description;
        this.statusSupplier = statusSupplier;
        this.activeSupplier = activeSupplier;
        this.actionHint = actionHint;
        this.primaryAction = primaryAction;
        this.secondaryAction = secondaryAction;
        this.tooltip = tooltip;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public @Nullable Text getTooltipText() {
        return tooltip;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean hovered = isMouseOver(mouseX, mouseY);
        boolean active = activeSupplier.getAsBoolean();

        hoverProgress = MathHelper.lerp(0.24f, hoverProgress, hovered ? 1.0f : 0.0f);
        selectedProgress = MathHelper.lerp(0.24f, selectedProgress, selected ? 1.0f : 0.0f);

        int accentColor = active ? GuiTheme.accentBright() : GuiTheme.accent();
        int fill = GuiTheme.mix(GuiTheme.panel(), GuiTheme.withAlpha(accentColor, 255), 0.06f + selectedProgress * 0.12f);
        int border = GuiTheme.mix(GuiTheme.withAlpha(GuiTheme.text(), 22), GuiTheme.withAlpha(accentColor, 168),
                0.20f + selectedProgress * 0.55f + hoverProgress * 0.18f);
        int glow = GuiTheme.withAlpha(accentColor, 8 + Math.round(12 * hoverProgress + 18 * selectedProgress));

        Render2DEngine.drawBlurredShadow(context.getMatrices(), getX(), getY(), width, height, 3, new Color(glow, true));
        Render2DEngine.drawRound(context.getMatrices(), getX(), getY(), width, height, 16, new Color(GuiTheme.withAlpha(border, 132), true));
        Render2DEngine.drawRound(context.getMatrices(), getX() + 1, getY() + 1, width - 2.0f, height - 2.0f, 15, new Color(fill, true));
        Render2DEngine.drawRound(context.getMatrices(), getX() + 2, getY() + 2, width - 4.0f, height - 4.0f, 14,
                new Color(GuiTheme.mix(fill, 0xFFFFFFFF, 0.02f + hoverProgress * 0.04f), true));

        Render2DEngine.drawRound(context.getMatrices(), getX() + 10, getY() + 12, 4.0f, height - 24.0f, 2.0f,
                new Color(GuiTheme.withAlpha(accentColor, active ? 240 : 110), true));

        String status = statusSupplier.get();
        int badgeWidth = Math.max(44, client.textRenderer.getWidth(GuiTheme.uniform(status)) + 16);
        int badgeX = getX() + width - badgeWidth - 12;
        int badgeY = getY() + 12;
        int badgeFill = active
                ? GuiTheme.withAlpha(accentColor, 168)
                : GuiTheme.withAlpha(GuiTheme.panelAlt(), 214);
        int badgeEdge = active
                ? GuiTheme.withAlpha(GuiTheme.accentBright(), 178)
                : GuiTheme.withAlpha(GuiTheme.text(), 28);

        int titleWidth = Math.max(20, badgeX - (getX() + 24) - 8);
        Text titleText = GuiTheme.uniform(client.textRenderer.trimToWidth(title, titleWidth));
        Text statusText = GuiTheme.uniform(status);
        Text hintText = GuiTheme.uniform(client.textRenderer.trimToWidth(actionHint, width - 36));
        List<OrderedText> descriptionLines = client.textRenderer.wrapLines(GuiTheme.uniform(description), Math.max(22, width - 48));

        context.drawTextWithShadow(client.textRenderer, titleText, getX() + 24, getY() + 12, GuiTheme.text());

        int descriptionY = getY() + 28;
        for (int i = 0; i < Math.min(2, descriptionLines.size()); i++) {
            context.drawText(client.textRenderer, descriptionLines.get(i), getX() + 24, descriptionY, GuiTheme.mutedText(), false);
            descriptionY += client.textRenderer.fontHeight + 1;
        }

        context.drawText(client.textRenderer, hintText, getX() + 24, getY() + height - 17,
                GuiTheme.withAlpha(GuiTheme.accentBright(), 214), false);

        GuiTheme.drawCard(context, badgeX, badgeY, badgeWidth, 18, badgeFill, badgeEdge, 9);
        context.drawCenteredTextWithShadow(client.textRenderer, statusText, badgeX + badgeWidth / 2, badgeY + 5, GuiTheme.text());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || !isMouseOver(mouseX, mouseY)) {
            return false;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            playDownSound(MinecraftClient.getInstance().getSoundManager());
            primaryAction.onPress(this);
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && secondaryAction != null) {
            playDownSound(MinecraftClient.getInstance().getSoundManager());
            secondaryAction.onPress(this);
            return true;
        }

        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
