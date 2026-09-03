package com.peaceman.alpha.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Community- und Mod-Tags für Relokation, Immunität und Cluster-Handling in NeoForge 1.21.
 */
public final class ModTags {
    private ModTags() {}

    public static final class Blocks {
        private Blocks() {}

        public static final TagKey<Block> RELOCATION_IMMUNE =
                BlockTags.create(ResourceLocation.fromNamespaceAndPath("c", "relocation_immune"));

        public static final TagKey<Block> RELOCATION_IMMUNE_FORGE =
                BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "relocation_immune"));

        public static final TagKey<Block> RELOCATES_AS_CLUSTER =
                BlockTags.create(ResourceLocation.fromNamespaceAndPath("c", "relocates_as_cluster"));

        public static final TagKey<Block> RELOCATES_AS_CLUSTER_FORGE =
                BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "relocates_as_cluster"));

        public static final TagKey<Block> INVENTORY_RELOCATION_SAFE =
                BlockTags.create(ResourceLocation.fromNamespaceAndPath("c", "inventory_relocation_safe"));

        public static final TagKey<Block> INVENTORY_RELOCATION_SAFE_FORGE =
                BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "inventory_relocation_safe"));
    }
}
