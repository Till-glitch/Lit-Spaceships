package com.lit.spaceships.world;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.registry.ModFeatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Bootstrap für alle {@link ConfiguredFeature}s des Weltraums.
 * Ersetzt manuelle JSON-Dateien unter {@code worldgen/configured_feature/}.
 */
public final class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> ASTEROID = createKey("asteroid");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPACE_WRECK = createKey("space_wreck");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ICE_COMET = createKey("ice_comet");

    private ModConfiguredFeatures() {
    }

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        bootstrapWith(context, ModFeatures.ASTEROID.get(), ModFeatures.SPACE_WRECK.get(), ModFeatures.ICE_COMET.get());
    }

    static void bootstrapWith(BootstrapContext<ConfiguredFeature<?, ?>> context,
                              Feature<NoneFeatureConfiguration> asteroid,
                              Feature<NoneFeatureConfiguration> spaceWreck,
                              Feature<NoneFeatureConfiguration> iceComet) {
        context.register(ASTEROID, new ConfiguredFeature<>(asteroid, NoneFeatureConfiguration.INSTANCE));
        context.register(SPACE_WRECK, new ConfiguredFeature<>(spaceWreck, NoneFeatureConfiguration.INSTANCE));
        context.register(ICE_COMET, new ConfiguredFeature<>(iceComet, NoneFeatureConfiguration.INSTANCE));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> createKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, name));
    }
}
