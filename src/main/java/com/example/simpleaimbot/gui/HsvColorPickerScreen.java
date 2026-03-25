package com.example.simpleaimbot.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;

public class HsvColorPickerScreen extends Screen {
    private final ColorConsumer callback;
    private final Screen parent;

    private int currentColor;
    private float hue;
    private float saturation = 1.0f;
    private float brightness = 1.0f;

    private int shellX;
    private int shellY;
    private int shellWidth;
    private int shellHeight;
    private int pickerX;
    private int pickerY;
    private int pickerSize;
    private int hueX;
    private int hueY;
    private int hueWidth = 24;
    private int hueHeight;
    private int previewX;
    private int previewY;
    private int previewWidth;

    private boolean draggingPicker;
    private boolean draggingHue;

    public HsvColorPickerScreen(ColorConsumer callback, Screen parent, int initialColor) {
        super(GuiTheme.uniform("Выбор цвета"));
        this.callback = callback;
        this.parent = parent;
        this.currentColor = initialColor;

        Color initial = new Color(initialColor, true);
        float[] hsv = Color.RGBtoHSB(initial.getRed(), initial.getGreen(), initial.getBlue(), null);
        hue = hsv[0] * 360.0f;
        saturation = hsv[1];
        brightness = hsv[2];
    }

    @Override
    protected void init() {
        shellWidth = Math.min(720, width - 60);
        shellHeight = Math.min(420, height - 50);
        shellX = (width - shellWidth) / 2;
        shellY = (height - shellHeight) / 2;

        pickerSize = Math.min(250, shellHeight - 140);
        hueHeight = pickerSize;
        pickerX = shellX + 26;
        pickerY = shellY + 72;
        hueX = pickerX + pickerSize + 16;
        hueY = pickerY;
        previewX = hueX + hueWidth + 20;
        previewY = pickerY;
        previewWidth = shellX + shellWidth - previewX - 24;

        addDrawableChild(new ModernButtonWidget(shellX + shellWidth - 220, shellY + shellHeight - 46, 94, 24,
                GuiTheme.uniform("Применить"), button -> {
                    callback.accept(currentColor);
                    MinecraftClient.getInstance().setScreen(parent);
                }).withVariant(ModernButtonWidget.Variant.PRIMARY)
                .withActive(true)
                .withTooltip(GuiTheme.uniform("Сохранить выбранный цвет и вернуться назад.")));

        addDrawableChild(new ModernButtonWidget(shellX + shellWidth - 114, shellY + shellHeight - 46, 88, 24,
                GuiTheme.uniform("Назад"), button -> MinecraftClient.getInstance().setScreen(parent))
                .withVariant(ModernButtonWidget.Variant.SECONDARY)
                .withTooltip(GuiTheme.uniform("Закрыть палитру без изменений.")));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        GuiTheme.drawBackdrop(context, width, height);
        GuiTheme.drawCard(context, shellX, shellY, shellWidth, shellHeight, GuiTheme.withAlpha(GuiTheme.panel(), 214), GuiTheme.panelEdge(), 18);
        GuiTheme.drawCard(context, shellX + 18, shellY + 18, shellWidth - 36, 40, GuiTheme.withAlpha(GuiTheme.panelAlt(), 220), GuiTheme.withAlpha(GuiTheme.accent(), 80), 14);

        super.render(context, mouseX, mouseY, delta);

        drawHeader(context);
        drawPicker(context);
        drawHueSlider(context);
        drawPreview(context);
        drawStats(context);
    }

    private void drawHeader(DrawContext context) {
        context.drawTextWithShadow(textRenderer, GuiTheme.uniform("Выбор цвета"), shellX + 30, shellY + 30, GuiTheme.text());
        context.drawText(textRenderer, GuiTheme.uniform("Настрой акцент клиента, не выходя из игры."), shellX + 30, shellY + 44, GuiTheme.mutedText(), false);
    }

    private void drawPicker(DrawContext context) {
        GuiTheme.drawCard(context, pickerX - 6, pickerY - 6, pickerSize + 12, pickerSize + 12,
                GuiTheme.withAlpha(GuiTheme.panelAlt(), 186), GuiTheme.withAlpha(GuiTheme.text(), 18), 16);

        for (int x = 0; x < pickerSize; x++) {
            float currentSaturation = (float) x / pickerSize;
            for (int y = 0; y < pickerSize; y++) {
                float currentBrightness = 1.0f - (float) y / pickerSize;
                int color = Color.HSBtoRGB(hue / 360.0f, currentSaturation, currentBrightness);
                context.fill(pickerX + x, pickerY + y, pickerX + x + 1, pickerY + y + 1, 0xFF000000 | color);
            }
        }

        int markerX = pickerX + (int) (saturation * pickerSize);
        int markerY = pickerY + (int) ((1.0f - brightness) * pickerSize);
        context.fill(markerX - 3, markerY - 3, markerX + 3, markerY + 3, 0xFFFFFFFF);
        context.fill(markerX - 2, markerY - 2, markerX + 2, markerY + 2, 0xFF000000);
    }

    private void drawHueSlider(DrawContext context) {
        GuiTheme.drawCard(context, hueX - 6, hueY - 6, hueWidth + 12, hueHeight + 12,
                GuiTheme.withAlpha(GuiTheme.panelAlt(), 186), GuiTheme.withAlpha(GuiTheme.text(), 18), 16);

        for (int y = 0; y < hueHeight; y++) {
            float currentHue = (float) y / hueHeight * 360.0f;
            int color = Color.HSBtoRGB(currentHue / 360.0f, 1.0f, 1.0f);
            context.fill(hueX, hueY + y, hueX + hueWidth, hueY + y + 1, 0xFF000000 | color);
        }

        int markerY = hueY + (int) (hue / 360.0f * hueHeight);
        context.fill(hueX - 3, markerY - 2, hueX + hueWidth + 3, markerY + 2, 0xFFFFFFFF);
        context.fill(hueX - 2, markerY - 1, hueX + hueWidth + 2, markerY + 1, 0xFF000000);
    }

    private void drawPreview(DrawContext context) {
        GuiTheme.drawCard(context, previewX, previewY - 6, previewWidth, pickerSize + 12,
                GuiTheme.withAlpha(GuiTheme.panelAlt(), 200), GuiTheme.withAlpha(GuiTheme.accent(), 70), 16);

        context.drawTextWithShadow(textRenderer, GuiTheme.uniform("Предпросмотр"), previewX + 16, previewY + 8, GuiTheme.text());
        context.drawText(textRenderer, GuiTheme.uniform("HEX"), previewX + 16, previewY + 28, GuiTheme.mutedText(), false);
        context.drawText(textRenderer, GuiTheme.uniform(toHex(currentColor)), previewX + 16, previewY + 40, GuiTheme.withAlpha(GuiTheme.accentBright(), 245), false);

        int swatchX = previewX + 16;
        int swatchY = previewY + 64;
        int swatchSize = Math.min(120, previewWidth - 32);
        GuiTheme.drawCard(context, swatchX, swatchY, swatchSize, swatchSize, currentColor, GuiTheme.withAlpha(0xFFFFFFFF, 92), 18);

        int barsX = swatchX + swatchSize + 18;
        int barWidth = Math.max(70, previewWidth - (barsX - previewX) - 16);
        drawValueBar(context, barsX, swatchY + 6, barWidth, "Hue", Math.round(hue));
        drawValueBar(context, barsX, swatchY + 36, barWidth, "Sat", Math.round(saturation * 100.0f));
        drawValueBar(context, barsX, swatchY + 66, barWidth, "Bri", Math.round(brightness * 100.0f));
    }

    private void drawStats(DrawContext context) {
        int textX = pickerX;
        int textY = shellY + shellHeight - 78;
        context.drawText(textRenderer, GuiTheme.uniform("ЛКМ по палитре меняет насыщенность и яркость."), textX, textY, GuiTheme.mutedText(), false);
        context.drawText(textRenderer, GuiTheme.uniform("ЛКМ по вертикальной шкале меняет оттенок."), textX, textY + 12, GuiTheme.mutedText(), false);
    }

    private void drawValueBar(DrawContext context, int x, int y, int width, String label, int value) {
        GuiTheme.drawCard(context, x, y, width, 20, GuiTheme.withAlpha(GuiTheme.panel(), 190), GuiTheme.withAlpha(GuiTheme.text(), 18), 10);
        context.drawText(textRenderer, GuiTheme.uniform(label), x + 8, y + 6, GuiTheme.mutedText(), false);
        context.drawText(textRenderer, GuiTheme.uniform(String.valueOf(value)), x + width - 8 - textRenderer.getWidth(String.valueOf(value)), y + 6, GuiTheme.text(), false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseX >= pickerX && mouseX <= pickerX + pickerSize && mouseY >= pickerY && mouseY <= pickerY + pickerSize) {
                draggingPicker = true;
                updateFromPicker(mouseX, mouseY);
                return true;
            }
            if (mouseX >= hueX && mouseX <= hueX + hueWidth && mouseY >= hueY && mouseY <= hueY + hueHeight) {
                draggingHue = true;
                updateFromHue(mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingPicker) {
            updateFromPicker(mouseX, mouseY);
            return true;
        }
        if (draggingHue) {
            updateFromHue(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingPicker = false;
        draggingHue = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void updateFromPicker(double mouseX, double mouseY) {
        float x = (float) MathHelper.clamp(mouseX - pickerX, 0, pickerSize);
        float y = (float) MathHelper.clamp(mouseY - pickerY, 0, pickerSize);
        saturation = x / pickerSize;
        brightness = 1.0f - (y / pickerSize);
        updateColor();
    }

    private void updateFromHue(double mouseY) {
        float y = (float) MathHelper.clamp(mouseY - hueY, 0, hueHeight);
        hue = y / hueHeight * 360.0f;
        updateColor();
    }

    private void updateColor() {
        int rgb = Color.HSBtoRGB(hue / 360.0f, saturation, brightness);
        currentColor = 0xFF000000 | rgb;
    }

    private String toHex(int color) {
        return String.format("#%06X", color & 0x00FFFFFF);
    }

    public interface ColorConsumer {
        void accept(int color);
    }
}
