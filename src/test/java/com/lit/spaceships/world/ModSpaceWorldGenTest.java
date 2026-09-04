package com.lit.spaceships.world;

import com.lit.spaceships.world.feature.AsteroidBeltFeature;
import com.lit.spaceships.world.feature.AsteroidFeature;
import com.lit.spaceships.world.feature.IceCometFeature;
import com.lit.spaceships.world.feature.MegaAsteroidFeature;
import com.lit.spaceships.world.feature.PlanetaryRingFeature;
import com.lit.spaceships.world.feature.SpaceWreckFeature;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
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

        ModConfiguredFeatures.bootstrapWith(configuredContext, asteroid, wreck, iceComet, megaAsteroid, planetaryRing, asteroidBelt);

        ArgumentCaptor<ConfiguredFeature<?, ?>> captor = ArgumentCaptor.forClass(ConfiguredFeature.class);
        verify(configuredContext).register(eq(ModConfiguredFeatures.ASTEROID), captor.capture());
        verify(configuredContext).register(eq(ModConfiguredFeatures.SPACE_WRECK), any());
        verify(configuredContext).register(eq(ModConfiguredFeatures.ICE_COMET), any());
        verify(configuredContext).register(eq(ModConfiguredFeatures.MEGA_ASTEROID), any());
        verify(configuredContext).register(eq(ModConfiguredFeatures.PLANETARY_RING), any());
        verify(configuredContext).register(eq(ModConfiguredFeatures.ASTEROID_BELT), any());

        ConfiguredFeature<?, ?> registered = captor.getValue();
        assertSame(asteroid, registered.feature());
        assertSame(NoneFeatureConfiguration.INSTANCE, registered.config());
    }

    @Test
    @DisplayName("Asteroiden-Platzierung: Count 4, InSquare, Uniformhöhe -40..280, Biome-Filter")
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
        assertEquals(4, provider.getMinValue());
        assertEquals(4, provider.getMaxValue());

        HeightRangePlacement height = (HeightRangePlacement) modifiers.get(2);
        UniformHeight uniform = privateField(height, "height", UniformHeight.class);
        assertEquals(-40, absoluteAnchorY(privateField(uniform, "minInclusive", VerticalAnchor.class)));
        assertEquals(280, absoluteAnchorY(privateField(uniform, "maxInclusive", VerticalAnchor.class)));
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
                ModPlacedFeatures.ASTEROID_BELT_PLACED), stepZeroKeys);
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
    @DisplayName("Wrack-Feld-Platzierung: 8x dichtere Rarity 1/4, InSquare, Uniformhöhe 0..200, Biome-Filter")
    void wreckFieldPlacementMathIsBounded() {
        List<PlacementModifier> modifiers = ModPlacedFeatures.wreckFieldPlacement();

        assertEquals(4, modifiers.size());
        assertInstanceOf(RarityFilter.class, modifiers.get(0));
        assertSame(InSquarePlacement.spread(), modifiers.get(1));
        assertInstanceOf(HeightRangePlacement.class, modifiers.get(2));
        assertSame(BiomeFilter.biome(), modifiers.get(3));

        RarityFilter rarity = (RarityFilter) modifiers.get(0);
        assertEquals(4, (int) privateField(rarity, "chance", Integer.class));

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
        DustParticleOptions dust = assertType(DustParticleOptions.class, particle.get().getOptions());
        assertEquals(0.498F, dust.getColor().x(), 0.0001F);
        assertEquals(0.0F, dust.getColor().y(), 0.0001F);
        assertEquals(1.0F, dust.getColor().z(), 0.0001F);
        assertEquals(0.006F, (float) privateField(particle.get(), "probability", Float.class), 0.0F);

        for (MobCategory category : MobCategory.values()) {
            assertTrue(nebula.getMobSettings().getMobs(category).isEmpty(), category.getName());
        }
        assertTrue(nebula.getGenerationSettings().features().isEmpty());
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

        NoiseGeneratorSettings settings = ModNoiseSettings.spaceNoiseSettings(temperatureNoise, vegetationNoise);

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
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModDimensions.SPACE_BIOME))
                .when(biomeGetter).getOrThrow(ModDimensions.SPACE_BIOME);
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModBiomes.PLASMA_NEBULA))
                .when(biomeGetter).getOrThrow(ModBiomes.PLASMA_NEBULA);
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModBiomes.FROZEN_EXPANSE))
                .when(biomeGetter).getOrThrow(ModBiomes.FROZEN_EXPANSE);
        doReturn(Holder.Reference.createStandAlone(biomeOwner, ModBiomes.VOID_WASTES))
                .when(biomeGetter).getOrThrow(ModBiomes.VOID_WASTES);

        Holder<NoiseGeneratorSettings> settingsHolder =
                Holder.direct(ModNoiseSettings.spaceNoiseSettings(
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
                ModBiomes.VOID_WASTES), possibleBiomes);
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
                ModPlacedFeatures.PLANETARY_RING_PLACED, ModPlacedFeatures.ASTEROID_BELT_PLACED), stepZeroKeys);
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
    void asteroidBeltSpecAndCorridorMath() {
        AsteroidBeltFeature.BeltSpec spec = AsteroidBeltFeature.specForCell(-2, 5);
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
