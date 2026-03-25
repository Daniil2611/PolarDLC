package com.example.simpleaimbot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", "polardlc.json");

    private static ModConfig config = new ModConfig();

    private ConfigManager() {
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            config = createDefaultConfig();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            ModConfig loadedConfig = GSON.fromJson(reader, ModConfig.class);
            config = loadedConfig != null ? loadedConfig : createDefaultConfig();
        } catch (IOException e) {
            LOGGER.error("Failed to load config", e);
            config = createDefaultConfig();
        }

        config.sanitize();
    }

    public static void save() {
        config.sanitize();

        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    public static ModConfig getConfig() {
        if (config == null) {
            config = createDefaultConfig();
        }
        return config;
    }

    private static ModConfig createDefaultConfig() {
        ModConfig defaults = new ModConfig();
        defaults.sanitize();
        return defaults;
    }
}
