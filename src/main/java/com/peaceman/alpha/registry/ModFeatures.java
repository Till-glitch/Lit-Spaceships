package com.peaceman.alpha.registry;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.world.feature.AsteroidFeature;
import com.peaceman.alpha.world.feature.SpaceWreckFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Alpha.MODID);

    public static final DeferredHolder<Feature<?>, AsteroidFeature> ASTEROID =
            FEATURES.register("asteroid", () -> new AsteroidFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, SpaceWreckFeature> SPACE_WRECK =
            FEATURES.register("space_wreck", () -> new SpaceWreckFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
