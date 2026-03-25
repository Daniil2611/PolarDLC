package com.example.simpleaimbot.gui.imgui;

import com.example.simpleaimbot.client.ClientBranding;
import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.internal.ImGuiContext;
import imgui.type.ImBoolean;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ImGuiSettingsScreen extends Screen {
    private static final float MAX_WIDTH = 1180.0f;
    private static final float MAX_HEIGHT = 760.0f;
    private static final float WINDOW_PADDING = 12.0f;
    private static final float LEFT_PANEL_WIDTH = 330.0f;
    private static ImGuiContext sharedContext;
    private static ImGuiImplGlfw sharedGlfw;
    private static ImGuiImplGl3 sharedGl3;
    private static boolean sharedInitialized;

    private final MinecraftClient client = MinecraftClient.getInstance();

    private ImFont uiFont;
    private boolean initialized;
    private String selectedModule = "triggerbot";
    private String waitingBind;

    public ImGuiSettingsScreen() {
        super(Text.literal(ClientBranding.NAME));
    }

    @Override
    protected void init() {
        super.init();
        if (initialized) {
            return;
        }

        if (!sharedInitialized) {
            sharedContext = ImGui.createContext();
            ImGui.setCurrentContext(sharedContext);

            ImGuiIO io = ImGui.getIO();
            io.setIniFilename("polardlc-imgui.ini");
            io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
            configureFonts(io);
            configureStyle();

            sharedGlfw = new ImGuiImplGlfw();
            sharedGl3 = new ImGuiImplGl3();
            sharedGlfw.init(client.getWindow().getHandle(), false);
            sharedGl3.init("#version 150");
            sharedInitialized = true;
        } else {
            ImGui.setCurrentContext(sharedContext);
        }

        initialized = true;
    }

    @Override
    public void removed() {
        waitingBind = null;
        initialized = false;
        super.removed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x30070A10);
        if (!initialized || !sharedInitialized || sharedContext == null) {
            return;
        }

        ImGui.setCurrentContext(sharedContext);
        syncMousePosition(mouseX, mouseY);
        sharedGlfw.newFrame();
        sharedGl3.newFrame();
        ImGui.newFrame();
        renderMainWindow();
        ImGui.render();
        sharedGl3.renderDrawData(ImGui.getDrawData());
    }

    private void renderMainWindow() {
        float windowPixelsWidth = client.getWindow().getWidth();
        float windowPixelsHeight = client.getWindow().getHeight();
        float windowWidth = Math.min(MAX_WIDTH, windowPixelsWidth - 24.0f);
        float windowHeight = Math.min(MAX_HEIGHT, windowPixelsHeight - 24.0f);
        float x = (windowPixelsWidth - windowWidth) * 0.5f;
        float y = (windowPixelsHeight - windowHeight) * 0.5f;

        ImGui.setNextWindowPos(x, y, ImGuiCond.Always);
        ImGui.setNextWindowSize(windowWidth, windowHeight, ImGuiCond.Always);

        int flags = ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoSavedSettings
                | ImGuiWindowFlags.NoTitleBar
                | ImGuiWindowFlags.NoMove;

        if (!ImGui.begin("##polardlc-main", flags)) {
            ImGui.end();
            return;
        }

        renderHero(windowWidth);

        if (ImGui.beginTabBar("tabs")) {
            if (ImGui.beginTabItem("Attack")) {
                renderAttackTab();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Visual")) {
                renderVisualTab();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Movement")) {
                renderMovementTab();
                ImGui.endTabItem();
            }
            ImGui.endTabBar();
        }

        ImGui.end();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (initialized && sharedInitialized) {
            ImGui.setCurrentContext(sharedContext);
            sharedGlfw.mouseButtonCallback(client.getWindow().getHandle(), button, GLFW.GLFW_PRESS, 0);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (initialized && sharedInitialized) {
            ImGui.setCurrentContext(sharedContext);
            sharedGlfw.mouseButtonCallback(client.getWindow().getHandle(), button, GLFW.GLFW_RELEASE, 0);
        }
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (initialized) {
            syncMousePosition(mouseX, mouseY);
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (initialized) {
            syncMousePosition(mouseX, mouseY);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (initialized && sharedInitialized) {
            ImGui.setCurrentContext(sharedContext);
            sharedGlfw.scrollCallback(client.getWindow().getHandle(), horizontalAmount, verticalAmount);
        }
        return true;
    }

    private void renderHero(float windowWidth) {
        ImGui.beginChild("hero", 0.0f, 92.0f, true);
        ImGui.text(ClientBranding.NAME);
        ImGui.sameLine();
        ImGui.textDisabled("v" + ClientBranding.VERSION);
        ImGui.textDisabled("ImGui UI active  |  Right Shift открывает меню  |  ESC закрывает экран");
        if (waitingBind != null) {
            ImGui.textDisabled("Ожидание клавиши для: " + waitingBind);
        } else {
            ImGui.textDisabled("Вкладки и функции оставлены на English, служебный текст переведен на русский.");
        }

        float buttonWidth = 136.0f;
        if (windowWidth > 620.0f) {
            ImGui.sameLine(Math.max(0.0f, windowWidth - buttonWidth - 54.0f));
        }
        if (ImGui.button(waitingBind != null ? "Отмена бинда" : "Закрыть", buttonWidth, 0.0f)) {
            if (waitingBind != null) {
                waitingBind = null;
            } else {
                close();
            }
        }
        ImGui.endChild();
        ImGui.spacing();
    }

    private void renderAttackTab() {
        List<ModuleRef> modules = List.of(
                new ModuleRef("triggerbot", "Triggerbot", "Бьет цель под прицелом."),
                new ModuleRef("killaura", "KillAura", "Основной боевой модуль с ротацией."),
                new ModuleRef("autototem", "AutoTotem", "Держит тотем в оффхенде при низком HP.")
        );
        ensureSelected(modules);
        renderModuleColumns(modules);
    }

    private void renderVisualTab() {
        List<ModuleRef> modules = List.of(
                new ModuleRef("jumpcircle", "Jump Circle", "Эффект приземления с несколькими режимами."),
                new ModuleRef("fullbright", "Fullbright", "Повышает яркость сцены."),
                new ModuleRef("playeresp", "Player ESP", "Подсвечивает игроков в мире."),
                new ModuleRef("lowfire", "Low Fire", "Уменьшает огонь на экране."),
                new ModuleRef("targetesp", "Target ESP", "Выделение текущей цели."),
                new ModuleRef("arraylist", "Array List", "Список активных модулей."),
                new ModuleRef("watermark", "Watermark", "Логотип, версия, FPS и ping."),
                new ModuleRef("targethud", "Target HUD", "Компактная панель цели."),
                new ModuleRef("attacklines", "Attack Lines", "Подсказка радиуса атаки."),
                new ModuleRef("swinganim", "Swing Anim", "Кастомная анимация руки.")
        );
        ensureSelected(modules);
        renderModuleColumns(modules);
    }

    private void renderMovementTab() {
        List<ModuleRef> modules = List.of(
                new ModuleRef("fly", "Fly", "Клиентский полет."),
                new ModuleRef("autosprint", "AutoSprint", "Автоматический бег."),
                new ModuleRef("inventorymove", "InventoryMove", "Движение с открытым инвентарем."),
                new ModuleRef("freelook", "FreeLook", "Свободная камера.")
        );
        ensureSelected(modules);
        renderModuleColumns(modules);
    }

    private void renderModuleColumns(List<ModuleRef> modules) {
        ImGui.beginChild("module-list", LEFT_PANEL_WIDTH, 0.0f, true);
        ImGui.textDisabled("Модули");
        ImGui.separator();

        for (ModuleRef module : modules) {
            boolean selected = module.id().equals(selectedModule);
            if (ImGui.selectable(module.title() + moduleStatusSuffix(module.id()), selected)) {
                selectedModule = module.id();
            }
            ImGui.textDisabled(module.description());
            ImGui.separator();
        }
        ImGui.endChild();

        ImGui.sameLine();

        ImGui.beginChild("module-settings", 0.0f, 0.0f, true);
        renderSelectedModule();
        ImGui.endChild();
    }

    private void renderSelectedModule() {
        ModConfig config = ConfigManager.getConfig();
        switch (selectedModule) {
            case "triggerbot" -> {
                renderHeader("Triggerbot", "Бьет цель под прицелом.");
                renderToggle("Включено", config.triggerbotEnabled, value -> config.triggerbotEnabled = value);
                renderKeyBind("Бинд", "triggerbot", config.triggerbotKeyCode);
            }
            case "killaura" -> {
                renderHeader("KillAura", "Основной боевой модуль с ротацией.");
                renderToggle("Включено", config.killauraEnabled, value -> config.killauraEnabled = value);
                renderKeyBind("Бинд", "killaura", config.killauraKeyCode);
                renderCycleButton("Extra Range", String.valueOf(config.extraRange), () -> config.extraRange = (config.extraRange + 1) % 4);
                renderCycleButton("Target Sort", config.targetSort.name(), () -> {
                    ModConfig.TargetSort[] values = ModConfig.TargetSort.values();
                    config.targetSort = values[(config.targetSort.ordinal() + 1) % values.length];
                });
                renderToggle("Criticals", config.criticals, value -> config.criticals = value);
                renderCycleButton("Rotation", config.rotationMode.name(), () -> {
                    ModConfig.RotationMode[] values = ModConfig.RotationMode.values();
                    config.rotationMode = values[(config.rotationMode.ordinal() + 1) % values.length];
                });
                renderToggle("Wall Check", config.wallCheckEnabled, value -> config.wallCheckEnabled = value);
            }
            case "autototem" -> {
                renderHeader("AutoTotem", "Держит тотем в оффхенде при низком HP.");
                renderToggle("Включено", config.autoTotemEnabled, value -> config.autoTotemEnabled = value);
                renderSliderInt("HP", config.autoTotemThreshold, 1, 20, value -> config.autoTotemThreshold = value);
                renderSliderInt("Delay", config.autoTotemDelay, 0, 20, value -> config.autoTotemDelay = value);
            }
            case "jumpcircle" -> {
                renderHeader("Jump Circle", "Эффект приземления с несколькими режимами.");
                renderToggle("Включено", config.jumpCircleEnabled, value -> config.jumpCircleEnabled = value);
                renderCycleButton("Mode", config.jumpCircleMode.name(), () -> {
                    ModConfig.JumpCircleMode[] values = ModConfig.JumpCircleMode.values();
                    config.jumpCircleMode = values[(config.jumpCircleMode.ordinal() + 1) % values.length];
                });
                renderSliderInt("Radius", config.jumpCircleRadius, 2, 7, value -> config.jumpCircleRadius = value);
                renderColorEditor("Color", config.jumpCircleColor, value -> config.jumpCircleColor = value);
            }
            case "fullbright" -> {
                renderHeader("Fullbright", "Повышает яркость сцены.");
                renderToggle("Включено", config.fullbrightEnabled, value -> config.fullbrightEnabled = value);
            }
            case "playeresp" -> {
                renderHeader("Player ESP", "Подсвечивает игроков в мире.");
                renderToggle("Включено", config.playerEspEnabled, value -> config.playerEspEnabled = value);
                renderToggle("Filled", config.playerEspFilled, value -> config.playerEspFilled = value);
                renderColorEditor("Color", config.espColor, value -> config.espColor = value);
            }
            case "lowfire" -> {
                renderHeader("Low Fire", "Уменьшает огонь на экране.");
                renderToggle("Включено", config.lowFireEnabled, value -> config.lowFireEnabled = value);
            }
            case "targetesp" -> {
                renderHeader("Target ESP", "Выделение текущей цели.");
                renderToggle("Включено", config.targetEspEnabled, value -> config.targetEspEnabled = value);
                renderCycleButton("Mode", config.targetEspMode.name(), () -> {
                    ModConfig.TargetEspMode[] values = ModConfig.TargetEspMode.values();
                    config.targetEspMode = values[(config.targetEspMode.ordinal() + 1) % values.length];
                });
                renderColorEditor("Color", config.targetEspColor, value -> config.targetEspColor = value);
            }
            case "arraylist" -> {
                renderHeader("Array List", "Список активных модулей.");
                renderToggle("Включено", config.arrayListEnabled, value -> config.arrayListEnabled = value);
                renderToggle("Right Side", config.arrayListRight, value -> config.arrayListRight = value);
                renderColorEditor("Glow", config.arrayListGlowColor, value -> config.arrayListGlowColor = value);
            }
            case "watermark" -> {
                renderHeader("Watermark", "Логотип, версия, FPS и ping.");
                renderToggle("Включено", config.watermarkEnabled, value -> config.watermarkEnabled = value);
                ImGui.textDisabled("HUD-модуль. Геометрию и иконку я отдельно поправил вне меню.");
            }
            case "targethud" -> {
                renderHeader("Target HUD", "Компактная панель цели.");
                renderToggle("Включено", config.targetHudEnabled, value -> config.targetHudEnabled = value);
                renderColorEditor("Glow", config.targetHudGlowColor, value -> config.targetHudGlowColor = value);
            }
            case "attacklines" -> {
                renderHeader("Attack Lines", "Подсказка радиуса атаки.");
                renderToggle("Включено", config.attackLinesEnabled, value -> config.attackLinesEnabled = value);
            }
            case "swinganim" -> {
                renderHeader("Swing Anim", "Кастомная анимация руки.");
                renderToggle("Включено", config.swingAnimationEnabled, value -> config.swingAnimationEnabled = value);
                renderCycleButton("Style", swingStyleLabel(config.swingAnimationStyle),
                        () -> config.swingAnimationStyle = nextSwingStyle(config.swingAnimationStyle));
            }
            case "fly" -> {
                renderHeader("Fly", "Клиентский полет.");
                renderToggle("Включено", config.flyEnabled, value -> config.flyEnabled = value);
            }
            case "autosprint" -> {
                renderHeader("AutoSprint", "Автоматический бег.");
                renderToggle("Включено", config.autoSprintEnabled, value -> config.autoSprintEnabled = value);
            }
            case "inventorymove" -> {
                renderHeader("InventoryMove", "Движение с открытым инвентарем.");
                renderToggle("Включено", config.inventoryMoveEnabled, value -> config.inventoryMoveEnabled = value);
            }
            case "freelook" -> {
                renderHeader("FreeLook", "Свободная камера.");
                renderCycleButton("Mode", config.freeLookToggle ? "TOGGLE" : "HOLD", () -> config.freeLookToggle = !config.freeLookToggle);
                renderKeyBind("Бинд", "freeLook", config.freeLookKeyCode);
            }
            default -> {
                renderHeader("PolarDLC", "Выбери модуль слева.");
                ImGui.textDisabled("Нет выбранного модуля.");
            }
        }
    }

    private void renderHeader(String title, String description) {
        ImGui.text(title);
        ImGui.separator();
        ImGui.textWrapped(description);
        ImGui.spacing();
    }

    private void renderToggle(String label, boolean currentValue, BooleanSetter setter) {
        ImBoolean value = new ImBoolean(currentValue);
        if (ImGui.checkbox(label, value)) {
            setter.set(value.get());
            ConfigManager.save();
        }
    }

    private void renderSliderInt(String label, int currentValue, int min, int max, IntSetter setter) {
        int[] values = { currentValue };
        if (ImGui.sliderInt(label, values, min, max)) {
            setter.set(values[0]);
            ConfigManager.save();
        }
    }

    private void renderCycleButton(String label, String value, Runnable action) {
        if (ImGui.button(label + ": " + value, -1.0f, 0.0f)) {
            action.run();
            ConfigManager.save();
        }
    }

    private void renderColorEditor(String label, int currentColor, IntSetter setter) {
        float[] rgba = intToColor(currentColor);
        if (ImGui.colorEdit4(label, rgba)) {
            setter.set(colorToInt(rgba));
            ConfigManager.save();
        }
    }

    private void renderKeyBind(String label, String bindId, int keyCode) {
        String caption = waitingBind != null && waitingBind.equals(bindId)
                ? label + ": нажми клавишу..."
                : label + ": " + keyLabel(keyCode);
        if (ImGui.button(caption, -1.0f, 0.0f)) {
            waitingBind = bindId;
        }
        if (ImGui.button("Сбросить##" + bindId, -1.0f, 0.0f)) {
            applyBind(bindId, -1);
            waitingBind = null;
            ConfigManager.save();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (initialized && sharedInitialized) {
            ImGui.setCurrentContext(sharedContext);
            sharedGlfw.keyCallback(client.getWindow().getHandle(), keyCode, scanCode, GLFW.GLFW_PRESS, modifiers);
        }
        if (waitingBind != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                waitingBind = null;
                return true;
            }
            applyBind(waitingBind, keyCode);
            waitingBind = null;
            ConfigManager.save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (initialized && sharedInitialized) {
            ImGui.setCurrentContext(sharedContext);
            sharedGlfw.keyCallback(client.getWindow().getHandle(), keyCode, scanCode, GLFW.GLFW_RELEASE, modifiers);
        }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (initialized && sharedInitialized) {
            ImGui.setCurrentContext(sharedContext);
            sharedGlfw.charCallback(client.getWindow().getHandle(), chr);
        }
        return true;
    }

    private void applyBind(String bindId, int keyCode) {
        ModConfig config = ConfigManager.getConfig();
        switch (bindId) {
            case "triggerbot" -> config.triggerbotKeyCode = keyCode;
            case "killaura" -> config.killauraKeyCode = keyCode;
            case "freeLook" -> config.freeLookKeyCode = keyCode;
            default -> {
            }
        }
    }

    private void ensureSelected(List<ModuleRef> modules) {
        boolean exists = modules.stream().anyMatch(module -> module.id().equals(selectedModule));
        if (!exists && !modules.isEmpty()) {
            selectedModule = modules.getFirst().id();
        }
    }

    private String moduleStatusSuffix(String id) {
        ModConfig config = ConfigManager.getConfig();
        boolean enabled = switch (id) {
            case "triggerbot" -> config.triggerbotEnabled;
            case "killaura" -> config.killauraEnabled;
            case "autototem" -> config.autoTotemEnabled;
            case "jumpcircle" -> config.jumpCircleEnabled;
            case "fullbright" -> config.fullbrightEnabled;
            case "playeresp" -> config.playerEspEnabled;
            case "lowfire" -> config.lowFireEnabled;
            case "targetesp" -> config.targetEspEnabled;
            case "arraylist" -> config.arrayListEnabled;
            case "watermark" -> config.watermarkEnabled;
            case "targethud" -> config.targetHudEnabled;
            case "attacklines" -> config.attackLinesEnabled;
            case "swinganim" -> config.swingAnimationEnabled;
            case "fly" -> config.flyEnabled;
            case "autosprint" -> config.autoSprintEnabled;
            case "inventorymove" -> config.inventoryMoveEnabled;
            case "freelook" -> config.freeLookToggle;
            default -> false;
        };
        return enabled ? "  [ON]" : "  [OFF]";
    }

    private void syncMousePosition(double scaledMouseX, double scaledMouseY) {
        double scale = client.getWindow().getScaleFactor();
        if (sharedInitialized) {
            ImGui.setCurrentContext(sharedContext);
            sharedGlfw.cursorPosCallback(client.getWindow().getHandle(), scaledMouseX * scale, scaledMouseY * scale);
        }
    }

    private void configureFonts(ImGuiIO io) {
        short[] glyphs = io.getFonts().getGlyphRangesCyrillic();
        Path fontPath = resolveFontPath();
        if (fontPath != null) {
            uiFont = io.getFonts().addFontFromFileTTF(fontPath.toString(), 18.0f, glyphs);
        }
        if (uiFont == null) {
            uiFont = io.getFonts().addFontDefault();
        }
        io.setFontDefault(uiFont);
    }

    private void configureStyle() {
        ImGui.styleColorsDark();
        ImGuiStyle style = ImGui.getStyle();
        style.setWindowPadding(WINDOW_PADDING, WINDOW_PADDING);
        style.setFramePadding(10.0f, 7.0f);
        style.setItemSpacing(10.0f, 9.0f);
        style.setItemInnerSpacing(8.0f, 6.0f);
        style.setWindowRounding(20.0f);
        style.setChildRounding(16.0f);
        style.setFrameRounding(11.0f);
        style.setPopupRounding(12.0f);
        style.setScrollbarRounding(12.0f);
        style.setGrabRounding(12.0f);
        style.setTabRounding(11.0f);
        style.setWindowBorderSize(1.0f);
        style.setChildBorderSize(1.0f);
        style.setFrameBorderSize(1.0f);
        style.setScrollbarSize(12.0f);

        style.setColor(ImGuiCol.Text, 239, 241, 246, 255);
        style.setColor(ImGuiCol.TextDisabled, 170, 174, 184, 255);
        style.setColor(ImGuiCol.WindowBg, 8, 9, 11, 224);
        style.setColor(ImGuiCol.ChildBg, 13, 14, 17, 210);
        style.setColor(ImGuiCol.PopupBg, 10, 11, 14, 226);
        style.setColor(ImGuiCol.Border, 74, 92, 132, 104);
        style.setColor(ImGuiCol.BorderShadow, 0, 0, 0, 0);
        style.setColor(ImGuiCol.FrameBg, 20, 22, 28, 214);
        style.setColor(ImGuiCol.FrameBgHovered, 30, 34, 44, 236);
        style.setColor(ImGuiCol.FrameBgActive, 41, 47, 63, 248);
        style.setColor(ImGuiCol.TitleBg, 7, 8, 10, 255);
        style.setColor(ImGuiCol.TitleBgActive, 12, 14, 18, 255);
        style.setColor(ImGuiCol.MenuBarBg, 12, 14, 18, 255);
        style.setColor(ImGuiCol.ScrollbarBg, 7, 8, 11, 138);
        style.setColor(ImGuiCol.ScrollbarGrab, 58, 70, 98, 220);
        style.setColor(ImGuiCol.ScrollbarGrabHovered, 76, 93, 131, 242);
        style.setColor(ImGuiCol.ScrollbarGrabActive, 96, 118, 166, 255);
        style.setColor(ImGuiCol.CheckMark, 131, 156, 216, 255);
        style.setColor(ImGuiCol.SliderGrab, 122, 145, 201, 244);
        style.setColor(ImGuiCol.SliderGrabActive, 143, 169, 230, 255);
        style.setColor(ImGuiCol.Button, 24, 27, 34, 214);
        style.setColor(ImGuiCol.ButtonHovered, 37, 43, 56, 238);
        style.setColor(ImGuiCol.ButtonActive, 49, 58, 77, 255);
        style.setColor(ImGuiCol.Header, 22, 25, 33, 220);
        style.setColor(ImGuiCol.HeaderHovered, 36, 42, 56, 240);
        style.setColor(ImGuiCol.HeaderActive, 48, 57, 76, 255);
        style.setColor(ImGuiCol.Separator, 70, 87, 126, 114);
        style.setColor(ImGuiCol.SeparatorHovered, 97, 121, 174, 192);
        style.setColor(ImGuiCol.SeparatorActive, 118, 144, 205, 255);
        style.setColor(ImGuiCol.ResizeGrip, 63, 78, 111, 92);
        style.setColor(ImGuiCol.ResizeGripHovered, 89, 111, 160, 176);
        style.setColor(ImGuiCol.ResizeGripActive, 114, 141, 202, 255);
        style.setColor(ImGuiCol.Tab, 18, 20, 27, 212);
        style.setColor(ImGuiCol.TabHovered, 35, 41, 54, 238);
        style.setColor(ImGuiCol.TabActive, 45, 53, 70, 255);
        style.setColor(ImGuiCol.TabUnfocused, 12, 14, 19, 198);
        style.setColor(ImGuiCol.TabUnfocusedActive, 28, 33, 45, 224);
    }

    private Path resolveFontPath() {
        String windowsRoot = System.getenv("WINDIR");
        if (windowsRoot == null || windowsRoot.isBlank()) {
            windowsRoot = "C:/Windows";
        }

        Path[] candidates = new Path[] {
                Path.of(windowsRoot, "Fonts", "segoeui.ttf"),
                Path.of(windowsRoot, "Fonts", "tahoma.ttf"),
                Path.of(windowsRoot, "Fonts", "arial.ttf")
        };

        for (Path path : candidates) {
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }

    private static float[] intToColor(int color) {
        return new float[] {
                ((color >>> 16) & 0xFF) / 255.0f,
                ((color >>> 8) & 0xFF) / 255.0f,
                (color & 0xFF) / 255.0f,
                ((color >>> 24) & 0xFF) / 255.0f
        };
    }

    private static int colorToInt(float[] color) {
        int a = Math.max(0, Math.min(255, Math.round(color[3] * 255.0f)));
        int r = Math.max(0, Math.min(255, Math.round(color[0] * 255.0f)));
        int g = Math.max(0, Math.min(255, Math.round(color[1] * 255.0f)));
        int b = Math.max(0, Math.min(255, Math.round(color[2] * 255.0f)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private String keyLabel(int keyCode) {
        if (keyCode == -1) {
            return "none";
        }

        try {
            return InputUtil.fromKeyCode(keyCode, 0).getLocalizedText().getString();
        } catch (Exception exception) {
            return "?";
        }
    }

    private static String swingStyleLabel(int style) {
        return switch (style) {
            case 1 -> "1 / Lunar";
            case 2 -> "2 / Eleven";
            case 3 -> "3 / Velocity";
            default -> "2 / Eleven";
        };
    }

    private static int nextSwingStyle(int style) {
        return switch (style) {
            case 1 -> 2;
            case 2 -> 3;
            default -> 1;
        };
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }

    @FunctionalInterface
    private interface IntSetter {
        void set(int value);
    }

    private record ModuleRef(String id, String title, String description) {
    }
}
