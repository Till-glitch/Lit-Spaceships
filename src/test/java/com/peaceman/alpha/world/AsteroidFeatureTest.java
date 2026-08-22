package com.peaceman.alpha.world;

import com.peaceman.alpha.registry.ModFeatures;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AsteroidFeatureTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
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
        assertNotNull(getClass().getResourceAsStream("/data/peaceman_alpha/worldgen/configured_feature/asteroid.json"), "asteroid configured feature fehlt");
        assertNotNull(getClass().getResourceAsStream("/data/peaceman_alpha/worldgen/configured_feature/space_wreck.json"), "space_wreck configured feature fehlt");
        assertNotNull(getClass().getResourceAsStream("/data/peaceman_alpha/worldgen/placed_feature/asteroid_placed.json"), "asteroid placed feature fehlt");
        assertNotNull(getClass().getResourceAsStream("/data/peaceman_alpha/worldgen/placed_feature/space_wreck_placed.json"), "space_wreck placed feature fehlt");
    }
}
