package com.lit.spaceships.registry;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.world.feature.AsteroidBeltFeature;
import com.lit.spaceships.world.feature.AsteroidFeature;
import com.lit.spaceships.world.feature.IceCometFeature;
import com.lit.spaceships.world.feature.MegaAsteroidFeature;
import com.lit.spaceships.world.feature.PlanetaryRingFeature;
import com.lit.spaceships.world.feature.SpaceWreckFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, LitSpaceships.MODID);

    public static final DeferredHolder<Feature<?>, AsteroidFeature> ASTEROID =
            FEATURES.register("asteroid", () -> new AsteroidFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, SpaceWreckFeature> SPACE_WRECK =
            FEATURES.register("space_wreck", () -> new SpaceWreckFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, IceCometFeature> ICE_COMET =
            FEATURES.register("ice_comet", () -> new IceCometFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, MegaAsteroidFeature> MEGA_ASTEROID =
            FEATURES.register("mega_asteroid", () -> new MegaAsteroidFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, PlanetaryRingFeature> PLANETARY_RING =
            FEATURES.register("planetary_ring", () -> new PlanetaryRingFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, AsteroidBeltFeature> ASTEROID_BELT =
            FEATURES.register("asteroid_belt", () -> new AsteroidBeltFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
