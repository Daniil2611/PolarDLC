package com.example.simpleaimbot.gui;

import com.example.simpleaimbot.client.ClientBranding;
import com.example.simpleaimbot.config.ConfigManager;
import com.example.simpleaimbot.config.ModConfig;
import com.example.simpleaimbot.utils.render.TextureStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AimbotSettingsScreen extends Screen {
    private static final int TAB_ATTACK = 0;
    private static final int TAB_VISUAL = 1;
    private static final int TAB_MOVEMENT = 2;

    private static final String CLIENT_NAME = ClientBranding.NAME;
    private static final String CLIENT_VERSION = ClientBranding.VERSION;
    private static final String[] TAB_DESCRIPTIONS_V2 = {
            "Боевые модули, ротация, AutoTotem и выбор целей.",
            "ESP, HUD, акцент интерфейса и анимации клиента.",
            "Передвижение, поведение камеры и быстрые утилиты."
    };

    private static final String[] TAB_LABELS = { "Attack", "Visual", "Movement" };
    private static final String[] TAB_DESCRIPTIONS = {
            "Боевые модули, ротация, AutoTotem и выбор целей.",
            "ESP, HUD, акцент интерфейса и анимации клиента.",
            "Передвижение, поведение камеры и быстрые утилиты."
    };
    private static final int SIDEBAR_SETTINGS_OFFSET = 188;
    private static final int MODULE_CARD_HEIGHT = 84;
    private static final int MODULE_CARD_GAP = 14;
    private static final int MODULE_ROW_HEIGHT = MODULE_CARD_HEIGHT + MODULE_CARD_GAP;
    private static final int SETTING_ROW_HEIGHT = 28;

    private boolean waitingForKey;
    private String waitingForFunction;
    private String selectedModuleId;
    private Text currentTooltip;

    private ModernButtonWidget triggerbotKeyButton;
    private ModernButtonWidget killauraKeyButton;
    private ModernButtonWidget freeLookKeyButton;

    private int currentTab = TAB_ATTACK;

    private int shellX;
    private int shellY;
    private int shellWidth;
    private int shellHeight;
    private int heroX;
    private int heroY;
    private int heroWidth;
    private int heroHeight;
    private int mainX;
    private int mainY;
    private int mainWidth;
    private int mainHeight;
    private int sidebarX;
    private int sidebarY;
    private int sidebarWidth;
    private int sidebarHeight;
    private int nextSettingY;
    private int moduleScroll;
    private int moduleMaxScroll;
    private int moduleRowCount;
    private int settingsScroll;
    private int settingsMaxScroll;
    private int settingsRowCount;

    private final List<ModernButtonWidget> tabButtons = new ArrayList<>();
    private final List<ModuleCardWidget> moduleButtons = new ArrayList<>();
    private final List<ModernButtonWidget> settingsButtons = new ArrayList<>();
    private final List<ModuleSpec> modules = new ArrayList<>();

    public AimbotSettingsScreen() {
        super(GuiTheme.uniform(CLIENT_NAME));
    }

    @Override
    protected void init() {
        super.init();
        computeLayout();
        clearTrackedWidgets();
        createModules();
        createTabButtons();
        ensureSelectedModule();
        rebuildModuleCards();
        rebuildSettingsButtons();
        updateTabButtons();
    }

    private void computeLayout() {
        shellX = 24;
        shellY = 20;
        shellWidth = width - 48;
        shellHeight = height - 40;

        heroX = shellX + 18;
        heroY = shellY + 18;
        heroWidth = shellWidth - 36;
        heroHeight = 98;

        sidebarWidth = Math.min(320, Math.max(258, shellWidth / 4));
        mainX = shellX + 18;
        mainY = heroY + heroHeight + 16;
        mainWidth = shellWidth - sidebarWidth - 54;
        mainHeight = shellHeight - (mainY - shellY) - 18;

        sidebarX = shellX + shellWidth - sidebarWidth - 18;
        sidebarY = mainY;
        sidebarHeight = mainHeight;
    }

    private void clearTrackedWidgets() {
        tabButtons.forEach(this::remove);
        moduleButtons.forEach(this::remove);
        settingsButtons.forEach(this::remove);
        tabButtons.clear();
        moduleButtons.clear();
        settingsButtons.clear();
    }

    private void createModules() {
        modules.clear();

        modules.add(new ModuleSpec(
                "triggerbot",
                TAB_ATTACK,
                "Triggerbot",
                "Бьет цель, когда прицел уже наведен на игрока.",
                () -> statusLabel(ConfigManager.getConfig().triggerbotEnabled),
                () -> ConfigManager.getConfig().triggerbotEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.triggerbotEnabled = !config.triggerbotEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Triggerbot",
                            () -> ConfigManager.getConfig().triggerbotEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.triggerbotEnabled = !config.triggerbotEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить Triggerbot."
                    ));

                    int rowY = screen.nextSettingY;
                    int resetWidth = 92;
                    int gap = 8;
                    screen.pushSettingRow(
                            screen.createKeyButton(screen.settingsX(), rowY, screen.settingsWidth() - resetWidth - gap,
                                    ConfigManager.getConfig().triggerbotKeyCode, "triggerbot",
                                    "Назначить клавишу для Triggerbot."),
                            screen.createResetButton(screen.settingsX() + screen.settingsWidth() - resetWidth, rowY, resetWidth,
                                    "triggerbot", "Сбросить клавишу Triggerbot.")
                    );
                },
                GuiTheme.uniform("ЛКМ переключает Triggerbot. ПКМ открывает его настройки."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "killaura",
                TAB_ATTACK,
                "KillAura",
                "Автоматическая ротация и атака по выбранной цели.",
                () -> statusLabel(ConfigManager.getConfig().killauraEnabled),
                () -> ConfigManager.getConfig().killauraEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.killauraEnabled = !config.killauraEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "KillAura",
                            () -> ConfigManager.getConfig().killauraEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.killauraEnabled = !config.killauraEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить KillAura."
                    ));

                    int rowY = screen.nextSettingY;
                    int resetWidth = 92;
                    int gap = 8;
                    screen.pushSettingRow(
                            screen.createKeyButton(screen.settingsX(), rowY, screen.settingsWidth() - resetWidth - gap,
                                    ConfigManager.getConfig().killauraKeyCode, "killaura",
                                    "Назначить клавишу для KillAura."),
                            screen.createResetButton(screen.settingsX() + screen.settingsWidth() - resetWidth, rowY, resetWidth,
                                    "killaura", "Сбросить клавишу KillAura.")
                    );

                    screen.pushSetting(screen.createValueButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            () -> "Extra range: " + ConfigManager.getConfig().extraRange,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.extraRange = (config.extraRange + 1) % 4;
                                ConfigManager.save();
                            },
                            "Дополнительная дистанция сопровождения цели.",
                            ModernButtonWidget.Variant.PRIMARY
                    ));
                    screen.pushSetting(screen.createValueButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            () -> "Target sort: " + ConfigManager.getConfig().targetSort.name(),
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                ModConfig.TargetSort[] values = ModConfig.TargetSort.values();
                                config.targetSort = values[(config.targetSort.ordinal() + 1) % values.length];
                                ConfigManager.save();
                            },
                            "Приоритет выбора цели по дистанции, здоровью или FOV.",
                            ModernButtonWidget.Variant.PRIMARY
                    ));
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Criticals",
                            () -> ConfigManager.getConfig().criticals,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.criticals = !config.criticals;
                                ConfigManager.save();
                            },
                            "Оставлять удары только в окне критической атаки."
                    ));
                    screen.pushSetting(screen.createValueButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            () -> "Rotation mode: " + ConfigManager.getConfig().rotationMode.name(),
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                ModConfig.RotationMode[] values = ModConfig.RotationMode.values();
                                config.rotationMode = values[(config.rotationMode.ordinal() + 1) % values.length];
                                ConfigManager.save();
                            },
                            "Выбрать режим ротации Legit, Rage или Funtime Snap.",
                            ModernButtonWidget.Variant.PRIMARY
                    ));
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Wall check",
                            () -> ConfigManager.getConfig().wallCheckEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.wallCheckEnabled = !config.wallCheckEnabled;
                                ConfigManager.save();
                            },
                            "Требовать прямую видимость цели."
                    ));
                },
                GuiTheme.uniform("ЛКМ переключает KillAura. ПКМ открывает настройки ротации и выбора цели."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "autototem",
                TAB_ATTACK,
                "AutoTotem",
                "Поддерживает тотем в левой руке при низком здоровье.",
                () -> statusLabel(ConfigManager.getConfig().autoTotemEnabled),
                () -> ConfigManager.getConfig().autoTotemEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.autoTotemEnabled = !config.autoTotemEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "AutoTotem",
                            () -> ConfigManager.getConfig().autoTotemEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.autoTotemEnabled = !config.autoTotemEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить AutoTotem."
                    ));
                    screen.pushSetting(screen.createValueButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            () -> "HP: " + ConfigManager.getConfig().autoTotemThreshold,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.autoTotemThreshold = config.autoTotemThreshold >= 20 ? 1 : config.autoTotemThreshold + 1;
                                ConfigManager.save();
                            },
                            "Порог здоровья для срабатывания AutoTotem.",
                            ModernButtonWidget.Variant.SECONDARY
                    ));
                    screen.pushSetting(screen.createValueButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            () -> "Delay: " + ConfigManager.getConfig().autoTotemDelay,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.autoTotemDelay = config.autoTotemDelay >= 20 ? 0 : config.autoTotemDelay + 1;
                                ConfigManager.save();
                            },
                            "Задержка между перестановками тотема.",
                            ModernButtonWidget.Variant.SECONDARY
                    ));
                },
                GuiTheme.uniform("ЛКМ переключает AutoTotem. ПКМ открывает порог и задержку."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "jumpcircle",
                TAB_VISUAL,
                "Jump Circle",
                "Эффект приземления с несколькими стилями круга.",
                () -> statusLabel(ConfigManager.getConfig().jumpCircleEnabled),
                () -> ConfigManager.getConfig().jumpCircleEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.jumpCircleEnabled = !config.jumpCircleEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Jump Circle",
                            () -> ConfigManager.getConfig().jumpCircleEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.jumpCircleEnabled = !config.jumpCircleEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить Jump Circle."
                    ));
                    screen.pushSetting(screen.createValueButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            () -> "Mode: " + ConfigManager.getConfig().jumpCircleMode.name(),
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                ModConfig.JumpCircleMode[] values = ModConfig.JumpCircleMode.values();
                                config.jumpCircleMode = values[(config.jumpCircleMode.ordinal() + 1) % values.length];
                                ConfigManager.save();
                            },
                            "Переключить стиль эффекта Jump Circle.",
                            ModernButtonWidget.Variant.PRIMARY
                    ));
                    screen.pushSetting(screen.createColorButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Цвет", ConfigManager.getConfig().jumpCircleColor,
                            "Открыть палитру Jump Circle.",
                            color -> ConfigManager.getConfig().jumpCircleColor = color
                    ));
                    screen.pushSetting(screen.createValueButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            () -> "Radius: " + ConfigManager.getConfig().jumpCircleRadius,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.jumpCircleRadius = config.jumpCircleRadius >= 7 ? 2 : config.jumpCircleRadius + 1;
                                ConfigManager.save();
                            },
                            "Изменить базовый радиус эффекта.",
                            ModernButtonWidget.Variant.SECONDARY
                    ));
                },
                GuiTheme.uniform("ЛКМ переключает Jump Circle. ПКМ открывает стиль, цвет и радиус."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "fullbright",
                TAB_VISUAL,
                "Fullbright",
                "Поддерживает яркость сцены без темных зон.",
                () -> statusLabel(ConfigManager.getConfig().fullbrightEnabled),
                () -> ConfigManager.getConfig().fullbrightEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.fullbrightEnabled = !config.fullbrightEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Fullbright",
                            () -> ConfigManager.getConfig().fullbrightEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.fullbrightEnabled = !config.fullbrightEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить Fullbright."
                    ));
                    screen.pushSetting(screen.createInfoButton("Дополнительных настроек для Fullbright пока нет."));
                },
                GuiTheme.uniform("ЛКМ переключает Fullbright. ПКМ показывает доступные параметры."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "playeresp",
                TAB_VISUAL,
                "Player ESP",
                "Контур или заливка игроков в мире.",
                () -> statusLabel(ConfigManager.getConfig().playerEspEnabled),
                () -> ConfigManager.getConfig().playerEspEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.playerEspEnabled = !config.playerEspEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Player ESP",
                            () -> ConfigManager.getConfig().playerEspEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.playerEspEnabled = !config.playerEspEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить Player ESP."
                    ));
                    screen.pushSetting(screen.createColorButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Цвет", ConfigManager.getConfig().espColor,
                            "Открыть палитру Player ESP.",
                            color -> ConfigManager.getConfig().espColor = color
                    ));
                    screen.pushSetting(screen.createValueButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            () -> yesNoLabel("Fill", ConfigManager.getConfig().playerEspFilled),
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.playerEspFilled = !config.playerEspFilled;
                                ConfigManager.save();
                            },
                            "Включить или выключить заливку Player ESP.",
                            ModernButtonWidget.Variant.SECONDARY
                    ));
                },
                GuiTheme.uniform("ЛКМ переключает Player ESP. ПКМ открывает цвет и заливку."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "lowfire",
                TAB_VISUAL,
                "Low Fire",
                "Уменьшает визуальный огонь на экране игрока.",
                () -> statusLabel(ConfigManager.getConfig().lowFireEnabled),
                () -> ConfigManager.getConfig().lowFireEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.lowFireEnabled = !config.lowFireEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Low Fire",
                            () -> ConfigManager.getConfig().lowFireEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.lowFireEnabled = !config.lowFireEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить Low Fire."
                    ));
                    screen.pushSetting(screen.createInfoButton("Low Fire не требует дополнительных настроек."));
                },
                GuiTheme.uniform("ЛКМ переключает Low Fire. ПКМ открывает карточку модуля."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "targetesp",
                TAB_VISUAL,
                "Target ESP",
                "Подсветка текущей цели KillAura с разными режимами.",
                () -> statusLabel(ConfigManager.getConfig().targetEspEnabled),
                () -> ConfigManager.getConfig().targetEspEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.targetEspEnabled = !config.targetEspEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Target ESP",
                            () -> ConfigManager.getConfig().targetEspEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.targetEspEnabled = !config.targetEspEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить Target ESP."
                    ));
                    screen.pushSetting(screen.createValueButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            () -> "Mode: " + ConfigManager.getConfig().targetEspMode.name(),
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                ModConfig.TargetEspMode[] values = ModConfig.TargetEspMode.values();
                                config.targetEspMode = values[(config.targetEspMode.ordinal() + 1) % values.length];
                                ConfigManager.save();
                            },
                            "Переключить стиль Target ESP.",
                            ModernButtonWidget.Variant.PRIMARY
                    ));
                    screen.pushSetting(screen.createColorButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Цвет", ConfigManager.getConfig().targetEspColor,
                            "Открыть палитру Target ESP.",
                            color -> ConfigManager.getConfig().targetEspColor = color
                    ));
                },
                GuiTheme.uniform("ЛКМ переключает Target ESP. ПКМ открывает режим и цвет."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "arraylist",
                TAB_VISUAL,
                "Array List",
                "Список активных модулей с новым оформлением.",
                () -> statusLabel(ConfigManager.getConfig().arrayListEnabled),
                () -> ConfigManager.getConfig().arrayListEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.arrayListEnabled = !config.arrayListEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Array List",
                            () -> ConfigManager.getConfig().arrayListEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.arrayListEnabled = !config.arrayListEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить Array List."
                    ));
                    screen.pushSetting(screen.createValueButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            () -> ConfigManager.getConfig().arrayListRight ? "Позиция: справа" : "Позиция: слева",
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.arrayListRight = !config.arrayListRight;
                                ConfigManager.save();
                            },
                            "Выбрать сторону экрана для Array List.",
                            ModernButtonWidget.Variant.SECONDARY
                    ));
                    screen.pushSetting(screen.createColorButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Цвет", ConfigManager.getConfig().arrayListGlowColor,
                            "Открыть палитру Array List.",
                            color -> ConfigManager.getConfig().arrayListGlowColor = color
                    ));
                },
                GuiTheme.uniform("ЛКМ переключает Array List. ПКМ открывает позицию и цвет."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "watermark",
                TAB_VISUAL,
                "Watermark",
                "Блок в левом верхнем углу с иконкой P, версией, FPS и ping.",
                () -> statusLabel(ConfigManager.getConfig().watermarkEnabled),
                () -> ConfigManager.getConfig().watermarkEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.watermarkEnabled = !config.watermarkEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Watermark",
                            () -> ConfigManager.getConfig().watermarkEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.watermarkEnabled = !config.watermarkEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить Watermark."
                    ));
                    screen.pushSetting(screen.createInfoButton("Показывает логотип клиента, версию, FPS и ping."));
                },
                GuiTheme.uniform("ЛКМ переключает Watermark. ПКМ открывает описание модуля."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "targethud",
                TAB_VISUAL,
                "Target HUD",
                "Карточка цели в стиле клиента с акцентной рамкой.",
                () -> statusLabel(ConfigManager.getConfig().targetHudEnabled),
                () -> ConfigManager.getConfig().targetHudEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.targetHudEnabled = !config.targetHudEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Target HUD",
                            () -> ConfigManager.getConfig().targetHudEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.targetHudEnabled = !config.targetHudEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить Target HUD."
                    ));
                    screen.pushSetting(screen.createColorButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Цвет", ConfigManager.getConfig().targetHudGlowColor,
                            "Открыть палитру рамки Target HUD.",
                            color -> ConfigManager.getConfig().targetHudGlowColor = color
                    ));
                },
                GuiTheme.uniform("ЛКМ переключает Target HUD. ПКМ открывает цвет рамки."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "attacklines",
                TAB_VISUAL,
                "Attack Lines",
                "Кольцо радиуса удара вокруг игрока.",
                () -> statusLabel(ConfigManager.getConfig().attackLinesEnabled),
                () -> ConfigManager.getConfig().attackLinesEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.attackLinesEnabled = !config.attackLinesEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Attack Lines",
                            () -> ConfigManager.getConfig().attackLinesEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.attackLinesEnabled = !config.attackLinesEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить Attack Lines."
                    ));
                    screen.pushSetting(screen.createInfoButton("Attack Lines использует текущую боевую дистанцию KillAura."));
                },
                GuiTheme.uniform("ЛКМ переключает Attack Lines. ПКМ открывает карточку модуля."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "swinganim",
                TAB_VISUAL,
                "Swing Anim",
                "Кастомная анимация удара в стиле Eleven.",
                () -> statusLabel(ConfigManager.getConfig().swingAnimationEnabled),
                () -> ConfigManager.getConfig().swingAnimationEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.swingAnimationEnabled = !config.swingAnimationEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Swing Anim",
                            () -> ConfigManager.getConfig().swingAnimationEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.swingAnimationEnabled = !config.swingAnimationEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить кастомную анимацию."
                    ));
                    screen.pushSetting(screen.createValueButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            () -> "Style: " + swingStyleLabel(ConfigManager.getConfig().swingAnimationStyle),
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.swingAnimationStyle = nextSwingStyle(config.swingAnimationStyle);
                                ConfigManager.save();
                            },
                            "Переключить стиль анимации удара.",
                            ModernButtonWidget.Variant.PRIMARY
                    ));
                    screen.pushSetting(screen.createInfoButton("Styles: Lunar, Eleven, Velocity"));
                },
                GuiTheme.uniform("ЛКМ переключает Swing Anim. ПКМ открывает карточку с фиксированным стилем 3."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "fly",
                TAB_MOVEMENT,
                "Fly",
                "Переключает клиентский режим полета.",
                () -> statusLabel(ConfigManager.getConfig().flyEnabled),
                () -> ConfigManager.getConfig().flyEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.flyEnabled = !config.flyEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "Fly",
                            () -> ConfigManager.getConfig().flyEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.flyEnabled = !config.flyEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить Fly."
                    ));
                    screen.pushSetting(screen.createInfoButton("У Fly в этом билде нет дополнительных параметров."));
                },
                GuiTheme.uniform("ЛКМ переключает Fly. ПКМ открывает карточку модуля."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "autosprint",
                TAB_MOVEMENT,
                "AutoSprint",
                "Автоматически включает спринт при движении.",
                () -> statusLabel(ConfigManager.getConfig().autoSprintEnabled),
                () -> ConfigManager.getConfig().autoSprintEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.autoSprintEnabled = !config.autoSprintEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "AutoSprint",
                            () -> ConfigManager.getConfig().autoSprintEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.autoSprintEnabled = !config.autoSprintEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить AutoSprint."
                    ));
                    screen.pushSetting(screen.createInfoButton("AutoSprint не имеет отдельных настроек."));
                },
                GuiTheme.uniform("ЛКМ переключает AutoSprint. ПКМ открывает карточку модуля."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "inventorymove",
                TAB_MOVEMENT,
                "InventoryMove",
                "Разрешает движение при открытых контейнерах и экранах.",
                () -> statusLabel(ConfigManager.getConfig().inventoryMoveEnabled),
                () -> ConfigManager.getConfig().inventoryMoveEnabled,
                screen -> {
                    ModConfig config = ConfigManager.getConfig();
                    config.inventoryMoveEnabled = !config.inventoryMoveEnabled;
                    ConfigManager.save();
                },
                screen -> {
                    screen.pushSetting(screen.createDynamicToggleButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            "InventoryMove",
                            () -> ConfigManager.getConfig().inventoryMoveEnabled,
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.inventoryMoveEnabled = !config.inventoryMoveEnabled;
                                ConfigManager.save();
                            },
                            "Включить или выключить InventoryMove."
                    ));
                    screen.pushSetting(screen.createInfoButton("Параметры InventoryMove в GUI не расширяются."));
                },
                GuiTheme.uniform("ЛКМ переключает InventoryMove. ПКМ открывает карточку модуля."),
                "ЛКМ: ВКЛ | ПКМ: настройки"
        ));

        modules.add(new ModuleSpec(
                "freelook",
                TAB_MOVEMENT,
                "FreeLook",
                "Независимая камера с режимом hold или toggle.",
                () -> ConfigManager.getConfig().freeLookToggle ? "TOGGLE" : "HOLD",
                () -> ConfigManager.getConfig().freeLookToggle,
                screen -> screen.selectModule("freelook"),
                screen -> {
                    screen.pushSetting(screen.createValueButton(
                            screen.settingsX(), screen.nextSettingY, screen.settingsWidth(),
                            () -> "FreeLook: " + (ConfigManager.getConfig().freeLookToggle ? "TOGGLE" : "HOLD"),
                            () -> {
                                ModConfig config = ConfigManager.getConfig();
                                config.freeLookToggle = !config.freeLookToggle;
                                ConfigManager.save();
                            },
                            "Выбрать режим переключения или удержания.",
                            ModernButtonWidget.Variant.PRIMARY
                    ));

                    int rowY = screen.nextSettingY;
                    int resetWidth = 92;
                    int gap = 8;
                    screen.pushSettingRow(
                            screen.createKeyButton(screen.settingsX(), rowY, screen.settingsWidth() - resetWidth - gap,
                                    ConfigManager.getConfig().freeLookKeyCode, "freeLook",
                                    "Назначить клавишу FreeLook."),
                            screen.createResetButton(screen.settingsX() + screen.settingsWidth() - resetWidth, rowY, resetWidth,
                                    "freeLook", "Сбросить клавишу FreeLook.")
                    );
                },
                GuiTheme.uniform("ПКМ открывает режим и клавишу FreeLook. ЛКМ просто фокусирует модуль."),
                "ЛКМ: открыть | ПКМ: настройки"
        ));

    }

    private void createTabButtons() {
        int tabGap = 10;
        int tabY = mainY + 38;
        int tabWidth = (mainWidth - 36 - tabGap * (TAB_LABELS.length - 1)) / TAB_LABELS.length;
        int x = mainX + 18;

        for (int i = 0; i < TAB_LABELS.length; i++) {
            int tabIndex = i;
            ModernButtonWidget button = new ModernButtonWidget(x, tabY, tabWidth, 24, GuiTheme.uniform(TAB_LABELS[i]), widget -> setTab(tabIndex))
                    .withVariant(ModernButtonWidget.Variant.TAB)
                    .withTooltip(GuiTheme.uniform(TAB_DESCRIPTIONS_V2[i]))
                    .withActive(i == currentTab);
            tabButtons.add(button);
            addDrawableChild(button);
            x += tabWidth + tabGap;
        }
    }

    private void setTab(int tab) {
        currentTab = tab;
        moduleScroll = 0;
        settingsScroll = 0;
        ensureSelectedModule();
        rebuildModuleCards();
        rebuildSettingsButtons();
        updateTabButtons();
    }

    private void updateTabButtons() {
        for (int i = 0; i < tabButtons.size(); i++) {
            tabButtons.get(i).setVisualActive(i == currentTab);
        }
    }

    private void ensureSelectedModule() {
        List<ModuleSpec> visibleModules = getModulesForTab(currentTab);
        if (visibleModules.isEmpty()) {
            selectedModuleId = null;
            return;
        }

        if (selectedModuleId == null || visibleModules.stream().noneMatch(module -> module.id().equals(selectedModuleId))) {
            selectedModuleId = visibleModules.getFirst().id();
        }
    }

    private void rebuildModuleCards() {
        moduleButtons.forEach(this::remove);
        moduleButtons.clear();

        List<ModuleSpec> visibleModules = getModulesForTab(currentTab);
        int cardsPerRow = moduleColumns();
        int cardWidth = (mainWidth - 36 - MODULE_CARD_GAP * (cardsPerRow - 1)) / cardsPerRow;
        moduleRowCount = visibleModules.isEmpty() ? 0 : ((visibleModules.size() - 1) / cardsPerRow) + 1;
        int visibleRows = Math.max(1, (mainContentHeight() + MODULE_CARD_GAP) / MODULE_ROW_HEIGHT);
        moduleMaxScroll = Math.max(0, (moduleRowCount - visibleRows) * MODULE_ROW_HEIGHT);
        moduleScroll = MathHelper.clamp(moduleScroll, 0, moduleMaxScroll);
        int startX = mainX + 18;
        int startY = mainContentTop() - moduleScroll;

        for (int index = 0; index < visibleModules.size(); index++) {
            ModuleSpec spec = visibleModules.get(index);
            int column = index % cardsPerRow;
            int row = index / cardsPerRow;
            int x = startX + column * (cardWidth + MODULE_CARD_GAP);
            int y = startY + row * MODULE_ROW_HEIGHT;

            ModuleCardWidget card = new ModuleCardWidget(
                    x, y, cardWidth, MODULE_CARD_HEIGHT,
                    spec.title(),
                    spec.description(),
                    spec.statusSupplier(),
                    spec.activeSupplier(),
                    spec.actionHint(),
                    widget -> {
                        spec.primaryAction().accept(this);
                        selectModule(spec.id());
                        rebuildSettingsButtons();
                    },
                    widget -> {
                        selectModule(spec.id());
                        rebuildSettingsButtons();
                    },
                    spec.tooltip()
            );
            card.setSelected(spec.id().equals(selectedModuleId));
            card.visible = isWithinMainViewport(y, MODULE_CARD_HEIGHT);
            moduleButtons.add(card);
            addDrawableChild(card);
        }
    }

    private void selectModule(String moduleId) {
        selectedModuleId = moduleId;
        settingsScroll = 0;
        for (int i = 0; i < moduleButtons.size(); i++) {
            ModuleCardWidget card = moduleButtons.get(i);
            ModuleSpec spec = getModulesForTab(currentTab).get(i);
            card.setSelected(spec.id().equals(moduleId));
        }
    }

    private void rebuildSettingsButtons() {
        settingsButtons.forEach(this::remove);
        settingsButtons.clear();
        settingsRowCount = 0;
        int requestedScroll = settingsScroll;
        nextSettingY = sidebarSettingsStartY() - requestedScroll;

        ModuleSpec selectedModule = getSelectedModule();
        if (selectedModule != null) {
            selectedModule.settingsBuilder().accept(this);
        }

        int visibleRows = Math.max(1, settingsViewportHeight() / SETTING_ROW_HEIGHT);
        settingsMaxScroll = Math.max(0, (settingsRowCount - visibleRows) * SETTING_ROW_HEIGHT);
        int clampedScroll = MathHelper.clamp(requestedScroll, 0, settingsMaxScroll);
        if (clampedScroll != requestedScroll) {
            settingsScroll = clampedScroll;
            rebuildSettingsButtons();
            return;
        }

        for (ModernButtonWidget button : settingsButtons) {
            button.visible = isWithinSettingsViewport(button.getY(), button.getHeight());
        }
    }

    private List<ModuleSpec> getModulesForTab(int tab) {
        List<ModuleSpec> visible = new ArrayList<>();
        for (ModuleSpec module : modules) {
            if (module.tab() == tab) {
                visible.add(module);
            }
        }
        return visible;
    }

    private ModuleSpec getSelectedModule() {
        if (selectedModuleId == null) {
            return null;
        }

        for (ModuleSpec module : modules) {
            if (module.id().equals(selectedModuleId)) {
                return module;
            }
        }
        return null;
    }

    private int settingsX() {
        return sidebarX + 16;
    }

    private int settingsWidth() {
        return sidebarWidth - 32;
    }

    private int moduleColumns() {
        if (mainWidth >= 900) {
            return 3;
        }
        if (mainWidth >= 620) {
            return 2;
        }
        return 1;
    }

    private int sidebarSettingsStartY() {
        return sidebarY + SIDEBAR_SETTINGS_OFFSET;
    }

    private int mainContentTop() {
        return mainY + 76;
    }

    private int mainContentBottom() {
        return shellY + shellHeight - 44;
    }

    private int mainContentHeight() {
        return Math.max(0, mainContentBottom() - mainContentTop());
    }

    private int settingsViewportBottom() {
        return sidebarY + sidebarHeight - 16;
    }

    private int settingsViewportHeight() {
        return Math.max(0, settingsViewportBottom() - sidebarSettingsStartY());
    }

    private boolean isWithinMainViewport(int y, int height) {
        return y >= mainContentTop() && y + height <= mainContentBottom();
    }

    private boolean isWithinSettingsViewport(int y, int height) {
        return y >= sidebarSettingsStartY() && y + height <= settingsViewportBottom();
    }

    private boolean isInsideMainScrollRegion(double mouseX, double mouseY) {
        return mouseX >= mainX + 18
                && mouseX <= mainX + mainWidth - 18
                && mouseY >= mainContentTop()
                && mouseY <= mainContentBottom();
    }

    private boolean isInsideSettingsScrollRegion(double mouseX, double mouseY) {
        return mouseX >= sidebarX + 16
                && mouseX <= sidebarX + sidebarWidth - 16
                && mouseY >= sidebarSettingsStartY()
                && mouseY <= settingsViewportBottom();
    }

    private void pushSetting(ModernButtonWidget button) {
        settingsButtons.add(button);
        addDrawableChild(button);
        settingsRowCount++;
        nextSettingY += SETTING_ROW_HEIGHT;
    }

    private void pushSettingRow(ModernButtonWidget left, ModernButtonWidget right) {
        settingsButtons.add(left);
        settingsButtons.add(right);
        addDrawableChild(left);
        addDrawableChild(right);
        settingsRowCount++;
        nextSettingY += SETTING_ROW_HEIGHT;
    }

    private ModernButtonWidget createDynamicToggleButton(int x, int y, int width, String label,
                                                         BooleanSupplier stateSupplier, Runnable action, String tooltip) {
        return new ModernButtonWidget(x, y, width, 24, GuiTheme.uniform(onOffLabel(label, stateSupplier.getAsBoolean())), button -> {
            action.run();
            boolean enabled = stateSupplier.getAsBoolean();
            button.setMessage(GuiTheme.uniform(onOffLabel(label, enabled)));
            setActiveState(button, enabled);
            rebuildSettingsButtons();
        }).withVariant(ModernButtonWidget.Variant.PRIMARY)
                .withTooltip(GuiTheme.uniform(tooltip))
                .withActive(stateSupplier.getAsBoolean());
    }

    private ModernButtonWidget createValueButton(int x, int y, int width, Supplier<String> labelSupplier, Runnable action,
                                                 String tooltip, ModernButtonWidget.Variant variant) {
        return new ModernButtonWidget(x, y, width, 24, GuiTheme.uniform(labelSupplier.get()), button -> {
            action.run();
            button.setMessage(GuiTheme.uniform(labelSupplier.get()));
            rebuildSettingsButtons();
        }).withVariant(variant)
                .withTooltip(GuiTheme.uniform(tooltip));
    }

    private ModernButtonWidget createKeyButton(int x, int y, int width, int keyCode, String function, String tooltip) {
        ModernButtonWidget button = new ModernButtonWidget(x, y, width, 24, GuiTheme.uniform(keyLabel(keyCode)), widget -> {
            waitingForKey = true;
            waitingForFunction = function;
            widget.setMessage(GuiTheme.uniform("Нажми клавишу..."));
        }).withVariant(ModernButtonWidget.Variant.SECONDARY)
                .withTooltip(GuiTheme.uniform(tooltip));

        switch (function) {
            case "triggerbot" -> triggerbotKeyButton = button;
            case "killaura" -> killauraKeyButton = button;
            case "freeLook" -> freeLookKeyButton = button;
            default -> {
            }
        }
        return button;
    }

    private ModernButtonWidget createResetButton(int x, int y, int width, String function, String tooltip) {
        return new ModernButtonWidget(x, y, width, 24, GuiTheme.uniform("Сброс"), button -> {
            ModConfig config = ConfigManager.getConfig();
            switch (function) {
                case "triggerbot" -> {
                    config.triggerbotKeyCode = -1;
                    if (triggerbotKeyButton != null) {
                        triggerbotKeyButton.setMessage(GuiTheme.uniform(keyLabel(-1)));
                    }
                }
                case "killaura" -> {
                    config.killauraKeyCode = -1;
                    if (killauraKeyButton != null) {
                        killauraKeyButton.setMessage(GuiTheme.uniform(keyLabel(-1)));
                    }
                }
                case "freeLook" -> {
                    config.freeLookKeyCode = -1;
                    if (freeLookKeyButton != null) {
                        freeLookKeyButton.setMessage(GuiTheme.uniform(keyLabel(-1)));
                    }
                }
                default -> {
                    return;
                }
            }
            ConfigManager.save();
        }).withVariant(ModernButtonWidget.Variant.UTILITY)
                .withTooltip(GuiTheme.uniform(tooltip));
    }

    private ModernButtonWidget createColorButton(int x, int y, int width, String label, int currentColor, String tooltip,
                                                 HsvColorPickerScreen.ColorConsumer consumer) {
        return new ModernButtonWidget(x, y, width, 24, GuiTheme.uniform(label), button ->
                MinecraftClient.getInstance().setScreen(new HsvColorPickerScreen(color -> {
                    consumer.accept(color);
                    ConfigManager.save();
                }, this, currentColor)))
                .withVariant(ModernButtonWidget.Variant.COLOR)
                .withTooltip(GuiTheme.uniform(tooltip))
                .withAccentColor(currentColor);
    }

    private ModernButtonWidget createInfoButton(String message) {
        ModernButtonWidget button = new ModernButtonWidget(settingsX(), nextSettingY, settingsWidth(), 24, GuiTheme.uniform(message), widget -> { })
                .withVariant(ModernButtonWidget.Variant.SECONDARY)
                .withTooltip(GuiTheme.uniform(message));
        button.active = false;
        return button;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        GuiTheme.drawBackdrop(context, width, height);
        drawFrame(context);
        currentTooltip = resolveTooltip(mouseX, mouseY);
        drawHeaderV2(context);
        drawMainPanelTextV2(context);
        drawSidebarV2(context);
        drawScrollBars(context);
        super.render(context, mouseX, mouseY, delta);
        drawFooterV2(context);
    }

    private void drawFrame(DrawContext context) {
        GuiTheme.drawCard(context, shellX, shellY, shellWidth, shellHeight, GuiTheme.withAlpha(GuiTheme.panel(), 208), GuiTheme.panelEdge(), 18);
        GuiTheme.drawCard(context, heroX, heroY, heroWidth, heroHeight, GuiTheme.withAlpha(GuiTheme.panelAlt(), 226), GuiTheme.withAlpha(GuiTheme.accentBright(), 90), 16);
        GuiTheme.drawCard(context, mainX, mainY, mainWidth, mainHeight, GuiTheme.withAlpha(GuiTheme.panel(), 188), GuiTheme.withAlpha(GuiTheme.text(), 20), 16);
        GuiTheme.drawCard(context, sidebarX, sidebarY, sidebarWidth, sidebarHeight, GuiTheme.withAlpha(GuiTheme.panelAlt(), 198), GuiTheme.withAlpha(GuiTheme.accent(), 56), 16);
    }

    private void drawHeader(DrawContext context) {
        context.drawTextWithShadow(textRenderer, GuiTheme.uniform(CLIENT_NAME), heroX + 18, heroY + 16, GuiTheme.text());
        context.drawText(textRenderer, GuiTheme.uniform(textRenderer.trimToWidth("Модульная панель с быстрым открытием настроек по ПКМ.", heroWidth - 220)), heroX + 18, heroY + 34, GuiTheme.mutedText(), false);
        context.drawText(textRenderer, GuiTheme.uniform("Версия " + CLIENT_VERSION), heroX + 18, heroY + 52, GuiTheme.withAlpha(GuiTheme.accentBright(), 240), false);

        int badgeWidth = 170;
        int badgeX = heroX + heroWidth - badgeWidth - 18;
        int badgeY = heroY + 18;
        GuiTheme.drawCard(context, badgeX, badgeY, badgeWidth, 44,
                GuiTheme.withAlpha(GuiTheme.accentSoft(), 164),
                GuiTheme.withAlpha(GuiTheme.accentBright(), 134),
                14);
        context.drawText(textRenderer, GuiTheme.uniform("Selected tab"), badgeX + 14, badgeY + 8, GuiTheme.mutedText(), false);
        context.drawText(textRenderer, GuiTheme.uniform(TAB_LABELS[currentTab]), badgeX + 14, badgeY + 24, GuiTheme.text(), false);
        context.drawText(textRenderer, GuiTheme.uniform(CLIENT_VERSION), badgeX + badgeWidth - 46, badgeY + 24,
                GuiTheme.withAlpha(GuiTheme.accentBright(), 244), false);
    }

    private void drawMainPanelText(DrawContext context) {
        context.drawTextWithShadow(textRenderer, GuiTheme.uniform("Модули"), mainX + 18, mainY + 34, GuiTheme.text());
        context.drawText(textRenderer, GuiTheme.uniform(textRenderer.trimToWidth("ЛКМ переключает модуль, ПКМ открывает его настройки справа.", mainWidth - 120)),
                mainX + 78, mainY + 34, GuiTheme.mutedText(), false);
    }

    private void drawSidebar(DrawContext context) {
        int textX = sidebarX + 16;
        int y = sidebarY + 18;
        int infoBottom = sidebarSettingsStartY() - 12;

        ModuleSpec selectedModule = getSelectedModule();
        String title = selectedModule != null ? selectedModule.title() : "Нет модуля";
        String description = selectedModule != null ? selectedModule.description() : "Выбери модуль из списка слева.";

        context.drawTextWithShadow(textRenderer, GuiTheme.uniform("Настройки"), textX, y, GuiTheme.text());
        y += 18;
        context.drawText(textRenderer, GuiTheme.uniform("Текущий модуль"), textX, y, GuiTheme.mutedText(), false);
        y += 12;
        context.drawText(textRenderer, GuiTheme.uniform(textRenderer.trimToWidth(title, sidebarWidth - 32)), textX, y, GuiTheme.withAlpha(GuiTheme.accentBright(), 245), false);
        y += 18;
        y = drawWrappedTextLimited(context, GuiTheme.uniform(description), textX, y, sidebarWidth - 32, GuiTheme.text(), 2) + 10;

        context.drawText(textRenderer, GuiTheme.uniform(waitingForKey ? "Ожидание клавиши" : "Подсказка"), textX, y, GuiTheme.mutedText(), false);
        y += 12;
        Text tooltip = currentTooltip != null ? currentTooltip : GuiTheme.uniform("Наведи курсор на карточку или кнопку, чтобы увидеть описание.");
        y = drawWrappedTextLimited(context, tooltip, textX, y, sidebarWidth - 32, GuiTheme.text(), 2) + 10;

        int sectionY = Math.min(infoBottom, Math.max(y + 2, sidebarSettingsStartY() - 24));
        GuiTheme.drawCard(context, textX, sectionY, sidebarWidth - 32, 20,
                GuiTheme.withAlpha(GuiTheme.panel(), 186), GuiTheme.withAlpha(GuiTheme.text(), 18), 10);
        context.drawText(textRenderer, GuiTheme.uniform("Параметры модуля"), textX + 10, sectionY + 6, GuiTheme.withAlpha(GuiTheme.accentBright(), 238), false);
    }

    private void drawFooter(DrawContext context) {
        int footerX = mainX + 18;
        int footerY = shellY + shellHeight - 34;
        int footerWidth = mainWidth - 36;
        GuiTheme.drawCard(context, footerX, footerY, footerWidth, 18,
                GuiTheme.withAlpha(GuiTheme.panelAlt(), 168), GuiTheme.withAlpha(GuiTheme.text(), 16), 10);

        String footerText = waitingForKey
                ? "Режим назначения клавиши активен: нажмите кнопку на клавиатуре или Esc для отмены."
                : "Правая кнопка мыши открывает настройки функции, не засоряя основной экран кнопками.";
        context.drawText(textRenderer, GuiTheme.uniform(textRenderer.trimToWidth(footerText, footerWidth - 20)), footerX + 10, footerY + 5, GuiTheme.mutedText(), false);
    }

    private void drawHeaderV2(DrawContext context) {
        int logoCardX = heroX + 18;
        int logoCardY = heroY + 16;
        GuiTheme.drawCard(context, logoCardX, logoCardY, 46, 46,
                GuiTheme.withAlpha(GuiTheme.panel(), 210),
                GuiTheme.withAlpha(GuiTheme.accentBright(), 136),
                14);
        int logoX = logoCardX + 7;
        int logoY = logoCardY + 7;
        float logoScale = 32.0f / 64.0f;
        context.getMatrices().push();
        context.getMatrices().translate(logoX, logoY, 0.0f);
        context.getMatrices().scale(logoScale, logoScale, 1.0f);
        context.drawTexture(RenderLayer::getGuiTextured, TextureStorage.POLAR_WATERMARK, 0, 0,
                0.0f, 0.0f, 64, 64, 64, 64);
        context.getMatrices().pop();

        int textX = logoCardX + 60;
        context.drawTextWithShadow(textRenderer, GuiTheme.uniform(CLIENT_NAME), textX, heroY + 18, GuiTheme.text());
        context.drawText(textRenderer, GuiTheme.uniform(textRenderer.trimToWidth("Модульная панель с быстрым открытием настроек по ПКМ.", heroWidth - 320)),
                textX, heroY + 34, GuiTheme.mutedText(), false);
        context.drawText(textRenderer, GuiTheme.uniform("Версия " + CLIENT_VERSION), textX, heroY + 50, GuiTheme.withAlpha(GuiTheme.accentBright(), 240), false);

        int statY = heroY + 64;
        drawStatChip(context, textX, statY, "Модули", String.valueOf(getModulesForTab(currentTab).size()));
        drawStatChip(context, textX + 90, statY, "Активно", String.valueOf(countActiveModules(currentTab)));
        drawStatChip(context, textX + 186, statY, "Сетка", moduleColumns() + " кол.");

        int badgeWidth = 182;
        int badgeX = heroX + heroWidth - badgeWidth - 18;
        int badgeY = heroY + 18;
        GuiTheme.drawCard(context, badgeX, badgeY, badgeWidth, 54,
                GuiTheme.withAlpha(GuiTheme.accentSoft(), 164),
                GuiTheme.withAlpha(GuiTheme.accentBright(), 134),
                16);
        context.drawText(textRenderer, GuiTheme.uniform("Selected tab"), badgeX + 14, badgeY + 10, GuiTheme.mutedText(), false);
        context.drawText(textRenderer, GuiTheme.uniform(TAB_LABELS[currentTab]), badgeX + 14, badgeY + 27, GuiTheme.text(), false);
        context.drawText(textRenderer, GuiTheme.uniform(CLIENT_VERSION), badgeX + badgeWidth - 44, badgeY + 27,
                GuiTheme.withAlpha(GuiTheme.accentBright(), 244), false);
    }

    private void drawMainPanelTextV2(DrawContext context) {
        int sectionY = mainY + 16;
        context.drawTextWithShadow(textRenderer, GuiTheme.uniform("Модули"), mainX + 18, sectionY, GuiTheme.text());
        context.drawText(textRenderer, GuiTheme.uniform(textRenderer.trimToWidth("ЛКМ переключает модуль, ПКМ открывает его настройки справа.", mainWidth - 260)),
                mainX + 78, sectionY, GuiTheme.mutedText(), false);

        String summary = getModulesForTab(currentTab).size() + " modules / " + countActiveModules(currentTab) + " active";
        int pillWidth = Math.max(128, textRenderer.getWidth(summary) + 18);
        int pillX = mainX + mainWidth - pillWidth - 18;
        GuiTheme.drawCard(context, pillX, mainY + 10, pillWidth, 22,
                GuiTheme.withAlpha(GuiTheme.panelAlt(), 214),
                GuiTheme.withAlpha(GuiTheme.accent(), 82),
                11);
        context.drawCenteredTextWithShadow(textRenderer,
                GuiTheme.uniform(summary),
                pillX + pillWidth / 2, mainY + 17, GuiTheme.text());
    }

    private void drawSidebarV2(DrawContext context) {
        int textX = sidebarX + 16;
        int y = sidebarY + 18;
        int infoBottom = sidebarSettingsStartY() - 12;

        ModuleSpec selectedModule = getSelectedModule();
        String title = selectedModule != null ? selectedModule.title() : "Нет модуля";
        String description = selectedModule != null ? selectedModule.description() : "Выбери модуль из списка слева.";
        String status = selectedModule != null ? selectedModule.statusSupplier().get() : "--";

        context.drawTextWithShadow(textRenderer, GuiTheme.uniform("Настройки"), textX, y, GuiTheme.text());
        y += 18;
        context.drawText(textRenderer, GuiTheme.uniform("Текущий модуль"), textX, y, GuiTheme.mutedText(), false);
        y += 12;
        context.drawText(textRenderer, GuiTheme.uniform(textRenderer.trimToWidth(title, sidebarWidth - 104)), textX, y, GuiTheme.withAlpha(GuiTheme.accentBright(), 245), false);
        GuiTheme.drawCard(context, sidebarX + sidebarWidth - 84, y - 6, 52, 18,
                GuiTheme.withAlpha(GuiTheme.panel(), 206),
                GuiTheme.withAlpha(GuiTheme.accent(), 96),
                9);
        context.drawCenteredTextWithShadow(textRenderer, GuiTheme.uniform(status), sidebarX + sidebarWidth - 58, y - 1, GuiTheme.text());
        y += 18;
        y = drawWrappedTextLimited(context, GuiTheme.uniform(description), textX, y, sidebarWidth - 32, GuiTheme.text(), 2) + 10;

        context.drawText(textRenderer, GuiTheme.uniform(waitingForKey ? "Ожидание клавиши" : "Подсказка"), textX, y, GuiTheme.mutedText(), false);
        y += 12;
        Text tooltip = currentTooltip != null ? currentTooltip : GuiTheme.uniform("Наведи курсор на карточку или кнопку, чтобы увидеть описание.");
        y = drawWrappedTextLimited(context, tooltip, textX, y, sidebarWidth - 32, GuiTheme.text(), 2) + 10;

        int sectionY = Math.min(infoBottom, Math.max(y + 2, sidebarSettingsStartY() - 24));
        GuiTheme.drawCard(context, textX, sectionY, sidebarWidth - 32, 20,
                GuiTheme.withAlpha(GuiTheme.panel(), 186), GuiTheme.withAlpha(GuiTheme.text(), 18), 10);
        context.drawText(textRenderer, GuiTheme.uniform("Параметры модуля"), textX + 10, sectionY + 6, GuiTheme.withAlpha(GuiTheme.accentBright(), 238), false);
    }

    private void drawFooterV2(DrawContext context) {
        int footerX = mainX + 18;
        int footerY = shellY + shellHeight - 34;
        int footerWidth = mainWidth - 36;
        GuiTheme.drawCard(context, footerX, footerY, footerWidth, 18,
                GuiTheme.withAlpha(GuiTheme.panelAlt(), 168), GuiTheme.withAlpha(GuiTheme.text(), 16), 10);

        String footerText = waitingForKey
                ? "Режим назначения клавиши активен: нажмите кнопку на клавиатуре или Esc для отмены."
                : "ПКМ по карточке открывает настройки функции, колесо мыши листает списки.";
        context.drawText(textRenderer, GuiTheme.uniform(textRenderer.trimToWidth(footerText, footerWidth - 20)), footerX + 10, footerY + 5, GuiTheme.mutedText(), false);
    }

    private void drawStatChip(DrawContext context, int x, int y, String label, String value) {
        int width = Math.max(72, textRenderer.getWidth(value) + 26);
        GuiTheme.drawCard(context, x, y, width, 20,
                GuiTheme.withAlpha(GuiTheme.panelAlt(), 214),
                GuiTheme.withAlpha(GuiTheme.accent(), 82),
                10);
        context.drawText(textRenderer, GuiTheme.uniform(label), x + 10, y + 6, GuiTheme.mutedText(), false);
        context.drawText(textRenderer, GuiTheme.uniform(value), x + width - textRenderer.getWidth(value) - 10, y + 6,
                GuiTheme.withAlpha(GuiTheme.accentBright(), 238), false);
    }

    private int countActiveModules(int tab) {
        int active = 0;
        for (ModuleSpec module : getModulesForTab(tab)) {
            if (module.activeSupplier().getAsBoolean()) {
                active++;
            }
        }
        return active;
    }

    private void drawScrollBars(DrawContext context) {
        drawScrollBar(context, mainX + mainWidth - 10, mainContentTop(), mainContentHeight(), moduleScroll, moduleMaxScroll);
        drawScrollBar(context, sidebarX + sidebarWidth - 8, sidebarSettingsStartY(), settingsViewportHeight(), settingsScroll, settingsMaxScroll);
    }

    private void drawScrollBar(DrawContext context, int x, int y, int height, int scroll, int maxScroll) {
        if (maxScroll <= 0 || height <= 18) {
            return;
        }

        GuiTheme.drawCard(context, x, y, 4, height, GuiTheme.withAlpha(GuiTheme.panelAlt(), 160), GuiTheme.withAlpha(GuiTheme.text(), 14), 2);
        int thumbHeight = Math.max(18, Math.round(height * 0.28f));
        int thumbTravel = Math.max(1, height - thumbHeight - 2);
        int thumbY = y + 1 + Math.round((scroll / (float) maxScroll) * thumbTravel);
        GuiTheme.drawCard(context, x, thumbY, 4, thumbHeight,
                GuiTheme.withAlpha(GuiTheme.accentSoft(), 214),
                GuiTheme.withAlpha(GuiTheme.accentBright(), 118),
                2);
    }

    private Text resolveTooltip(int mouseX, int mouseY) {
        if (waitingForKey) {
            return GuiTheme.uniform("Назначьте клавишу для выбранной функции. Escape отменяет ожидание.");
        }

        for (var child : children()) {
            if (child instanceof ModernButtonWidget button && isMouseOver(button, mouseX, mouseY) && button.getTooltipText() != null) {
                return button.getTooltipText();
            }
            if (child instanceof ModuleCardWidget card && isMouseOver(card, mouseX, mouseY) && card.getTooltipText() != null) {
                return card.getTooltipText();
            }
        }
        return null;
    }

    private int drawWrappedText(DrawContext context, Text text, int x, int y, int width, int color) {
        List<OrderedText> lines = textRenderer.wrapLines(text, width);
        int currentY = y;
        for (OrderedText line : lines) {
            context.drawText(textRenderer, line, x, currentY, color, false);
            currentY += textRenderer.fontHeight + 2;
        }
        return currentY;
    }

    private int drawWrappedTextLimited(DrawContext context, Text text, int x, int y, int width, int color, int maxLines) {
        List<OrderedText> lines = textRenderer.wrapLines(text, width);
        int currentY = y;
        int rendered = Math.min(lines.size(), maxLines);
        for (int i = 0; i < rendered; i++) {
            context.drawText(textRenderer, lines.get(i), x, currentY, color, false);
            currentY += textRenderer.fontHeight + 2;
        }
        return currentY;
    }

    private boolean isMouseOver(ButtonWidget button, int mouseX, int mouseY) {
        return mouseX >= button.getX()
                && mouseY >= button.getY()
                && mouseX < button.getX() + button.getWidth()
                && mouseY < button.getY() + button.getHeight();
    }

    private boolean isMouseOver(ModuleCardWidget button, int mouseX, int mouseY) {
        return mouseX >= button.getX()
                && mouseY >= button.getY()
                && mouseX < button.getX() + button.getWidth()
                && mouseY < button.getY() + button.getHeight();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int direction = verticalAmount > 0 ? -1 : 1;
        if (isInsideSettingsScrollRegion(mouseX, mouseY) && settingsMaxScroll > 0) {
            settingsScroll = MathHelper.clamp(settingsScroll + direction * SETTING_ROW_HEIGHT, 0, settingsMaxScroll);
            rebuildSettingsButtons();
            return true;
        }
        if (isInsideMainScrollRegion(mouseX, mouseY) && moduleMaxScroll > 0) {
            moduleScroll = MathHelper.clamp(moduleScroll + direction * MODULE_ROW_HEIGHT, 0, moduleMaxScroll);
            rebuildModuleCards();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForKey) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                ModConfig config = ConfigManager.getConfig();
                if ("triggerbot".equals(waitingForFunction)) {
                    config.triggerbotKeyCode = keyCode;
                    if (triggerbotKeyButton != null) {
                        triggerbotKeyButton.setMessage(GuiTheme.uniform(keyLabel(keyCode)));
                    }
                } else if ("killaura".equals(waitingForFunction)) {
                    config.killauraKeyCode = keyCode;
                    if (killauraKeyButton != null) {
                        killauraKeyButton.setMessage(GuiTheme.uniform(keyLabel(keyCode)));
                    }
                } else if ("freeLook".equals(waitingForFunction)) {
                    config.freeLookKeyCode = keyCode;
                    if (freeLookKeyButton != null) {
                        freeLookKeyButton.setMessage(GuiTheme.uniform(keyLabel(keyCode)));
                    }
                }
                ConfigManager.save();
            }

            waitingForKey = false;
            waitingForFunction = null;
            rebuildSettingsButtons();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void setActiveState(ButtonWidget button, boolean active) {
        if (button instanceof ModernButtonWidget modernButtonWidget) {
            modernButtonWidget.setVisualActive(active);
        }
    }

    private static String onOffLabel(String label, boolean enabled) {
        return label + ": " + statusLabel(enabled);
    }

    private static String yesNoLabel(String label, boolean enabled) {
        return label + ": " + (enabled ? "Да" : "Нет");
    }

    private static String statusLabel(boolean enabled) {
        return enabled ? "ВКЛ" : "ВЫКЛ";
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

    private String keyLabel(int keyCode) {
        if (keyCode == -1) {
            return "Клавиша: нет";
        }

        try {
            return "Клавиша: " + InputUtil.fromKeyCode(keyCode, 0).getLocalizedText().getString();
        } catch (Exception e) {
            return "Клавиша: ?";
        }
    }

    private record ModuleSpec(
            String id,
            int tab,
            String title,
            String description,
            Supplier<String> statusSupplier,
            BooleanSupplier activeSupplier,
            Consumer<AimbotSettingsScreen> primaryAction,
            Consumer<AimbotSettingsScreen> settingsBuilder,
            Text tooltip,
            String actionHint
    ) {
    }
}
