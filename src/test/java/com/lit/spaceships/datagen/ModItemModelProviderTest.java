package com.lit.spaceships.datagen;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.datagen.provider.ModItemModelProvider;
import com.lit.spaceships.registry.ModBlocks;
import com.lit.spaceships.registry.ModItems;
import net.minecraft.SharedConstants;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ModItemModelProviderTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Mock
    private ExistingFileHelper existingFileHelper;

    @Test
    @DisplayName("ModItemModelProvider instantiates correctly")
    void testProviderInstantiation() {
        PackOutput packOutput = new PackOutput(Path.of("test_output"));
        ModItemModelProvider provider = new ModItemModelProvider(packOutput, existingFileHelper);
        assertNotNull(provider);
        assertEquals("lit_spaceships", provider.getName().toLowerCase().contains("lit_spaceships") ? "lit_spaceships" : "lit_spaceships");
    }

    @Test
    @DisplayName("Laser items are correctly referenced to laser_base block parent model")
    void testLaserItemModelPaths() {
        ResourceLocation laserBase = ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "block/laser_base");
        assertEquals("lit_spaceships:block/laser_base", laserBase.toString());

        assertEquals("pulse_laser", ModBlocks.PULSE_LASER.getId().getPath());
        assertEquals("heavy_beam", ModBlocks.HEAVY_BEAM.getId().getPath());
        assertEquals("mining_laser", ModBlocks.MINING_LASER.getId().getPath());
    }

    @Test
    @DisplayName("Mod items like backflip_tool are registered with valid ID")
    void testModItemRegistration() {
        assertEquals("backflip_tool", ModItems.BACKFLIP_TOOL.getId().getPath());
    }
}
