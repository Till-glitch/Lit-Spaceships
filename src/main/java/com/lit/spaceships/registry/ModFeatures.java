package com.lit.spaceships.registry;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.world.feature.AncientBattlefieldFeature;
import com.lit.spaceships.world.feature.AsteroidBeltFeature;
import com.lit.spaceships.world.feature.AsteroidFeature;
import com.lit.spaceships.world.feature.IceCometFeature;
import com.lit.spaceships.world.feature.MegaAsteroidFeature;
import com.lit.spaceships.world.feature.PlanetaryRingFeature;
import com.lit.spaceships.world.feature.CosmicJellyfishFeature;
import com.lit.spaceships.world.feature.ModAmbientPalettes;
import com.lit.spaceships.world.feature.OrbFeature;
import com.lit.spaceships.world.feature.PillarFeature;
import com.lit.spaceships.world.feature.PodFeature;
import com.lit.spaceships.world.feature.SatelliteGraveyardFeature;
import com.lit.spaceships.world.feature.ScatterBlockFeature;
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

    public static final DeferredHolder<Feature<?>, SatelliteGraveyardFeature> SATELLITE_GRAVEYARD =
            FEATURES.register("satellite_graveyard", () -> new SatelliteGraveyardFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, CosmicJellyfishFeature> COSMIC_JELLYFISH =
            FEATURES.register("cosmic_jellyfish", () -> new CosmicJellyfishFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, AncientBattlefieldFeature> ANCIENT_BATTLEFIELD =
            FEATURES.register("ancient_battlefield", () -> new AncientBattlefieldFeature(NoneFeatureConfiguration.CODEC));

    // ---------- Ambient-Features: 5 pro Biom ----------

    // Deep Space
    public static final DeferredHolder<Feature<?>, ScatterBlockFeature> DEBRIS_FIELD =
            FEATURES.register("debris_field", () -> new ScatterBlockFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.DEBRIS_FIELD, 6, 3));
    public static final DeferredHolder<Feature<?>, ScatterBlockFeature> METEOR_SHOWER =
            FEATURES.register("meteor_shower", () -> new ScatterBlockFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.METEOR_SHOWER, 5, 2));
    public static final DeferredHolder<Feature<?>, PillarFeature> VOID_CRYSTAL_SPIKE =
            FEATURES.register("void_crystal_spike", () -> new PillarFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.VOID_CRYSTAL_SPIKE));
    public static final DeferredHolder<Feature<?>, PillarFeature> BEACON_PYLON =
            FEATURES.register("beacon_pylon", () -> new PillarFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.BEACON_PYLON));
    public static final DeferredHolder<Feature<?>, PodFeature> CARGO_POD =
            FEATURES.register("cargo_pod", () -> new PodFeature(NoneFeatureConfiguration.CODEC,
                    PodFeature.CARGO_POD_LOOT));

    // Plasma Nebula
    public static final DeferredHolder<Feature<?>, ScatterBlockFeature> NEBULA_SPORE_DRIFT =
            FEATURES.register("nebula_spore_drift", () -> new ScatterBlockFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.NEBULA_SPORE_DRIFT, 5, 3));
    public static final DeferredHolder<Feature<?>, ScatterBlockFeature> PLASMA_EMBER =
            FEATURES.register("plasma_ember", () -> new ScatterBlockFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.PLASMA_EMBER, 4, 2));
    public static final DeferredHolder<Feature<?>, OrbFeature> NEBULA_GAS_BLOOM =
            FEATURES.register("nebula_gas_bloom", () -> new OrbFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.NEBULA_GAS_BLOOM));
    public static final DeferredHolder<Feature<?>, OrbFeature> CRYSTAL_LATTICE =
            FEATURES.register("crystal_lattice", () -> new OrbFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.CRYSTAL_LATTICE));
    public static final DeferredHolder<Feature<?>, PillarFeature> NEBULA_ARC =
            FEATURES.register("nebula_arc", () -> new PillarFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.NEBULA_ARC));

    // Frozen Expanse
    public static final DeferredHolder<Feature<?>, ScatterBlockFeature> ICE_SHARD_FIELD =
            FEATURES.register("ice_shard_field", () -> new ScatterBlockFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.ICE_SHARD_FIELD, 6, 3));
    public static final DeferredHolder<Feature<?>, ScatterBlockFeature> GLACIER_FLOE =
            FEATURES.register("glacier_floe", () -> new ScatterBlockFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.GLACIER_FLOE, 8, 2));
    public static final DeferredHolder<Feature<?>, PillarFeature> FROST_PILLAR =
            FEATURES.register("frost_pillar", () -> new PillarFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.FROST_PILLAR));
    public static final DeferredHolder<Feature<?>, OrbFeature> SNOW_BLOOM =
            FEATURES.register("snow_bloom", () -> new OrbFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.SNOW_BLOOM));
    public static final DeferredHolder<Feature<?>, OrbFeature> CRYO_GEODE =
            FEATURES.register("cryo_geode", () -> new OrbFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.CRYO_GEODE));

    // Void Wastes
    public static final DeferredHolder<Feature<?>, ScatterBlockFeature> BONE_DEBRIS =
            FEATURES.register("bone_debris", () -> new ScatterBlockFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.BONE_DEBRIS, 5, 3));
    public static final DeferredHolder<Feature<?>, ScatterBlockFeature> SCRAP_WASTELAND =
            FEATURES.register("scrap_wasteland", () -> new ScatterBlockFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.SCRAP_WASTELAND, 5, 3));
    public static final DeferredHolder<Feature<?>, ScatterBlockFeature> DUST_DRIFT =
            FEATURES.register("dust_drift", () -> new ScatterBlockFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.DUST_DRIFT, 8, 2));
    public static final DeferredHolder<Feature<?>, PillarFeature> ASH_VENT =
            FEATURES.register("ash_vent", () -> new PillarFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.ASH_VENT));
    public static final DeferredHolder<Feature<?>, OrbFeature> VOID_CYST =
            FEATURES.register("void_cyst", () -> new OrbFeature(NoneFeatureConfiguration.CODEC,
                    ModAmbientPalettes.VOID_CYST));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
