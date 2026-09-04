package com.lit.spaceships.datagen.provider;

import com.lit.spaceships.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collections;

public class ModBlockLootTableProvider extends BlockLootSubProvider {

    public ModBlockLootTableProvider(HolderLookup.Provider provider) {
        super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags(), provider);
    }

    @Override
    protected void generate() {
        // Self-Drop für alle registrierten Mod-Blöcke
        this.dropSelf(ModBlocks.EXAMPLE_BLOCK.get());
        this.dropSelf(ModBlocks.SPACESHIP_REACTOR.get());
        this.dropSelf(ModBlocks.SPACESHIP_SHIELD.get());
        this.dropSelf(ModBlocks.SPACESHIP_CONTROL.get());
        this.dropSelf(ModBlocks.SPACESHIP_HELM.get());

        // Auch Laser droppen sich selbst als Item
        this.dropSelf(ModBlocks.PULSE_LASER.get());
        this.dropSelf(ModBlocks.HEAVY_BEAM.get());
        this.dropSelf(ModBlocks.MINING_LASER.get());
        this.dropSelf(ModBlocks.WARP_ENGINE.get());
    }

    @Override
    public Iterable<Block> getKnownBlocks() {
        // Algorithmische Validierung über alle registrierten Blöcke im ModBlocks-Register
        return ModBlocks.BLOCKS.getEntries().stream()
                .filter(DeferredHolder::isBound)
                .map(holder -> (Block) holder.value())
                .toList();
    }
}
