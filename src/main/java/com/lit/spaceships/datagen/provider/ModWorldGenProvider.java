package com.lit.spaceships.datagen.provider;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.world.ModBiomes;
import com.lit.spaceships.world.ModConfiguredFeatures;
import com.lit.spaceships.world.ModPlacedFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Erzeugt alle Datapack-Registry-Einträge des Weltraums aus Java-Bootstrap-Code:
 * Biome, ConfiguredFeatures und PlacedFeatures (Strukturen folgen in späteren Epochen).
 * Grundregel: niemals manuelle JSON-Dateien unter {@code data/lit_spaceships/worldgen/}.
 */
public final class ModWorldGenProvider extends DatapackBuiltinEntriesProvider {

    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.BIOME, ModBiomes::bootstrap)
            .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
            .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap);

    public ModWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, BUILDER, Set.of(LitSpaceships.MODID));
    }
}
