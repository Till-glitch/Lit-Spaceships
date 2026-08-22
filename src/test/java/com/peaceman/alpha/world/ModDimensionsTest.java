package com.peaceman.alpha.world;

import com.peaceman.alpha.Alpha;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ModDimensionsTest {

    @Test
    @DisplayName("ModDimensions ResourceKeys besitzen korrekte Namespaces und Pfade")
    void testModDimensions_Keys() {
        assertEquals(Alpha.MODID, ModDimensions.SPACE_LEVEL.location().getNamespace());
        assertEquals("space", ModDimensions.SPACE_LEVEL.location().getPath());

        assertEquals(Alpha.MODID, ModDimensions.SPACE_DIM_TYPE.location().getNamespace());
        assertEquals("space_type", ModDimensions.SPACE_DIM_TYPE.location().getPath());

        assertEquals(Alpha.MODID, ModDimensions.SPACE_BIOME.location().getNamespace());
        assertEquals("space_biome", ModDimensions.SPACE_BIOME.location().getPath());

        assertEquals(Alpha.MODID, ModDimensions.SPACE_NOISE_SETTINGS.location().getNamespace());
        assertEquals("space_noise", ModDimensions.SPACE_NOISE_SETTINGS.location().getPath());

        assertEquals(Alpha.MODID, ModDimensions.SPACE_STEM.location().getNamespace());
        assertEquals("space", ModDimensions.SPACE_STEM.location().getPath());
    }

    @Test
    @DisplayName("Datapack Dimension JSON Konfigurationen existieren im Classpath")
    void testDatapackDimensionFilesExist() {
        assertNotNull(getClass().getResourceAsStream("/data/peaceman_alpha/dimension/space.json"), "space.json fehlt");
        assertNotNull(getClass().getResourceAsStream("/data/peaceman_alpha/dimension_type/space_type.json"), "space_type.json fehlt");
        assertNotNull(getClass().getResourceAsStream("/data/peaceman_alpha/worldgen/biome/space_biome.json"), "space_biome.json fehlt");
        assertNotNull(getClass().getResourceAsStream("/data/peaceman_alpha/worldgen/noise_settings/space_noise.json"), "space_noise.json fehlt");
    }
}
