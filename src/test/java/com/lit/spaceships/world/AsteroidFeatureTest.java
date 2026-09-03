package com.lit.spaceships.world;

import com.lit.spaceships.registry.ModFeatures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AsteroidFeatureTest {

    @org.junit.jupiter.api.BeforeAll
    static void initMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("ModFeatures hält Registrierungs-Holder für Asteroiden und Wracks")
    void testModFeatures_RegistrationHolders() {
        assertNotNull(ModFeatures.ASTEROID);
        assertEquals("asteroid", ModFeatures.ASTEROID.getId().getPath());

        assertNotNull(ModFeatures.SPACE_WRECK);
        assertEquals("space_wreck", ModFeatures.SPACE_WRECK.getId().getPath());
    }

    @Test
    @DisplayName("WorldGen Configured and Placed Feature JSONs existieren im Classpath")
    void testWorldGenFeatureFilesExist() {
        assertNotNull(getClass().getResourceAsStream("/data/lit_spaceships/worldgen/configured_feature/asteroid.json"), "asteroid configured feature fehlt");
        assertNotNull(getClass().getResourceAsStream("/data/lit_spaceships/worldgen/configured_feature/space_wreck.json"), "space_wreck configured feature fehlt");
        assertNotNull(getClass().getResourceAsStream("/data/lit_spaceships/worldgen/placed_feature/asteroid_placed.json"), "asteroid placed feature fehlt");
        assertNotNull(getClass().getResourceAsStream("/data/lit_spaceships/worldgen/placed_feature/space_wreck_placed.json"), "space_wreck placed feature fehlt");
    }
}
