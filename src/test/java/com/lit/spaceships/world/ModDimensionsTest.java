package com.lit.spaceships.world;

import com.lit.spaceships.LitSpaceships;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ModDimensionsTest {

    @Test
    @DisplayName("ModDimensions ResourceKeys besitzen korrekte Namespaces und Pfade")
    void testModDimensions_Keys() {
        assertEquals(LitSpaceships.MODID, ModDimensions.SPACE_LEVEL.location().getNamespace());
        assertEquals("space", ModDimensions.SPACE_LEVEL.location().getPath());

        assertEquals(LitSpaceships.MODID, ModDimensions.SPACE_DIM_TYPE.location().getNamespace());
        assertEquals("space_type", ModDimensions.SPACE_DIM_TYPE.location().getPath());

        assertEquals(LitSpaceships.MODID, ModDimensions.SPACE_BIOME.location().getNamespace());
        assertEquals("space_biome", ModDimensions.SPACE_BIOME.location().getPath());

        assertEquals(LitSpaceships.MODID, ModDimensions.SPACE_NOISE_SETTINGS.location().getNamespace());
        assertEquals("space_noise", ModDimensions.SPACE_NOISE_SETTINGS.location().getPath());

        assertEquals(LitSpaceships.MODID, ModDimensions.SPACE_STEM.location().getNamespace());
        assertEquals("space", ModDimensions.SPACE_STEM.location().getPath());
    }

    @Test
    @DisplayName("Datapack Dimension JSON Konfigurationen existieren im Classpath")
    void testDatapackDimensionFilesExist() {
        assertNotNull(getClass().getResourceAsStream("/data/lit_spaceships/dimension/space.json"), "space.json fehlt");
        assertNotNull(getClass().getResourceAsStream("/data/lit_spaceships/dimension_type/space_type.json"), "space_type.json fehlt");
        assertNotNull(getClass().getResourceAsStream("/data/lit_spaceships/worldgen/biome/space_biome.json"), "space_biome.json fehlt");
        assertNotNull(getClass().getResourceAsStream("/data/lit_spaceships/worldgen/noise_settings/space_noise.json"), "space_noise.json fehlt");
    }
}
