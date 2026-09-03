package com.peaceman.alpha.datagen;

import com.peaceman.alpha.datagen.provider.ModBlockLootTableProvider;
import com.peaceman.alpha.datagen.provider.ModLootTableProvider;
import com.peaceman.alpha.registry.ModBlocks;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ModLootTableProviderTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Mock
    private HolderLookup.Provider lookupProvider;

    @Test
    @DisplayName("ModLootTableProvider factory creates a valid LootTableProvider")
    void testCreateLootTableProvider() {
        PackOutput packOutput = new PackOutput(Path.of("test_output"));
        CompletableFuture<HolderLookup.Provider> future = CompletableFuture.completedFuture(lookupProvider);

        LootTableProvider provider = ModLootTableProvider.create(packOutput, future);
        assertNotNull(provider);
        assertEquals("Loot Tables", provider.getName());
    }

    @Test
    @DisplayName("ModBlockLootTableProvider getKnownBlocks runs safely and ModBlocks registry contains all ship blocks")
    void testGetKnownBlocksCompleteness() {
        ModBlockLootTableProvider subProvider = new ModBlockLootTableProvider(lookupProvider);
        Iterable<Block> knownBlocks = subProvider.getKnownBlocks();
        assertNotNull(knownBlocks);

        // Verify that all 8 essential blocks are registered in ModBlocks
        var entries = ModBlocks.BLOCKS.getEntries();
        assertEquals(8, entries.size());

        var blockIds = entries.stream().map(e -> e.getId().getPath()).toList();
        assertTrue(blockIds.contains("example_block"));
        assertTrue(blockIds.contains("spaceship_control"));
        assertTrue(blockIds.contains("spaceship_helm"));
        assertTrue(blockIds.contains("spaceship_reactor"));
        assertTrue(blockIds.contains("spaceship_shield"));
        assertTrue(blockIds.contains("pulse_laser"));
        assertTrue(blockIds.contains("heavy_beam"));
        assertTrue(blockIds.contains("mining_laser"));
    }
}
