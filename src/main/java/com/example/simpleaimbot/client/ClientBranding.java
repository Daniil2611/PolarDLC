package com.example.simpleaimbot.client;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;

public final class ClientBranding {
    public static final String MOD_ID = "simpleaimbot";
    public static final String NAME = resolveName();
    public static final String VERSION = resolveVersion();

    private ClientBranding() {
    }

    private static String resolveName() {
        ModMetadata metadata = resolveMetadata();
        return metadata != null ? metadata.getName() : "PolarDLC";
    }

    private static String resolveVersion() {
        ModMetadata metadata = resolveMetadata();
        return metadata != null ? metadata.getVersion().getFriendlyString() : "0.2.0";
    }

    private static ModMetadata resolveMetadata() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata())
                .orElse(null);
    }
}
