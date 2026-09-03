package com.lit.spaceships.testutil;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Globaler JUnit 5 Extension Callback, der die NeoForge 21.1 LoadingModList
 * und Minecraft Bootstrap (Blocks, FeatureFlags, Registries) vor der Ausführung
 * von Unit-Tests headless initialisiert.
 */
public class MinecraftBootstrapExtension implements BeforeAllCallback {

    private static volatile boolean initialized = false;

    public static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }

        try {
            Class<?> clazz = Class.forName("net.neoforged.fml.loading.LoadingModList");
            Method ofMethod = clazz.getDeclaredMethod("of", List.class, List.class, List.class, List.class, Map.class);
            ofMethod.setAccessible(true);
            ofMethod.invoke(null, List.of(), List.of(), List.of(), List.of(), Map.of());
        } catch (Throwable ignored) {
        }

        try {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        initialized = true;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        ensureInitialized();
    }
}
