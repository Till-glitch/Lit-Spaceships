package com.peaceman.alpha.datagen.provider;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * DataGen Provider für Community- und Relokations-Block-Tags in NeoForge 1.21.
 */
public class ModBlockTagsProvider extends BlockTagsProvider {

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Alpha.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.Blocks.RELOCATION_IMMUNE)
                .add(Blocks.BEDROCK, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME, Blocks.NETHER_PORTAL,
                        Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK,
                        Blocks.STRUCTURE_BLOCK, Blocks.JIGSAW, Blocks.BARRIER);

        tag(ModTags.Blocks.RELOCATION_IMMUNE_FORGE)
                .addTag(ModTags.Blocks.RELOCATION_IMMUNE);

        tag(ModTags.Blocks.RELOCATES_AS_CLUSTER);
        tag(ModTags.Blocks.RELOCATES_AS_CLUSTER_FORGE)
                .addTag(ModTags.Blocks.RELOCATES_AS_CLUSTER);

        tag(ModTags.Blocks.INVENTORY_RELOCATION_SAFE);
        tag(ModTags.Blocks.INVENTORY_RELOCATION_SAFE_FORGE)
                .addTag(ModTags.Blocks.INVENTORY_RELOCATION_SAFE);
    }
}
