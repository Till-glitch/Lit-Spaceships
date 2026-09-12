package com.lit.spaceships.world;

import com.lit.spaceships.world.feature.AsteroidBeltFeature;
import com.lit.spaceships.world.feature.AsteroidFeature;
import com.lit.spaceships.world.feature.IceCometFeature;
import com.lit.spaceships.world.feature.MegaAsteroidFeature;
import com.lit.spaceships.world.ModAmbientFeatures;
import com.lit.spaceships.world.feature.PlanetaryRingFeature;
import com.lit.spaceships.world.feature.SpaceWreckFeature;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifiziert die Registry-Schlüssel, den Bootstrap der Datapack-Registries und die
 * Platzierungs-Mathe des Weltraums (Migration von Hand-JSON auf RegistrySetBuilder).
 */
@ExtendWith(MockitoExtension.class)
class ModSpaceWorldGenTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Mock
    private BootstrapContext<ConfiguredFeature<?, ?>> configuredContext;

    @Mock
    private BootstrapContext<PlacedFeature> placedContext;

    @Mock
    private BootstrapContext<Biome> biomeContext;

    @Mock
    private HolderGetter<ConfiguredFeature<?, ?>> configuredGetter;

    @Mock
    private HolderGetter<PlacedFeature> placedGetter;

    private final HolderOwner<ConfiguredFeature<?, ?>> configuredOwner = new HolderOwner<>() {
    };
    private final HolderOwner<PlacedFeature> placedOwner = new HolderOwner<>() {
    };
    private final HolderOwner<Biome> biomeOwner = new HolderOwner<>() {
    };

    @Mock
    private HolderGetter<Biome> biomeGetter;

    @Mock
    private BootstrapContext<StructureTemplatePool> poolContext;

    @Mock
    private BootstrapContext<Structure> structureContext;

    @Mock
    private BootstrapContext<StructureSet> structureSetContext;

    @Mock
    private HolderGetter<StructureTemplatePool> poolGetter;

    @Mock
    private HolderGetter<Structure> structureGetter;

    private final HolderOwner<StructureTemplatePool> poolOwner = new HolderOwner<>() {
    };

    private static final String[][] AMBIENT_FEATURES = {
            {"debris_field", "meteor_shower", "void_crystal_spike", "beacon_pylon", "cargo_pod"},
            {"nebula_spore_drift", "plasma_ember", "nebula_gas_bloom", "crystal_lattice", "nebula_arc"},
            {"ice_shard_field", "glacier_floe", "frost_pillar", "snow_bloom", "cryo_geode"},
            {"bone_debris", "scrap_wasteland", "dust_drift", "ash_vent", "void_cyst"}
    };

    /** Stubbt alle 20 Ambient-Placed-Features (Bootstrap registriert alle 4 Biome). */
    private void stubAmbientPlacedFeatures() {
        for (String[] names : AMBIENT_FEATURES) {
            for (String n : names) {
                doReturn(Holder.Reference.createStandAlone(placedOwner, ModAmbientFeatures.placedForTest(n + "_placed")))
                        .when(placedGetter).getOrThrow(ModAmbientFeatures.placedForTest(n + "_placed"));
            }
        }
        // Epoch 8: extreme biome signature features (alle 7 Biome werden gebootstrapped)
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModAmbientFeatures.GRAVITY_RIFT_DISK_PLACED))
                .when(placedGetter).getOrThrow(ModAmbientFeatures.GRAVITY_RIFT_DISK_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModAmbientFeatures.STELLAR_FLARE_PLACED))
                .when(placedGetter).getOrThrow(ModAmbientFeatures.STELLAR_FLARE_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModAmbientFeatures.ION_PYLON_PLACED))
                .when(placedGetter).getOrThrow(ModAmbientFeatures.ION_PYLON_PLACED);
    }

    private void stubAllBiomes() {
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModDimensions.SPACE_BIOME))
                .when(biomeGetter).getOrThrow(ModDimensions.SPACE_BIOME);
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModBiomes.PLASMA_NEBULA))
                .when(biomeGetter).getOrThrow(ModBiomes.PLASMA_NEBULA);
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModBiomes.FROZEN_EXPANSE))
                .when(biomeGetter).getOrThrow(ModBiomes.FROZEN_EXPANSE);
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModBiomes.VOID_WASTES))
                .when(biomeGetter).getOrThrow(ModBiomes.VOID_WASTES);
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModBiomes.GRAVITY_RIFT))
                .when(biomeGetter).getOrThrow(ModBiomes.GRAVITY_RIFT);
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModBiomes.STELLAR_CORONA))
                .when(biomeGetter).getOrThrow(ModBiomes.STELLAR_CORONA);
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModBiomes.ION_STORM))
                .when(biomeGetter).getOrThrow(ModBiomes.ION_STORM);
    }

    @Test
    @DisplayName("Alle Weltraum-Registry-Schlüssel liegen in der Mod-Namespace mit erwarteten Pfaden")
    void allKeysLiveInModNamespace() {
        assertEquals("lit_spaceships", ModDimensions.SPACE_BIOME.location().getNamespace());
        assertEquals("space_biome", ModDimensions.SPACE_BIOME.location().getPath());

        assertKey(ModConfiguredFeatures.ASTEROID, Registries.CONFIGURED_FEATURE, "asteroid");
        assertKey(ModConfiguredFeatures.SPACE_WRECK, Registries.CONFIGURED_FEATURE, "space_wreck");
        assertKey(ModConfiguredFeatures.ICE_COMET, Registries.CONFIGURED_FEATURE, "ice_comet");
        assertKey(ModPlacedFeatures.ASTEROID_PLACED, Registries.PLACED_FEATURE, "asteroid_placed");
        assertKey(ModPlacedFeatures.SPACE_WRECK_PLACED, Registries.PLACED_FEATURE, "space_wreck_placed");
        assertKey(ModPlacedFeatures.ICE_COMET_PLACED, Registries.PLACED_FEATURE, "ice_comet_placed");
        assertKey(ModConfiguredFeatures.MEGA_ASTEROID, Registries.CONFIGURED_FEATURE, "mega_asteroid");
        assertKey(ModConfiguredFeatures.PLANETARY_RING, Registries.CONFIGURED_FEATURE, "planetary_ring");
        assertKey(ModConfiguredFeatures.ASTEROID_BELT, Registries.CONFIGURED_FEATURE, "asteroid_belt");
        assertKey(ModPlacedFeatures.MEGA_ASTEROID_PLACED, Registries.PLACED_FEATURE, "mega_asteroid_placed");
        assertKey(ModPlacedFeatures.PLANETARY_RING_PLACED, Registries.PLACED_FEATURE, "planetary_ring_placed");
        assertKey(ModPlacedFeatures.ASTEROID_BELT_PLACED, Registries.PLACED_FEATURE, "asteroid_belt_placed");
        assertKey(ModConfiguredFeatures.SATELLITE_GRAVEYARD, Registries.CONFIGURED_FEATURE, "satellite_graveyard");
        assertKey(ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED, Registries.PLACED_FEATURE, "satellite_graveyard_placed");
        assertKey(ModConfiguredFeatures.COSMIC_JELLYFISH, Registries.CONFIGURED_FEATURE, "cosmic_jellyfish");
        assertKey(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED, Registries.PLACED_FEATURE, "cosmic_jellyfish_placed");
        for (String[] pair : new String[][]{
                {"gravity_rift_disk", "stellar_flare", "ion_pylon"}}) {
            for (String feature : pair) {
                assertKey(ModAmbientFeatures.cfgForTest(feature), Registries.CONFIGURED_FEATURE, feature);
                assertKey(ModAmbientFeatures.placedForTest(feature + "_placed"), Registries.PLACED_FEATURE, feature + "_placed");
            }
        }
        assertKey(ModConfiguredFeatures.ANCIENT_BATTLEFIELD, Registries.CONFIGURED_FEATURE, "ancient_battlefield");
        assertKey(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED, Registries.PLACED_FEATURE, "ancient_battlefield_placed");
        assertEquals("lit_spaceships", ModBiomes.GRAVITY_RIFT.location().getNamespace());
        assertEquals("gravity_rift", ModBiomes.GRAVITY_RIFT.location().getPath());
        assertEquals("stellar_corona", ModBiomes.STELLAR_CORONA.location().getPath());
        assertEquals("ion_storm", ModBiomes.ION_STORM.location().getPath());
        for (String[] name : new String[][]{
                {"debris_field", "meteor_shower", "void_crystal_spike", "beacon_pylon", "cargo_pod"},
                {"nebula_spore_drift", "plasma_ember", "nebula_gas_bloom", "crystal_lattice", "nebula_arc"},
                {"ice_shard_field", "glacier_floe", "frost_pillar", "snow_bloom", "cryo_geode"},
                {"bone_debris", "scrap_wasteland", "dust_drift", "ash_vent", "void_cyst"}}) {
            for (String feature : name) {
                ResourceKey<ConfiguredFeature<?, ?>> cfg = ModAmbientFeatures.cfgForTest(feature);
                assertKey(cfg, Registries.CONFIGURED_FEATURE, feature);
                assertKey(ModAmbientFeatures.placedForTest(feature + "_placed"), Registries.PLACED_FEATURE, feature + "_placed");
            }
        }
        assertKey(ModPlacedFeatures.WRECK_FIELD_PLACED, Registries.PLACED_FEATURE, "wreck_field_placed");
    }

    private void assertKey(ResourceKey<?> key, ResourceKey<? extends net.minecraft.core.Registry<?>> registry, String path) {
        assertEquals(registry.location(), key.registry());
        assertEquals(ResourceLocation.fromNamespaceAndPath("lit_spaceships", path), key.location());
    }

    @Test
    @DisplayName("ConfiguredFeature-Bootstrap registriert Asteroid, Wrack und Eiskomet")
    void configuredFeatureBootstrapRegistersBothFeatures() {
        AsteroidFeature asteroid = new AsteroidFeature(NoneFeatureConfiguration.CODEC);
        SpaceWreckFeature wreck = new SpaceWreckFeature(NoneFeatureConfiguration.CODEC);
        IceCometFeature iceComet = new IceCometFeature(NoneFeatureConfiguration.CODEC);
        MegaAsteroidFeature megaAsteroid = new MegaAsteroidFeature(NoneFeatureConfiguration.CODEC);
        PlanetaryRingFeature planetaryRing = new PlanetaryRingFeature(NoneFeatureConfiguration.CODEC);
        AsteroidBeltFeature asteroidBelt = new AsteroidBeltFeature(NoneFeatureConfiguration.CODEC);
        com.lit.spaceships.world.feature.SatelliteGraveyardFeature satelliteGraveyard =
                new com.lit.spaceships.world.feature.SatelliteGraveyardFeature(NoneFeatureConfiguration.CODEC);
        com.lit.spaceships.world.feature.CosmicJellyfishFeature cosmicJellyfish =
                new com.lit.spaceships.world.feature.CosmicJellyfishFeature(NoneFeatureConfiguration.CODEC);
        com.lit.spaceships.world.feature.AncientBattlefieldFeature ancientBattlefield =
                new com.lit.spaceships.world.feature.AncientBattlefieldFeature(NoneFeatureConfiguration.CODEC);

        ModConfiguredFeatures.bootstrapWith(configuredContext, asteroid, wreck, iceComet, megaAsteroid, planetaryRing, asteroidBelt, satelliteGraveyard, cosmicJellyfish, ancientBattlefield);

        ArgumentCaptor<ConfiguredFeature<?, ?>> captor = ArgumentCaptor.forClass(ConfiguredFeature.class);
        verify(configuredContext).register(eq(ModConfiguredFeatures.ASTEROID), captor.capture());
        verify(configuredContext).register(eq(ModConfiguredFeatures.SPACE_WRECK), any());
        verify(configuredContext).register(eq(ModConfiguredFeatures.ICE_COMET), any());
        verify(configuredContext).register(eq(ModConfiguredFeatures.MEGA_ASTEROID), any());
        verify(configuredContext).register(eq(ModConfiguredFeatures.PLANETARY_RING), any());
        verify(configuredContext).register(eq(ModConfiguredFeatures.ASTEROID_BELT), any());
        verify(configuredContext).register(eq(ModConfiguredFeatures.SATELLITE_GRAVEYARD), any());
        verify(configuredContext).register(eq(ModConfiguredFeatures.COSMIC_JELLYFISH), any());
        verify(configuredContext).register(eq(ModConfiguredFeatures.ANCIENT_BATTLEFIELD), any());

        ConfiguredFeature<?, ?> registered = captor.getValue();
        assertSame(asteroid, registered.feature());
        assertSame(NoneFeatureConfiguration.INSTANCE, registered.config());
    }

    @Test
    @DisplayName("Asteroiden-Platzierung: Count 1 (Rebalance), InSquare, Uniformhöhe -40..280, Biome-Filter")
    void asteroidPlacementMathIsBounded() {
        List<PlacementModifier> modifiers = ModPlacedFeatures.asteroidPlacement();

        assertEquals(4, modifiers.size());
        assertInstanceOf(CountPlacement.class, modifiers.get(0));
        assertSame(InSquarePlacement.spread(), modifiers.get(1));
        assertInstanceOf(HeightRangePlacement.class, modifiers.get(2));
        assertSame(BiomeFilter.biome(), modifiers.get(3));

        CountPlacement count = (CountPlacement) modifiers.get(0);
        IntProvider provider = privateField(count, "count", IntProvider.class);
        assertSame(ConstantInt.class, provider.getClass());
        assertEquals(1, provider.getMinValue());
        assertEquals(1, provider.getMaxValue());

        HeightRangePlacement height = (HeightRangePlacement) modifiers.get(2);
        UniformHeight uniform = privateField(height, "height", UniformHeight.class);
        assertEquals(-40, absoluteAnchorY(privateField(uniform, "minInclusive", VerticalAnchor.class)));
        assertEquals(280, absoluteAnchorY(privateField(uniform, "maxInclusive", VerticalAnchor.class)));
    }

    @Test
    @DisplayName("Asteroiden-Erz-Kern: bewusst selten (Diamant 3%, Debris 2%) nach Rebalance")
    void asteroidOreRatesAreRebalanced() {
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(4242L);
        int diamond = 0, debris = 0, gold = 0, iron = 0;
        for (int i = 0; i < 100_000; i++) {
            BlockState state = AsteroidFeature.determineBlockState(0, 0.0D, random);
            if (state.is(Blocks.DIAMOND_ORE)) diamond++;
            if (state.is(Blocks.ANCIENT_DEBRIS)) debris++;
            if (state.is(Blocks.RAW_GOLD_BLOCK)) gold++;
            if (state.is(Blocks.RAW_IRON_BLOCK)) iron++;
        }
        assertEquals(0.03D, diamond / 100_000.0D, 0.005D, "Diamant-Kernrate ~3%");
        assertEquals(0.02D, debris / 100_000.0D, 0.005D, "Ancient-Debris-Kernrate ~2%");
        assertEquals(0.06D, gold / 100_000.0D, 0.005D, "Raw-Gold-Kernrate ~6%");
        assertEquals(0.10D, iron / 100_000.0D, 0.005D, "Raw-Iron-Kernrate ~10%");
    }

    @Test
    @DisplayName("Wrack-Platzierung: Rarity 1/32, InSquare, Uniformhöhe 0..200, Biome-Filter")
    void spaceWreckPlacementMathIsBounded() {
        List<PlacementModifier> modifiers = ModPlacedFeatures.spaceWreckPlacement();

        assertEquals(4, modifiers.size());
        assertInstanceOf(RarityFilter.class, modifiers.get(0));
        assertSame(InSquarePlacement.spread(), modifiers.get(1));
        assertInstanceOf(HeightRangePlacement.class, modifiers.get(2));
        assertSame(BiomeFilter.biome(), modifiers.get(3));

        RarityFilter rarity = (RarityFilter) modifiers.get(0);
        assertEquals(32, (int) privateField(rarity, "chance", Integer.class));

        HeightRangePlacement height = (HeightRangePlacement) modifiers.get(2);
        UniformHeight uniform = privateField(height, "height", UniformHeight.class);
        assertEquals(0, absoluteAnchorY(privateField(uniform, "minInclusive", VerticalAnchor.class)));
        assertEquals(200, absoluteAnchorY(privateField(uniform, "maxInclusive", VerticalAnchor.class)));
    }

    @Test
    @DisplayName("PlacedFeature-Bootstrap verknüpft die ConfiguredFeatures korrekt")
    void placedFeatureBootstrapResolvesConfiguredFeatures() {
        Holder.Reference<ConfiguredFeature<?, ?>> asteroidCf =
                Holder.Reference.createStandAlone(configuredOwner, ModConfiguredFeatures.ASTEROID);
        Holder.Reference<ConfiguredFeature<?, ?>> wreckCf =
                Holder.Reference.createStandAlone(configuredOwner, ModConfiguredFeatures.SPACE_WRECK);
        when(placedContext.lookup(Registries.CONFIGURED_FEATURE)).thenReturn(configuredGetter);
        doReturn(asteroidCf).when(configuredGetter).getOrThrow(ModConfiguredFeatures.ASTEROID);
        doReturn(wreckCf).when(configuredGetter).getOrThrow(ModConfiguredFeatures.SPACE_WRECK);

        ModPlacedFeatures.bootstrap(placedContext);

        ArgumentCaptor<PlacedFeature> captor = ArgumentCaptor.forClass(PlacedFeature.class);
        verify(placedContext).register(eq(ModPlacedFeatures.ASTEROID_PLACED), captor.capture());
        verify(placedContext).register(eq(ModPlacedFeatures.SPACE_WRECK_PLACED), any());

        PlacedFeature asteroid = captor.getValue();
        assertSame(asteroidCf, asteroid.feature());
        assertEquals(4, asteroid.placement().size());
    }

    @Test
    @DisplayName("Space-Biome: absolute Dunkelheit, keine Spawns, beide Features in Stufe 0")
    void spaceBiomeIsVoidLikeAndReferencesSpaceFeatures() {
        Holder.Reference<PlacedFeature> asteroidPlaced =
                Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ASTEROID_PLACED);
        Holder.Reference<PlacedFeature> wreckPlaced =
                Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.SPACE_WRECK_PLACED);
        when(biomeContext.lookup(Registries.PLACED_FEATURE)).thenReturn(placedGetter);
        doReturn(asteroidPlaced).when(placedGetter).getOrThrow(ModPlacedFeatures.ASTEROID_PLACED);
        doReturn(wreckPlaced).when(placedGetter).getOrThrow(ModPlacedFeatures.SPACE_WRECK_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ICE_COMET_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ICE_COMET_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.WRECK_FIELD_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.WRECK_FIELD_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.MEGA_ASTEROID_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.MEGA_ASTEROID_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.PLANETARY_RING_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.PLANETARY_RING_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ASTEROID_BELT_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ASTEROID_BELT_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.COSMIC_JELLYFISH_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED);

        stubAmbientPlacedFeatures();
        ModBiomes.bootstrap(biomeContext);

        ArgumentCaptor<Biome> captor = ArgumentCaptor.forClass(Biome.class);
        verify(biomeContext).register(eq(ModDimensions.SPACE_BIOME), captor.capture());

        Biome biome = captor.getValue();
        assertFalse(biome.hasPrecipitation());
        assertEquals(0.0F, biome.getBaseTemperature());
        assertEquals(0, biome.getFogColor());
        assertEquals(0, biome.getSkyColor());
        assertEquals(328981, biome.getWaterColor());
        assertEquals(328981, biome.getWaterFogColor());
        assertTrue(biome.getSpecialEffects().getGrassColorOverride().isPresent());
        assertTrue(biome.getSpecialEffects().getFoliageColorOverride().isPresent());

        // Null natürliche Mob-Spawns
        for (MobCategory category : MobCategory.values()) {
            assertTrue(biome.getMobSettings().getMobs(category).isEmpty(), category.getName());
        }

        // Features: Asteroid + Wrack in Deko-Stufe 0 (entspricht alter features[0]-Liste)
        BiomeGenerationSettings generation = biome.getGenerationSettings();
        List<HolderSet<PlacedFeature>> featureSteps = generation.features();
        assertEquals(1, featureSteps.size());
        List<Holder<PlacedFeature>> stepZero = featureSteps.get(0).stream().toList();
        assertTrue(stepZero.contains(asteroidPlaced));
        assertTrue(stepZero.contains(wreckPlaced));
    }

    @Test
    @DisplayName("Void Wastes: vollkommene Schwärze ohne Partikel, mit Asteroiden und dichtem Wrack-Feld")
    void voidWastesIsDeprivedDerelictZone() {
        when(biomeContext.lookup(Registries.PLACED_FEATURE)).thenReturn(placedGetter);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ASTEROID_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ASTEROID_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.SPACE_WRECK_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.SPACE_WRECK_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ICE_COMET_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ICE_COMET_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.WRECK_FIELD_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.WRECK_FIELD_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.MEGA_ASTEROID_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.MEGA_ASTEROID_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.PLANETARY_RING_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.PLANETARY_RING_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ASTEROID_BELT_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ASTEROID_BELT_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.COSMIC_JELLYFISH_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED);

        stubAmbientPlacedFeatures();
        ModBiomes.bootstrap(biomeContext);

        ArgumentCaptor<Biome> captor = ArgumentCaptor.forClass(Biome.class);
        verify(biomeContext).register(eq(ModBiomes.VOID_WASTES), captor.capture());

        Biome wastes = captor.getValue();
        assertFalse(wastes.hasPrecipitation());
        // Vollkommene Schwärze + sensorische Deprivation: kein Partikel, kein Mood-Sound
        assertEquals(0, wastes.getFogColor());
        assertEquals(0, wastes.getSkyColor());
        assertTrue(wastes.getAmbientParticle().isEmpty());
        assertTrue(wastes.getAmbientMood().isEmpty());

        for (MobCategory category : MobCategory.values()) {
            assertTrue(wastes.getMobSettings().getMobs(category).isEmpty(), category.getName());
        }

        // Derelict Spawn Area: normale Asteroiden + dichtes Wrack-Feld + seltene Mega-Asteroiden in Stufe 0
        List<HolderSet<PlacedFeature>> featureSteps = wastes.getGenerationSettings().features();
        assertEquals(1, featureSteps.size());
        List<ResourceKey<PlacedFeature>> stepZeroKeys = featureSteps.get(0).stream()
                .map(holder -> holder.unwrapKey().orElseThrow())
                .toList();
        assertEquals(List.of(ModPlacedFeatures.ASTEROID_PLACED, ModPlacedFeatures.WRECK_FIELD_PLACED,
                ModPlacedFeatures.MEGA_ASTEROID_PLACED, ModPlacedFeatures.PLANETARY_RING_PLACED,
                ModPlacedFeatures.ASTEROID_BELT_PLACED, ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED,
                ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED, ModAmbientFeatures.BONE_DEBRIS_PLACED,
                ModAmbientFeatures.SCRAP_WASTELAND_PLACED, ModAmbientFeatures.DUST_DRIFT_PLACED,
                ModAmbientFeatures.ASH_VENT_PLACED, ModAmbientFeatures.VOID_CYST_PLACED), stepZeroKeys);
    }

    @Test
    @DisplayName("Mega-Asteroid-Platzierung: Rarity 1/96 (Chunk-Budget), Uniformhöhe -40..280, Biome-Filter")
    void megaAsteroidPlacementMathIsBounded() {
        List<PlacementModifier> modifiers = ModPlacedFeatures.megaAsteroidPlacement();

        assertEquals(4, modifiers.size());
        assertInstanceOf(RarityFilter.class, modifiers.get(0));
        assertSame(InSquarePlacement.spread(), modifiers.get(1));
        assertInstanceOf(HeightRangePlacement.class, modifiers.get(2));
        assertSame(BiomeFilter.biome(), modifiers.get(3));

        RarityFilter rarity = (RarityFilter) modifiers.get(0);
        assertEquals(96, (int) privateField(rarity, "chance", Integer.class));

        HeightRangePlacement height = (HeightRangePlacement) modifiers.get(2);
        UniformHeight uniform = privateField(height, "height", UniformHeight.class);
        assertEquals(-40, absoluteAnchorY(privateField(uniform, "minInclusive", VerticalAnchor.class)));
        assertEquals(280, absoluteAnchorY(privateField(uniform, "maxInclusive", VerticalAnchor.class)));
    }

    @Test
    @DisplayName("Mega-Asteroid radial: Geode (Kalzit/Amethyst/Luft), Kaverne, Mantel, Kruste, außen null")
    void megaAsteroidRadialLayersAreDeterministic() {
        RandomSource random = RandomSource.create(7L);

        // Außerhalb des Ellipsoids: nichts setzen
        assertNull(MegaAsteroidFeature.radialLayer(1.05D, 100.0D, random, 3));

        // Kruste (norm 0.9): fest, Steinarten
        BlockState crust = MegaAsteroidFeature.radialLayer(0.9D, 100.0D, random, 3);
        assertFalse(crust.isAir());
        assertTrue(crust.is(Blocks.ANDESITE) || crust.is(Blocks.BASALT)
                || crust.is(Blocks.COBBLESTONE) || crust.is(Blocks.STONE));

        // Mantel (norm 0.7): fest, Erz-Deepslate-Mischung
        BlockState mantle = MegaAsteroidFeature.radialLayer(0.7D, 100.0D, random, 3);
        assertFalse(mantle.isAir());

        // Hohler Kavernenraum (norm 0.5 <= 0.55, außerhalb der Geode dist 4 > 3): Luft
        assertEquals(Blocks.AIR, MegaAsteroidFeature.radialLayer(0.5D, 4.0D, random, 3).getBlock());

        // Kalzit-Geodenhülle (dist 3 == geodeOuter)
        assertEquals(Blocks.CALCITE, MegaAsteroidFeature.radialLayer(0.3D, 3.0D, random, 3).getBlock());

        // Amethyst-Mantel (dist 2 == geodeOuter - 1)
        BlockState amethyst = MegaAsteroidFeature.radialLayer(0.2D, 2.0D, random, 3);
        assertTrue(amethyst.is(Blocks.AMETHYST_BLOCK) || amethyst.is(Blocks.BUDDING_AMETHYST));

        // Geodenkammer (dist 1 <= geodeOuter - 2): Luft
        assertEquals(Blocks.AIR, MegaAsteroidFeature.radialLayer(0.1D, 1.0D, random, 3).getBlock());

        // Determinismus: identischer Seed liefert identische Schichtfolge
        RandomSource a = RandomSource.create(99L);
        RandomSource b = RandomSource.create(99L);
        for (int i = 0; i < 20; i++) {
            assertEquals(MegaAsteroidFeature.radialLayer(0.7D, 100.0D, a, 3).getBlock(),
                    MegaAsteroidFeature.radialLayer(0.7D, 100.0D, b, 3).getBlock());
        }
    }

    @Test
    @DisplayName("Wrack-Feld-Platzierung: Rarity 1/10 (Rebalance), InSquare, Uniformhöhe 0..200, Biome-Filter")
    void wreckFieldPlacementMathIsBounded() {
        List<PlacementModifier> modifiers = ModPlacedFeatures.wreckFieldPlacement();

        assertEquals(4, modifiers.size());
        assertInstanceOf(RarityFilter.class, modifiers.get(0));
        assertSame(InSquarePlacement.spread(), modifiers.get(1));
        assertInstanceOf(HeightRangePlacement.class, modifiers.get(2));
        assertSame(BiomeFilter.biome(), modifiers.get(3));

        RarityFilter rarity = (RarityFilter) modifiers.get(0);
        assertEquals(10, (int) privateField(rarity, "chance", Integer.class));

        HeightRangePlacement height = (HeightRangePlacement) modifiers.get(2);
        UniformHeight uniform = privateField(height, "height", UniformHeight.class);
        assertEquals(0, absoluteAnchorY(privateField(uniform, "minInclusive", VerticalAnchor.class)));
        assertEquals(200, absoluteAnchorY(privateField(uniform, "maxInclusive", VerticalAnchor.class)));
    }

    @Test
    @DisplayName("Plasma-Nebel: violetter Nebel #7F00FF, glühende Partikel, keine Spawns, keine Features")
    void plasmaNebulaIsGlowingVioletZone() {
        when(biomeContext.lookup(Registries.PLACED_FEATURE)).thenReturn(placedGetter);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ASTEROID_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ASTEROID_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.SPACE_WRECK_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.SPACE_WRECK_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ICE_COMET_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ICE_COMET_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.WRECK_FIELD_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.WRECK_FIELD_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.MEGA_ASTEROID_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.MEGA_ASTEROID_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.PLANETARY_RING_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.PLANETARY_RING_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ASTEROID_BELT_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ASTEROID_BELT_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.COSMIC_JELLYFISH_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED);

        stubAmbientPlacedFeatures();
        ModBiomes.bootstrap(biomeContext);

        ArgumentCaptor<Biome> captor = ArgumentCaptor.forClass(Biome.class);
        verify(biomeContext).register(eq(ModBiomes.PLASMA_NEBULA), captor.capture());
        verify(biomeContext).register(eq(ModDimensions.SPACE_BIOME), any());

        Biome nebula = captor.getValue();
        assertFalse(nebula.hasPrecipitation());
        assertEquals(0x7F00FF, nebula.getFogColor());
        assertEquals(0x1A0033, nebula.getSkyColor());
        assertEquals(0x1A0033, nebula.getWaterColor());
        assertEquals(0x1A0033, nebula.getWaterFogColor());

        java.util.Optional<net.minecraft.world.level.biome.AmbientParticleSettings> particle = nebula.getAmbientParticle();
        assertTrue(particle.isPresent());
        // Nebel-Atmen: Seelenwind-Loop + Warped-Mood
        assertSame(net.minecraft.sounds.SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP,
                nebula.getAmbientLoop().orElseThrow());
        assertTrue(nebula.getAmbientMood().isPresent());
        assertTrue(nebula.getAmbientAdditions().isPresent());
        DustParticleOptions dust = assertType(DustParticleOptions.class, particle.get().getOptions());
        assertEquals(0.498F, dust.getColor().x(), 0.0001F);
        assertEquals(0.0F, dust.getColor().y(), 0.0001F);
        assertEquals(1.0F, dust.getColor().z(), 0.0001F);
        assertEquals(0.006F, (float) privateField(particle.get(), "probability", Float.class), 0.0F);

        for (MobCategory category : MobCategory.values()) {
            assertTrue(nebula.getMobSettings().getMobs(category).isEmpty(), category.getName());
        }
        // Nebula hat jetzt ihre ersten Features: die leuchtenden Quallen
        List<net.minecraft.core.HolderSet<PlacedFeature>> nebulaSteps = nebula.getGenerationSettings().features();
        assertEquals(1, nebulaSteps.size());
        List<ResourceKey<PlacedFeature>> nebulaKeys = nebulaSteps.get(0).stream()
                .map(h -> h.unwrapKey().orElseThrow()).toList();
        assertEquals(List.of(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED,
                ModAmbientFeatures.NEBULA_SPORE_DRIFT_PLACED, ModAmbientFeatures.PLASMA_EMBER_PLACED,
                ModAmbientFeatures.NEBULA_GAS_BLOOM_PLACED, ModAmbientFeatures.CRYSTAL_LATTICE_PLACED,
                ModAmbientFeatures.NEBULA_ARC_PLACED), nebulaKeys);
    }

    @Test
    @DisplayName("Multi-Noise-Verteilung: 4 Rechtecke über Temperatur und Feuchte, lückenlos geroutet")
    void spaceBiomeDistributionRoutesTemperature() {
        Climate.ParameterList<ResourceKey<Biome>> distribution = ModDimensions.spaceBiomeDistribution();

        assertSame(ModBiomes.FROZEN_EXPANSE,
                distribution.findValue(new Climate.TargetPoint(-8000L, 0L, 0L, 0L, 0L, 0L)));
        assertSame(ModBiomes.VOID_WASTES,
                distribution.findValue(new Climate.TargetPoint(0L, -8000L, 0L, 0L, 0L, 0L)));
        assertSame(ModDimensions.SPACE_BIOME,
                distribution.findValue(new Climate.TargetPoint(0L, 8000L, 0L, 0L, 0L, 0L)));
        assertSame(ModBiomes.PLASMA_NEBULA,
                distribution.findValue(new Climate.TargetPoint(8000L, 0L, 0L, 0L, 0L, 0L)));
        assertSame(ModBiomes.VOID_WASTES,
                distribution.findValue(new Climate.TargetPoint(1000L, -5000L, 0L, 0L, 0L, 0L)));
        assertSame(ModBiomes.FROZEN_EXPANSE,
                distribution.findValue(new Climate.TargetPoint(-5000L, 9000L, 0L, 0L, 0L, 0L)));
    }

    @Test
    @DisplayName("Space-Noise-Settings: reiner Void mit negativer Dichte, Temperatur- und Feuchte-Noise")
    void spaceNoiseSettingsAreVoidWithTemperatureNoise() {
        Holder<NormalNoise.NoiseParameters> temperatureNoise =
                Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0));
        Holder<NormalNoise.NoiseParameters> vegetationNoise =
                Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0));
        Holder<NormalNoise.NoiseParameters> continentalnessNoise =
                Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0));
        Holder<NormalNoise.NoiseParameters> erosionNoise =
                Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0));

        NoiseGeneratorSettings settings = ModNoiseSettings.spaceNoiseSettings(
                temperatureNoise, vegetationNoise, continentalnessNoise, erosionNoise);

        assertEquals(-64, settings.seaLevel());
        assertTrue(settings.disableMobGeneration());
        assertFalse(settings.aquifersEnabled());
        assertFalse(settings.oreVeinsEnabled());
        assertFalse(settings.useLegacyRandomSource());
        assertTrue(settings.defaultBlock().isAir());
        assertTrue(settings.defaultFluid().isAir());

        net.minecraft.world.level.levelgen.NoiseSettings shape = settings.noiseSettings();
        assertEquals(-64, shape.minY());
        assertEquals(384, shape.height());
        assertEquals(1, shape.noiseSizeHorizontal());
        assertEquals(2, shape.noiseSizeVertical());

        NoiseRouter router = settings.noiseRouter();
        assertTrue(router.temperature().minValue() < 0.0 && router.temperature().maxValue() > 0.0,
                "Temperatur muss echte Noise-Struktur besitzen (Multi-Noise-Routing)");
        assertTrue(router.vegetation().minValue() < 0.0 && router.vegetation().maxValue() > 0.0,
                "Feuchteachse (vegetation) muss echte Noise-Struktur besitzen");
        assertTrue(router.continents().minValue() < 0.0 && router.continents().maxValue() > 0.0,
                "Continentalness-Achse (C) muss echte Noise-Struktur besitzen");
        assertTrue(router.ridges().minValue() < 0.0 && router.ridges().maxValue() > 0.0,
                "Ridges-Achse (W) muss echte Noise-Struktur besitzen");
        assertEquals(0.0, router.erosion().minValue(), 0.0, "Erosionsachse bleibt ungenutzt");
        assertEquals(-1.0D, router.finalDensity().minValue(), 0.0D);
        assertEquals(-1.0D, router.finalDensity().maxValue(), 0.0D);
    }

    @Test
    @DisplayName("Space-Dimension-Typ: kosmische Nacht, kein Skylight, Ankern erlaubt, Null-Spawn-Licht")
    void spaceDimensionTypeMatchesCosmicNight() {
        DimensionType type = ModDimensions.spaceDimensionType();

        assertEquals(18000L, type.fixedTime().getAsLong());
        assertFalse(type.hasSkyLight());
        assertFalse(type.hasCeiling());
        assertFalse(type.ultraWarm());
        assertFalse(type.natural());
        assertEquals(1.0D, type.coordinateScale());
        assertFalse(type.bedWorks());
        assertTrue(type.respawnAnchorWorks());
        assertEquals(-64, type.minY());
        assertEquals(384, type.height());
        assertEquals(384, type.logicalHeight());
        assertEquals(0.0F, type.ambientLight());
        assertEquals(ResourceLocation.withDefaultNamespace("overworld"), type.effectsLocation());

        DimensionType.MonsterSettings monsterSettings = type.monsterSettings();
        assertEquals(0, monsterSettings.monsterSpawnLightTest().getMinValue());
        assertEquals(0, monsterSettings.monsterSpawnLightTest().getMaxValue());
        assertEquals(0, monsterSettings.monsterSpawnBlockLightLimit());
        assertFalse(monsterSettings.piglinSafe());
        assertFalse(monsterSettings.hasRaids());
    }

    @Test
    @DisplayName("LevelStem verdrahtet Noise-Generator mit Multi-Noise-Quelle über beide Weltraum-Biome")
    void levelStemWiresMultiNoiseGenerator() {
        stubAllBiomes();

        Holder<NoiseGeneratorSettings> settingsHolder =
                Holder.direct(ModNoiseSettings.spaceNoiseSettings(
                        Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0)),
                        Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0)),
                        Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0)),
                        Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0))));
        Holder<DimensionType> typeHolder = Holder.direct(ModDimensions.spaceDimensionType());

        LevelStem stem = ModDimensions.spaceLevelStem(biomeGetter, settingsHolder, typeHolder);

        assertSame(typeHolder, stem.type());
        NoiseBasedChunkGenerator generator = assertType(NoiseBasedChunkGenerator.class, stem.generator());
        assertSame(settingsHolder, generator.generatorSettings());

        Set<ResourceKey<Biome>> possibleBiomes = generator.getBiomeSource().possibleBiomes().stream()
                .map(holder -> holder.unwrapKey().orElseThrow())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of(ModDimensions.SPACE_BIOME, ModBiomes.PLASMA_NEBULA, ModBiomes.FROZEN_EXPANSE,
                ModBiomes.VOID_WASTES, ModBiomes.GRAVITY_RIFT, ModBiomes.STELLAR_CORONA,
                ModBiomes.ION_STORM), possibleBiomes);
    }

    @Test
    @DisplayName("Frozen Expanse: cyanfarbene Atmosphäre, Schneeflocken-Partikel, Eis-Kometen-Felder")
    void frozenExpanseIsIcyCyanZone() {
        when(biomeContext.lookup(Registries.PLACED_FEATURE)).thenReturn(placedGetter);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ASTEROID_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ASTEROID_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.SPACE_WRECK_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.SPACE_WRECK_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ICE_COMET_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ICE_COMET_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.WRECK_FIELD_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.WRECK_FIELD_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.MEGA_ASTEROID_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.MEGA_ASTEROID_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.PLANETARY_RING_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.PLANETARY_RING_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ASTEROID_BELT_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ASTEROID_BELT_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.COSMIC_JELLYFISH_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED);

        stubAmbientPlacedFeatures();
        ModBiomes.bootstrap(biomeContext);

        ArgumentCaptor<Biome> captor = ArgumentCaptor.forClass(Biome.class);
        verify(biomeContext).register(eq(ModBiomes.FROZEN_EXPANSE), captor.capture());

        Biome frozen = captor.getValue();
        assertFalse(frozen.hasPrecipitation());
        assertEquals(0x00FFFF, frozen.getFogColor());
        assertEquals(0x003344, frozen.getSkyColor());
        assertTrue(frozen.getAmbientParticle().isPresent());
        assertInstanceOf(net.minecraft.core.particles.SimpleParticleType.class, frozen.getAmbientParticle().get().getOptions());
        assertSame(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, frozen.getAmbientParticle().get().getOptions());
        assertEquals(0.015F, (float) privateField(frozen.getAmbientParticle().get(), "probability", Float.class), 0.0F);

        for (MobCategory category : MobCategory.values()) {
            assertTrue(frozen.getMobSettings().getMobs(category).isEmpty(), category.getName());
        }

        // Hochdichte Eiskometen-Felder + seltene Mega-Asteroiden in Deko-Stufe 0
        List<HolderSet<PlacedFeature>> featureSteps = frozen.getGenerationSettings().features();
        assertEquals(1, featureSteps.size());
        List<ResourceKey<PlacedFeature>> stepZeroKeys = featureSteps.get(0).stream()
                .map(holder -> holder.unwrapKey().orElseThrow())
                .toList();
        assertEquals(List.of(ModPlacedFeatures.ICE_COMET_PLACED, ModPlacedFeatures.MEGA_ASTEROID_PLACED,
                ModPlacedFeatures.PLANETARY_RING_PLACED, ModPlacedFeatures.ASTEROID_BELT_PLACED,
                ModAmbientFeatures.ICE_SHARD_FIELD_PLACED, ModAmbientFeatures.GLACIER_FLOE_PLACED,
                ModAmbientFeatures.FROST_PILLAR_PLACED, ModAmbientFeatures.SNOW_BLOOM_PLACED,
                ModAmbientFeatures.CRYO_GEODE_PLACED), stepZeroKeys);
    }

    @Test
    @DisplayName("Eiskomet-Platzierung: hohe Dichte Count 8, InSquare, Uniformhöhe -40..280, Biome-Filter")
    void iceCometPlacementMathIsBounded() {
        List<PlacementModifier> modifiers = ModPlacedFeatures.iceCometPlacement();

        assertEquals(4, modifiers.size());
        assertInstanceOf(CountPlacement.class, modifiers.get(0));
        assertSame(InSquarePlacement.spread(), modifiers.get(1));
        assertInstanceOf(HeightRangePlacement.class, modifiers.get(2));
        assertSame(BiomeFilter.biome(), modifiers.get(3));

        CountPlacement count = (CountPlacement) modifiers.get(0);
        IntProvider provider = privateField(count, "count", IntProvider.class);
        assertEquals(8, provider.getMinValue());
        assertEquals(8, provider.getMaxValue());

        HeightRangePlacement height = (HeightRangePlacement) modifiers.get(2);
        UniformHeight uniform = privateField(height, "height", UniformHeight.class);
        assertEquals(-40, absoluteAnchorY(privateField(uniform, "minInclusive", VerticalAnchor.class)));
        assertEquals(280, absoluteAnchorY(privateField(uniform, "maxInclusive", VerticalAnchor.class)));
    }

    @Test
    @DisplayName("PlacedFeature-Bootstrap verknüpft auch den Eiskometen mit der hohen Dichte")
    void placedFeatureBootstrapResolvesIceComet() {
        when(placedContext.lookup(Registries.CONFIGURED_FEATURE)).thenReturn(configuredGetter);
        doReturn(Holder.Reference.createStandAlone(configuredOwner, ModConfiguredFeatures.ASTEROID))
                .when(configuredGetter).getOrThrow(ModConfiguredFeatures.ASTEROID);
        doReturn(Holder.Reference.createStandAlone(configuredOwner, ModConfiguredFeatures.SPACE_WRECK))
                .when(configuredGetter).getOrThrow(ModConfiguredFeatures.SPACE_WRECK);
        doReturn(Holder.Reference.createStandAlone(configuredOwner, ModConfiguredFeatures.ICE_COMET))
                .when(configuredGetter).getOrThrow(ModConfiguredFeatures.ICE_COMET);

        ModPlacedFeatures.bootstrap(placedContext);

        ArgumentCaptor<PlacedFeature> captor = ArgumentCaptor.forClass(PlacedFeature.class);
        verify(placedContext).register(eq(ModPlacedFeatures.ICE_COMET_PLACED), captor.capture());

        PlacedFeature iceComet = captor.getValue();
        assertEquals(ModConfiguredFeatures.ICE_COMET, iceComet.feature().unwrapKey().orElseThrow());
        assertEquals(4, iceComet.placement().size());
    }

    @Test
    @DisplayName("Ringspec pro Zelle: deterministisch, Radius 100-300, Y 64-192, Dicke 1-3, Ring in Zelle geklemmt")
    void planetaryRingSpecIsDeterministicAndBounded() {
        PlanetaryRingFeature.RingSpec spec = PlanetaryRingFeature.specForCell(3, -7);
        PlanetaryRingFeature.RingSpec again = PlanetaryRingFeature.specForCell(3, -7);
        assertEquals(spec, again, "Gleiche Zelle muss identischen Ring liefern");

        assertTrue(spec.radius() >= 100.0D && spec.radius() <= 300.0D, "Radius 100-300");
        assertTrue(spec.ringY() >= 64 && spec.ringY() <= 192, "Ring-Y 64-192");
        assertTrue(spec.thickness() >= 1 && spec.thickness() <= 3, "Dicke 1-3");

        // Nahtlosigkeit: Zentrum so geklemmt, dass der Ring komplett in der Zelle bleibt
        double cellOriginX = 3 * PlanetaryRingFeature.CELL_SIZE;
        double cellOriginZ = -7 * PlanetaryRingFeature.CELL_SIZE;
        assertTrue(spec.centerX() - spec.radius() >= cellOriginX, "Ring links in der Zelle");
        assertTrue(spec.centerX() + spec.radius() <= cellOriginX + PlanetaryRingFeature.CELL_SIZE, "Ring rechts in der Zelle");
        assertTrue(spec.centerZ() - spec.radius() >= cellOriginZ, "Ring vorne in der Zelle");
        assertTrue(spec.centerZ() + spec.radius() <= cellOriginZ + PlanetaryRingFeature.CELL_SIZE, "Ring hinten in der Zelle");

        // Unterschiedliche Zellen liefern unterschiedliche Ringe
        PlanetaryRingFeature.RingSpec other = PlanetaryRingFeature.specForCell(3, -6);
        assertNotEquals(spec, other);
    }

    @Test
    @DisplayName("Ringpalette: ausschließlich Eis, gefärbtes Glas und Staub/Sediment")
    void planetaryRingPaletteIsIceGlassDust() {
        RandomSource random = RandomSource.create(5L);
        Set<Block> allowed = Set.of(Blocks.BLUE_ICE, Blocks.PACKED_ICE, Blocks.ICE,
                Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS, Blocks.WHITE_STAINED_GLASS,
                Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL);
        for (int i = 0; i < 200; i++) {
            Block block = PlanetaryRingFeature.ringBlock(random).getBlock();
            assertTrue(allowed.contains(block), "Unerwarteter Ringblock: " + block);
        }
    }

    @Test
    @DisplayName("Gürtel-Spec pro Zelle: deterministisch, korridor-geometrie und cluster-noise korrekt")
    void asteroidBeltSpecAndCorridorMath() {        AsteroidBeltFeature.BeltSpec spec = AsteroidBeltFeature.specForCell(-2, 5);
        assertEquals(spec, AsteroidBeltFeature.specForCell(-2, 5), "Gleiche Zelle muss identischen Gürtel liefern");

        assertTrue(spec.halfLength() >= 160.0D && spec.halfLength() <= 320.0D, "Halbe Länge 160-320");
        assertTrue(spec.halfWidth() >= 8.0D && spec.halfWidth() <= 16.0D, "Halbe Breite 8-16");
        assertTrue(spec.yMin() >= -32 && spec.yMax() <= 256, "Y-Band innerhalb -32..256");

        // Nahtlosigkeit: Gürtel inklusive Endpunkte vollständig in der Zelle
        double originX = -2 * AsteroidBeltFeature.CELL_SIZE;
        double originZ = 5 * AsteroidBeltFeature.CELL_SIZE;
        assertTrue(spec.centerX() - spec.halfLength() >= originX
                && spec.centerX() + spec.halfLength() <= originX + AsteroidBeltFeature.CELL_SIZE);
        assertTrue(spec.centerZ() - spec.halfLength() >= originZ
                && spec.centerZ() + spec.halfLength() <= originZ + AsteroidBeltFeature.CELL_SIZE);

        // Korridor-Geometrie: Punkt auf der Achse -> Abstand 0; seitlich -> perpendikularer Abstand
        AsteroidBeltFeature.BeltSpec axis = new AsteroidBeltFeature.BeltSpec(0, 0, 1, 0, 100, 10, 0, 64, 0);
        assertEquals(0.0D, AsteroidBeltFeature.distanceToBelt(50, 0, axis), 0.0001D);
        assertEquals(7.0D, AsteroidBeltFeature.distanceToBelt(50, 7, axis), 0.0001D);
        // Jenseits des Endpunkts (proj 150 > halfLength 100): Abstand zum Endpunkt (100, 0)
        assertEquals(Math.hypot(150.0D - 100.0D, 3.0D), AsteroidBeltFeature.distanceToBelt(150, 3, axis), 0.0001D);

        // Cluster-Noise: glatte Sinus-Dichte in [0,1], deterministisch
        double noise = AsteroidBeltFeature.clusterNoise(13.0D, 0.7D);
        assertTrue(noise >= 0.0D && noise <= 1.0D);
        assertEquals(noise, AsteroidBeltFeature.clusterNoise(13.0D, 0.7D), 0.0D);
        // Maximale Dichte bei Phase + Viertelwelle
        assertEquals(1.0D, AsteroidBeltFeature.clusterNoise(Math.PI / 2.0D / 0.25D, 0.0D), 0.0001D);
    }

    @Test
    @DisplayName("Template-Pools: Station (2), Dreadnought (2) und Alien-Monolith (1) registriert")
    void templatePoolsContainAllModules() {
        doReturn(Holder.Reference.createStandAlone(poolOwner, Pools.EMPTY))
                .when(poolGetter).getOrThrow(Pools.EMPTY);
        when(poolContext.lookup(Registries.TEMPLATE_POOL)).thenReturn(poolGetter);

        ModTemplatePools.bootstrap(poolContext);

        ArgumentCaptor<ResourceKey<StructureTemplatePool>> keyCaptor =
                ArgumentCaptor.forClass(ResourceKey.class);
        ArgumentCaptor<StructureTemplatePool> poolCaptor =
                ArgumentCaptor.forClass(StructureTemplatePool.class);
        verify(poolContext, times(17)).register(keyCaptor.capture(), poolCaptor.capture());

        java.util.Map<ResourceKey<StructureTemplatePool>, StructureTemplatePool> pools =
                new java.util.HashMap<>();
        for (int i = 0; i < keyCaptor.getAllValues().size(); i++) {
            pools.put(keyCaptor.getAllValues().get(i), poolCaptor.getAllValues().get(i));
        }

        assertEquals(1, pools.get(ModTemplatePools.SPACE_STATION_START).size());
        assertEquals(9, pools.get(ModTemplatePools.SPACE_STATION_ROOMS).size());
        assertEquals(1, pools.get(ModTemplatePools.DREADNOUGHT_WRECK_START).size());
        assertEquals(6, pools.get(ModTemplatePools.DREADNOUGHT_WRECK_SECTIONS).size());
        assertEquals(1, pools.get(ModTemplatePools.ALIEN_OUTPOST_START).size());
        assertEquals(1, pools.get(ModTemplatePools.COSMIC_VAULT_START).size());
        assertEquals(1, pools.get(ModTemplatePools.PIRATE_OUTPOST_START).size());
        assertEquals(1, pools.get(ModTemplatePools.LEVIATHAN_BONES_START).size());
        assertEquals(1, pools.get(ModTemplatePools.THE_MONOLITH_START).size());
        assertEquals(1, pools.get(ModTemplatePools.JUMP_GATE_START).size());
        assertEquals(1, pools.get(ModTemplatePools.COLONY_DOME_START).size());
        assertEquals(1, pools.get(ModTemplatePools.BEHEMOTH_BRIDGE).size());
        assertEquals(5, pools.get(ModTemplatePools.BEHEMOTH_SECTIONS).size());
        assertEquals(1, pools.get(ModTemplatePools.BEHEMOTH_END).size());
        assertEquals(1, pools.get(ModTemplatePools.RELAY_ARRAY_START).size());
        assertEquals(1, pools.get(ModTemplatePools.SOLAR_COLLECTOR_START).size());
        assertEquals(1, pools.get(ModTemplatePools.DEEP_OUTPOST_START).size());
    }

    @Test
    @DisplayName("Strukturen: Station (4 Biome), Dreadnought (Space/Void) und Alien (Space/Nebula) als Jigsaw")
    void structuresAreJigsawWithExpectedBiomesAndHeights() {
        doReturn(Holder.Reference.createStandAlone(poolOwner, ModTemplatePools.SPACE_STATION_START))
                .when(poolGetter).getOrThrow(ModTemplatePools.SPACE_STATION_START);
        doReturn(Holder.Reference.createStandAlone(poolOwner, ModTemplatePools.DREADNOUGHT_WRECK_START))
                .when(poolGetter).getOrThrow(ModTemplatePools.DREADNOUGHT_WRECK_START);
        doReturn(Holder.Reference.createStandAlone(poolOwner, ModTemplatePools.ALIEN_OUTPOST_START))
                .when(poolGetter).getOrThrow(ModTemplatePools.ALIEN_OUTPOST_START);

        when(structureContext.lookup(Registries.TEMPLATE_POOL)).thenReturn(poolGetter);
        when(structureContext.lookup(Registries.BIOME)).thenReturn(biomeGetter);
        // Alle 8 referenzierten Biome (Strict Stubs)
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModDimensions.SPACE_BIOME))
                .when(biomeGetter).getOrThrow(ModDimensions.SPACE_BIOME);
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModBiomes.PLASMA_NEBULA))
                .when(biomeGetter).getOrThrow(ModBiomes.PLASMA_NEBULA);
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModBiomes.VOID_WASTES))
                .when(biomeGetter).getOrThrow(ModBiomes.VOID_WASTES);
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModBiomes.FROZEN_EXPANSE))
                .when(biomeGetter).getOrThrow(ModBiomes.FROZEN_EXPANSE);
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModBiomes.STELLAR_CORONA))
                .when(biomeGetter).getOrThrow(ModBiomes.STELLAR_CORONA);
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModBiomes.GRAVITY_RIFT))
                .when(biomeGetter).getOrThrow(ModBiomes.GRAVITY_RIFT);

        ModStructures.bootstrapStructure(structureContext);

        // Station
        ArgumentCaptor<Structure> stationCaptor = ArgumentCaptor.forClass(Structure.class);
        verify(structureContext).register(eq(ModStructures.SPACE_STATION), stationCaptor.capture());
        Structure station = stationCaptor.getValue();
        assertInstanceOf(JigsawStructure.class, station);
        assertSame(GenerationStep.Decoration.SURFACE_STRUCTURES, station.step());
        assertSame(TerrainAdjustment.NONE, station.terrainAdaptation());
        Set<ResourceKey<Biome>> stationBiomes = station.biomes().stream()
                .map(h -> h.unwrapKey().orElseThrow())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of(ModDimensions.SPACE_BIOME, ModBiomes.PLASMA_NEBULA,
                ModBiomes.FROZEN_EXPANSE, ModBiomes.VOID_WASTES), stationBiomes);

        // Dreadnought Wreck
        ArgumentCaptor<Structure> dreadnoughtCaptor = ArgumentCaptor.forClass(Structure.class);
        verify(structureContext).register(eq(ModStructures.DREADNOUGHT_WRECK), dreadnoughtCaptor.capture());
        Structure dreadnought = dreadnoughtCaptor.getValue();
        assertInstanceOf(JigsawStructure.class, dreadnought);
        assertSame(GenerationStep.Decoration.SURFACE_STRUCTURES, dreadnought.step());
        assertSame(TerrainAdjustment.NONE, dreadnought.terrainAdaptation());
        Set<ResourceKey<Biome>> dreadnoughtBiomes = dreadnought.biomes().stream()
                .map(h -> h.unwrapKey().orElseThrow())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of(ModDimensions.SPACE_BIOME, ModBiomes.VOID_WASTES), dreadnoughtBiomes);

        // Alien Outpost
        ArgumentCaptor<Structure> alienCaptor = ArgumentCaptor.forClass(Structure.class);
        verify(structureContext).register(eq(ModStructures.ALIEN_OUTPOST), alienCaptor.capture());
        Structure alien = alienCaptor.getValue();
        assertInstanceOf(JigsawStructure.class, alien);
        assertSame(GenerationStep.Decoration.SURFACE_STRUCTURES, alien.step());
        assertSame(TerrainAdjustment.NONE, alien.terrainAdaptation());
        Set<ResourceKey<Biome>> alienBiomes = alien.biomes().stream()
                .map(h -> h.unwrapKey().orElseThrow())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of(ModDimensions.SPACE_BIOME, ModBiomes.PLASMA_NEBULA), alienBiomes);
    }

    @Test
    @DisplayName("Structure-Sets: Station (36/12), Dreadnought (48/16) und Alien (40/14) LINEAR")
    void structureSetsUseRandomSpreadPlacements() {
        doReturn(Holder.Reference.createStandAlone(new HolderOwner<Structure>() {}, ModStructures.SPACE_STATION))
                .when(structureGetter).getOrThrow(ModStructures.SPACE_STATION);
        doReturn(Holder.Reference.createStandAlone(new HolderOwner<Structure>() {}, ModStructures.DREADNOUGHT_WRECK))
                .when(structureGetter).getOrThrow(ModStructures.DREADNOUGHT_WRECK);
        doReturn(Holder.Reference.createStandAlone(new HolderOwner<Structure>() {}, ModStructures.ALIEN_OUTPOST))
                .when(structureGetter).getOrThrow(ModStructures.ALIEN_OUTPOST);

        when(structureSetContext.lookup(Registries.STRUCTURE)).thenReturn(structureGetter);

        ModStructures.bootstrapStructureSet(structureSetContext);

        // Station Set (36, 12)
        ArgumentCaptor<StructureSet> stationCaptor = ArgumentCaptor.forClass(StructureSet.class);
        verify(structureSetContext).register(eq(ModStructures.SPACE_STATION_SET), stationCaptor.capture());
        RandomSpreadStructurePlacement stationPlacement = assertType(RandomSpreadStructurePlacement.class, stationCaptor.getValue().placement());
        assertEquals(36, stationPlacement.spacing());
        assertEquals(12, stationPlacement.separation());
        assertSame(RandomSpreadType.LINEAR, stationPlacement.spreadType());

        // Dreadnought Set (48, 16)
        ArgumentCaptor<StructureSet> dreadnoughtCaptor = ArgumentCaptor.forClass(StructureSet.class);
        verify(structureSetContext).register(eq(ModStructures.DREADNOUGHT_WRECK_SET), dreadnoughtCaptor.capture());
        RandomSpreadStructurePlacement dreadnoughtPlacement = assertType(RandomSpreadStructurePlacement.class, dreadnoughtCaptor.getValue().placement());
        assertEquals(48, dreadnoughtPlacement.spacing());
        assertEquals(16, dreadnoughtPlacement.separation());
        assertSame(RandomSpreadType.LINEAR, dreadnoughtPlacement.spreadType());

        // Alien Outpost Set (40, 14)
        ArgumentCaptor<StructureSet> alienCaptor = ArgumentCaptor.forClass(StructureSet.class);
        verify(structureSetContext).register(eq(ModStructures.ALIEN_OUTPOST_SET), alienCaptor.capture());
        RandomSpreadStructurePlacement alienPlacement = assertType(RandomSpreadStructurePlacement.class, alienCaptor.getValue().placement());
        assertEquals(40, alienPlacement.spacing());
        assertEquals(14, alienPlacement.separation());
        assertSame(RandomSpreadType.LINEAR, alienPlacement.spreadType());
    }

    @Test
    @DisplayName("NBT-Templates: Alle 8 Struktur-Module existieren im Classpath")
    void allStructureNbtTemplatesExist() {
        String[] templates = {
                "/data/lit_spaceships/structure/space_station/docking_hub.nbt",
                "/data/lit_spaceships/structure/space_station/solar_wing.nbt",
                "/data/lit_spaceships/structure/space_station/laboratory.nbt",
                "/data/lit_spaceships/structure/space_station/reactor_room.nbt",
                "/data/lit_spaceships/structure/dreadnought_wreck/command_bridge.nbt",
                "/data/lit_spaceships/structure/dreadnought_wreck/corridor_breached.nbt",
                "/data/lit_spaceships/structure/dreadnought_wreck/engineering_core.nbt",
                "/data/lit_spaceships/structure/alien_outpost/monolith.nbt"
        };
        for (String path : templates) {
            assertNotNull(getClass().getResourceAsStream(path), path + " fehlt im Classpath");
        }
    }

    @Test
    @DisplayName("Kisten-Loot: ModChestLootTableProvider generiert alle Weltraum-Loot-Tables (inkl. Cosmic Vault)")
    void chestLootTablesGenerateExpectedEntries() {
        java.util.Map<ResourceKey<net.minecraft.world.level.storage.loot.LootTable>, net.minecraft.world.level.storage.loot.LootTable.Builder> tables =
                new java.util.HashMap<>();
        new com.lit.spaceships.datagen.provider.ModChestLootTableProvider(
                org.mockito.Mockito.mock(HolderLookup.Provider.class)).generate(tables::put);

        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.SPACE_STATION_CORE));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.DREADNOUGHT_ARMORY));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.ALIEN_MONOLITH));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.COSMIC_VAULT));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.PIRATE_CACHE));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.LEVIATHAN_HOARD));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.MONOLITH_SECRET));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.GATE_CACHE));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.SATELLITE_DEBRIS));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.JELLY_HEART));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.BATTLEFIELD_SALVAGE));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.CARGO_POD));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.BEHEMOTH_MANIFEST));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.REACTOR_CORE_SALVAGE));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.RELAY_INTERCEPT));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.SOLAR_HARVEST));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.DEEP_OUTPOST_ARCHIVE));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.COLONY_LARDER));

        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.SPACE_STATION_CORE).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.DREADNOUGHT_ARMORY).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.ALIEN_MONOLITH).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.COSMIC_VAULT).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.PIRATE_CACHE).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.LEVIATHAN_HOARD).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.MONOLITH_SECRET).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.GATE_CACHE).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.SATELLITE_DEBRIS).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.JELLY_HEART).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.BATTLEFIELD_SALVAGE).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.CARGO_POD).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.BEHEMOTH_MANIFEST).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.REACTOR_CORE_SALVAGE).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.RELAY_INTERCEPT).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.SOLAR_HARVEST).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.DEEP_OUTPOST_ARCHIVE).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.COLONY_LARDER).build());
    }

    @Test
    @DisplayName("7-Biome-Partition: extreme Zonen stechen heraus, Basis-Zonen bleiben intakt")
    void sevenBiomePartitionRoutesExtremeZones() {
        Climate.ParameterList<ResourceKey<Biome>> dist = ModDimensions.spaceBiomeDistribution();

        // Gravity Rift: C extrem niedrig + W extrem hoch gewinnt ueber ALLE Basis-Biome
        assertEquals(ModBiomes.GRAVITY_RIFT, dist.findValue(
                point(-0.5F, 0.5F, -0.95F, 0.0F, 0.0F, 0.85F)));
        // Stellar Corona: C hoch + T extrem heiss
        assertEquals(ModBiomes.STELLAR_CORONA, dist.findValue(
                point(0.9F, 0.5F, 0.8F, 0.0F, 0.0F, 0.0F)));
        // Ion Storm: W extrem tief + H hoch
        assertEquals(ModBiomes.ION_STORM, dist.findValue(
                point(0.0F, 0.7F, 0.0F, 0.0F, 0.0F, -0.8F)));

        // Basis-Partition bleibt intakt bei neutralen C/W:
        assertEquals(ModBiomes.FROZEN_EXPANSE, dist.findValue(point(-0.8F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)));
        assertEquals(ModBiomes.VOID_WASTES, dist.findValue(point(0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F)));
        assertEquals(ModDimensions.SPACE_BIOME, dist.findValue(point(0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F)));
        assertEquals(ModBiomes.PLASMA_NEBULA, dist.findValue(point(0.9F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F)));

        // Gapless-Abdeckung: alle 7 Biome werden von einem 4D-Raster erreicht
        java.util.Set<ResourceKey<Biome>> reachable = new java.util.HashSet<>();
        for (float t = -1.0F; t <= 1.0F; t += 0.25F) {
            for (float h = -1.0F; h <= 1.0F; h += 0.25F) {
                for (float c = -1.0F; c <= 1.0F; c += 0.25F) {
                    for (float w = -1.0F; w <= 1.0F; w += 0.25F) {
                        reachable.add(dist.findValue(point(t, h, c, 0.0F, 0.0F, w)));
                    }
                }
            }
        }
        assertEquals(java.util.Set.of(ModBiomes.GRAVITY_RIFT, ModBiomes.STELLAR_CORONA,
                ModBiomes.ION_STORM, ModBiomes.FROZEN_EXPANSE, ModBiomes.VOID_WASTES,
                ModDimensions.SPACE_BIOME, ModBiomes.PLASMA_NEBULA), reachable, "Alle 7 Biome muessen erreichbar sein");
    }

    private static Climate.TargetPoint point(float t, float h, float c, float e, float d, float w) {
        // Climate-Parameter werden mit 10000 skaliert (vanilla Climate.Parameter.point)
        return new Climate.TargetPoint((long) (t * 10000F), (long) (h * 10000F), (long) (c * 10000F),
                (long) (e * 10000F), (long) (d * 10000F), (long) (w * 10000F));
    }

    @Test
    @DisplayName("Extreme-Zellen-Specs: deterministisch und in Zellgrenzen geklemmt")
    void extremeCellSpecsAreDeterministicAndBounded() {
        // Gravity Rift
        var rift = com.lit.spaceships.world.feature.GravityRiftFeature.specForCell(3, -4);
        assertEquals(rift, com.lit.spaceships.world.feature.GravityRiftFeature.specForCell(3, -4));
        assertTrue(rift.riftY() >= 40 && rift.riftY() <= 200);
        double riftMargin = com.lit.spaceships.world.feature.GravityRiftFeature.R_DUST_OUT + 16.0D;
        assertTrue(rift.centerX() >= 3 * 2048.0D + riftMargin
                && rift.centerX() <= 4 * 2048.0D - riftMargin);

        // Stellar Corona: 3 Flares, Y 96..288, sicher in der Zelle
        var flares = com.lit.spaceships.world.feature.StellarCoronaFeature.specsForCell(1, 2);
        assertEquals(3, flares.length);
        assertEquals(flares[0], com.lit.spaceships.world.feature.StellarCoronaFeature.specsForCell(1, 2)[0]);
        for (var flare : flares) {
            assertTrue(flare.flareY() >= 96 && flare.flareY() <= 288);
            assertTrue(flare.flareX() >= 1 * 1024 + 64 && flare.flareX() <= 2 * 1024 - 64);
        }

        // Ion Storm: 4 Pylone, Y 64..256
        var pylons = com.lit.spaceships.world.feature.IonStormFeature.specsForCell(-2, 5);
        assertEquals(4, pylons.length);
        assertEquals(pylons[3], com.lit.spaceships.world.feature.IonStormFeature.specsForCell(-2, 5)[3]);
        for (var pylon : pylons) {
            assertTrue(pylon.groundY() >= 64 && pylon.groundY() <= 256);
            assertTrue(pylon.height() >= 3 && pylon.height() <= 5);
        }
    }

    private static <T> T privateField(Object owner, String name, Class<T> type) {
        try {
            var field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(owner));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Feld nicht lesbar: " + name, e);
        }
    }

    private static int absoluteAnchorY(VerticalAnchor anchor) {
        assertSame(VerticalAnchor.Absolute.class, anchor.getClass());
        return ((VerticalAnchor.Absolute) anchor).y();
    }

    private static void assertInstanceOf(Class<?> expected, Object actual) {
        assertTrue(expected.isInstance(actual), "Erwartet " + expected.getSimpleName() + ", war " + actual.getClass().getSimpleName());
    }

    private static <T> T assertType(Class<T> expected, Object actual) {
        assertInstanceOf(expected, actual);
        return expected.cast(actual);
    }
}
