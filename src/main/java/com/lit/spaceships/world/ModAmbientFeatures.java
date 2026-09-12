package com.lit.spaceships.world;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.registry.ModFeatures;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

/**
 * Ambient-Features: 5 zusaetzliche Features pro Weltraum-Biom (20 insgesamt).
 * Klein, billig und Biom-thematisch — sie geben jedem Biom seine eigene
 * Textur ohne die grossen Strukturen zu ersetzen.
 *
 * <p>Wird am Ende von {@link ModConfiguredFeatures#bootstrap} (configured) und
 * {@link ModPlacedFeatures#bootstrap} (placed) aufgerufen.</p>
 */
public final class ModAmbientFeatures {

    // ---------- Deep Space ----------
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_DEBRIS_FIELD = cfg("debris_field");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_METEOR_SHOWER = cfg("meteor_shower");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_VOID_CRYSTAL_SPIKE = cfg("void_crystal_spike");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_BEACON_PYLON = cfg("beacon_pylon");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_CARGO_POD = cfg("cargo_pod");

    public static final ResourceKey<PlacedFeature> DEBRIS_FIELD_PLACED = placed("debris_field_placed");
    public static final ResourceKey<PlacedFeature> METEOR_SHOWER_PLACED = placed("meteor_shower_placed");
    public static final ResourceKey<PlacedFeature> VOID_CRYSTAL_SPIKE_PLACED = placed("void_crystal_spike_placed");
    public static final ResourceKey<PlacedFeature> BEACON_PYLON_PLACED = placed("beacon_pylon_placed");
    public static final ResourceKey<PlacedFeature> CARGO_POD_PLACED = placed("cargo_pod_placed");

    // ---------- Plasma Nebula ----------
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_NEBULA_SPORE_DRIFT = cfg("nebula_spore_drift");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_PLASMA_EMBER = cfg("plasma_ember");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_NEBULA_GAS_BLOOM = cfg("nebula_gas_bloom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_CRYSTAL_LATTICE = cfg("crystal_lattice");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_NEBULA_ARC = cfg("nebula_arc");

    public static final ResourceKey<PlacedFeature> NEBULA_SPORE_DRIFT_PLACED = placed("nebula_spore_drift_placed");
    public static final ResourceKey<PlacedFeature> PLASMA_EMBER_PLACED = placed("plasma_ember_placed");
    public static final ResourceKey<PlacedFeature> NEBULA_GAS_BLOOM_PLACED = placed("nebula_gas_bloom_placed");
    public static final ResourceKey<PlacedFeature> CRYSTAL_LATTICE_PLACED = placed("crystal_lattice_placed");
    public static final ResourceKey<PlacedFeature> NEBULA_ARC_PLACED = placed("nebula_arc_placed");

    // ---------- Frozen Expanse ----------
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_ICE_SHARD_FIELD = cfg("ice_shard_field");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_GLACIER_FLOE = cfg("glacier_floe");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_FROST_PILLAR = cfg("frost_pillar");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_SNOW_BLOOM = cfg("snow_bloom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_CRYO_GEODE = cfg("cryo_geode");

    public static final ResourceKey<PlacedFeature> ICE_SHARD_FIELD_PLACED = placed("ice_shard_field_placed");
    public static final ResourceKey<PlacedFeature> GLACIER_FLOE_PLACED = placed("glacier_floe_placed");
    public static final ResourceKey<PlacedFeature> FROST_PILLAR_PLACED = placed("frost_pillar_placed");
    public static final ResourceKey<PlacedFeature> SNOW_BLOOM_PLACED = placed("snow_bloom_placed");
    public static final ResourceKey<PlacedFeature> CRYO_GEODE_PLACED = placed("cryo_geode_placed");

    // ---------- Void Wastes ----------
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_BONE_DEBRIS = cfg("bone_debris");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_SCRAP_WASTELAND = cfg("scrap_wasteland");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_DUST_DRIFT = cfg("dust_drift");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_ASH_VENT = cfg("ash_vent");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_VOID_CYST = cfg("void_cyst");
    // ---------- Epoch 8: extreme biome signature features ----------
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_GRAVITY_RIFT_DISK = cfg("gravity_rift_disk");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_STELLAR_FLARE = cfg("stellar_flare");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CFG_ION_PYLON = cfg("ion_pylon");

    public static final ResourceKey<PlacedFeature> BONE_DEBRIS_PLACED = placed("bone_debris_placed");
    public static final ResourceKey<PlacedFeature> SCRAP_WASTELAND_PLACED = placed("scrap_wasteland_placed");
    public static final ResourceKey<PlacedFeature> DUST_DRIFT_PLACED = placed("dust_drift_placed");
    public static final ResourceKey<PlacedFeature> ASH_VENT_PLACED = placed("ash_vent_placed");
    public static final ResourceKey<PlacedFeature> VOID_CYST_PLACED = placed("void_cyst_placed");
    public static final ResourceKey<PlacedFeature> GRAVITY_RIFT_DISK_PLACED = placed("gravity_rift_disk_placed");
    public static final ResourceKey<PlacedFeature> STELLAR_FLARE_PLACED = placed("stellar_flare_placed");
    public static final ResourceKey<PlacedFeature> ION_PYLON_PLACED = placed("ion_pylon_placed");

    private ModAmbientFeatures() {
    }

    /** Registriert alle 20 ConfiguredFeatures (Feature-Instanzen aus ModFeatures). */
    public static void bootstrapConfigured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        register(context, CFG_DEBRIS_FIELD, ModFeatures.DEBRIS_FIELD.get());
        register(context, CFG_METEOR_SHOWER, ModFeatures.METEOR_SHOWER.get());
        register(context, CFG_VOID_CRYSTAL_SPIKE, ModFeatures.VOID_CRYSTAL_SPIKE.get());
        register(context, CFG_BEACON_PYLON, ModFeatures.BEACON_PYLON.get());
        register(context, CFG_CARGO_POD, ModFeatures.CARGO_POD.get());

        register(context, CFG_NEBULA_SPORE_DRIFT, ModFeatures.NEBULA_SPORE_DRIFT.get());
        register(context, CFG_PLASMA_EMBER, ModFeatures.PLASMA_EMBER.get());
        register(context, CFG_NEBULA_GAS_BLOOM, ModFeatures.NEBULA_GAS_BLOOM.get());
        register(context, CFG_CRYSTAL_LATTICE, ModFeatures.CRYSTAL_LATTICE.get());
        register(context, CFG_NEBULA_ARC, ModFeatures.NEBULA_ARC.get());

        register(context, CFG_ICE_SHARD_FIELD, ModFeatures.ICE_SHARD_FIELD.get());
        register(context, CFG_GLACIER_FLOE, ModFeatures.GLACIER_FLOE.get());
        register(context, CFG_FROST_PILLAR, ModFeatures.FROST_PILLAR.get());
        register(context, CFG_SNOW_BLOOM, ModFeatures.SNOW_BLOOM.get());
        register(context, CFG_CRYO_GEODE, ModFeatures.CRYO_GEODE.get());

        register(context, CFG_BONE_DEBRIS, ModFeatures.BONE_DEBRIS.get());
        register(context, CFG_SCRAP_WASTELAND, ModFeatures.SCRAP_WASTELAND.get());
        register(context, CFG_DUST_DRIFT, ModFeatures.DUST_DRIFT.get());
        register(context, CFG_ASH_VENT, ModFeatures.ASH_VENT.get());
        register(context, CFG_VOID_CYST, ModFeatures.VOID_CYST.get());
        register(context, CFG_GRAVITY_RIFT_DISK, ModFeatures.GRAVITY_RIFT.get());
        register(context, CFG_STELLAR_FLARE, ModFeatures.STELLAR_CORONA.get());
        register(context, CFG_ION_PYLON, ModFeatures.ION_STORM.get());
    }

    /** Registriert alle 20 PlacedFeatures mit Biom-thematischen Hoehenbaendern. */
    public static void bootstrapPlaced(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);

        // Deep Space: weiter Streubereich
        placed(context, configured, DEBRIS_FIELD_PLACED, CFG_DEBRIS_FIELD, 2, -40, 280, 1);
        placed(context, configured, METEOR_SHOWER_PLACED, CFG_METEOR_SHOWER, 2, -40, 280, 1);
        placed(context, configured, VOID_CRYSTAL_SPIKE_PLACED, CFG_VOID_CRYSTAL_SPIKE, 1, -40, 280, 1);
        placed(context, configured, BEACON_PYLON_PLACED, CFG_BEACON_PYLON, 1, -40, 280, 2);
        placed(context, configured, CARGO_POD_PLACED, CFG_CARGO_POD, 1, 0, 240, 24);

        // Plasma Nebula: upper void, wo die Nebel Woelkchen treiben
        placed(context, configured, NEBULA_SPORE_DRIFT_PLACED, CFG_NEBULA_SPORE_DRIFT, 2, 96, 288, 1);
        placed(context, configured, PLASMA_EMBER_PLACED, CFG_PLASMA_EMBER, 2, 96, 288, 1);
        placed(context, configured, NEBULA_GAS_BLOOM_PLACED, CFG_NEBULA_GAS_BLOOM, 1, 96, 288, 1);
        placed(context, configured, CRYSTAL_LATTICE_PLACED, CFG_CRYSTAL_LATTICE, 1, 96, 288, 2);
        placed(context, configured, NEBULA_ARC_PLACED, CFG_NEBULA_ARC, 1, 96, 288, 1);

        // Frozen Expanse: kompaktes Band
        placed(context, configured, ICE_SHARD_FIELD_PLACED, CFG_ICE_SHARD_FIELD, 2, -40, 280, 1);
        placed(context, configured, GLACIER_FLOE_PLACED, CFG_GLACIER_FLOE, 2, -40, 280, 1);
        placed(context, configured, FROST_PILLAR_PLACED, CFG_FROST_PILLAR, 1, -40, 280, 1);
        placed(context, configured, SNOW_BLOOM_PLACED, CFG_SNOW_BLOOM, 1, -40, 280, 2);
        placed(context, configured, CRYO_GEODE_PLACED, CFG_CRYO_GEODE, 1, -40, 280, 2);

        // Void Wastes: Streu ueber den Wuestenbogen
        placed(context, configured, BONE_DEBRIS_PLACED, CFG_BONE_DEBRIS, 2, -40, 240, 1);
        placed(context, configured, SCRAP_WASTELAND_PLACED, CFG_SCRAP_WASTELAND, 2, 0, 240, 1);
        placed(context, configured, DUST_DRIFT_PLACED, CFG_DUST_DRIFT, 2, -40, 240, 1);
        placed(context, configured, ASH_VENT_PLACED, CFG_ASH_VENT, 1, 0, 240, 2);
        placed(context, configured, VOID_CYST_PLACED, CFG_VOID_CYST, 1, 0, 240, 2);
        // Epoch 8: extreme biome signature features
        placed(context, configured, GRAVITY_RIFT_DISK_PLACED, CFG_GRAVITY_RIFT_DISK, 2, 0, 240, 1);
        placed(context, configured, STELLAR_FLARE_PLACED, CFG_STELLAR_FLARE, 2, 96, 288, 1);
        placed(context, configured, ION_PYLON_PLACED, CFG_ION_PYLON, 2, 64, 256, 1);
    }

    private static void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                 ResourceKey<ConfiguredFeature<?, ?>> key, Feature<NoneFeatureConfiguration> feature) {
        context.register(key, new ConfiguredFeature<>(feature, NoneFeatureConfiguration.INSTANCE));
    }

    private static void placed(BootstrapContext<PlacedFeature> context,
                               HolderGetter<ConfiguredFeature<?, ?>> configured,
                               ResourceKey<PlacedFeature> placedKey,
                               ResourceKey<ConfiguredFeature<?, ?>> configuredKey,
                               int count, int minY, int maxY, int rarity) {
        List<PlacementModifier> modifiers;
        if (rarity > 1) {
            modifiers = List.of(
                    RarityFilter.onAverageOnceEvery(rarity),
                    CountPlacement.of(count),
                    InSquarePlacement.spread(),
                    HeightRangePlacement.uniform(VerticalAnchor.absolute(minY), VerticalAnchor.absolute(maxY)),
                    BiomeFilter.biome());
        } else {
            modifiers = List.of(
                    CountPlacement.of(count),
                    InSquarePlacement.spread(),
                    HeightRangePlacement.uniform(VerticalAnchor.absolute(minY), VerticalAnchor.absolute(maxY)),
                    BiomeFilter.biome());
        }
        context.register(placedKey, new PlacedFeature(configured.getOrThrow(configuredKey), modifiers));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> cfg(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, name));
    }

    /** Test-Zugriff auf die konfigurierten Keys (namensbasiert, deterministisch). */
    public static ResourceKey<ConfiguredFeature<?, ?>> cfgForTest(String name) {
        return cfg(name);
    }

    /** Test-Zugriff auf die Placed-Keys (namensbasiert, deterministisch). */
    public static ResourceKey<PlacedFeature> placedForTest(String name) {
        return placed(name);
    }
}
