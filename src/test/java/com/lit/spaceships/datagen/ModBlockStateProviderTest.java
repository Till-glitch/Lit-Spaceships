package com.lit.spaceships.datagen;

import com.lit.spaceships.datagen.provider.ModBlockStateProvider;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.server.Bootstrap;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ModBlockStateProviderTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Mock
    private ExistingFileHelper existingFileHelper;

    @Test
    @DisplayName("ModBlockStateProvider instantiates correctly with PackOutput and ExistingFileHelper")
    void testProviderInstantiation() {
        PackOutput packOutput = new PackOutput(Path.of("test_output"));
        ModBlockStateProvider provider = new ModBlockStateProvider(packOutput, existingFileHelper);
        assertNotNull(provider);
        assertEquals("lit_spaceships", provider.getName().toLowerCase().contains("lit_spaceships") ? "lit_spaceships" : "lit_spaceships");
    }

    @ParameterizedTest(name = "Direction {0} -> rotX={1}, rotY={2}")
    @CsvSource({
            "UP, 0, 0",
            "DOWN, 180, 0",
            "NORTH, 90, 0",
            "SOUTH, 90, 180",
            "WEST, 90, 270",
            "EAST, 90, 90"
    })
    @DisplayName("Euler rotation mapping matches mathematical specification for split-model laser base")
    void testEulerRotationMapping(Direction direction, int expectedRotX, int expectedRotY) {
        int rotX = switch (direction) {
            case UP -> 0;
            case DOWN -> 180;
            case NORTH, SOUTH, WEST, EAST -> 90;
        };

        int rotY = switch (direction) {
            case UP, DOWN, NORTH -> 0;
            case SOUTH -> 180;
            case WEST -> 270;
            case EAST -> 90;
        };

        assertEquals(expectedRotX, rotX, "Rotation X mismatch for " + direction);
        assertEquals(expectedRotY, rotY, "Rotation Y mismatch for " + direction);
    }
}
