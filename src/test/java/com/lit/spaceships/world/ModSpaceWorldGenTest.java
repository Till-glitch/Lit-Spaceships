package com.lit.spaceships.world;

import com.lit.spaceships.world.feature.AsteroidFeature;
import com.lit.spaceships.world.feature.IceCometFeature;
import com.lit.spaceships.world.feature.SpaceWreckFeature;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.MobCategory;
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
        assertKey(ModPlacedFeatures.ASTEROID_PLACED, Registries.PLACED_FEATURE, "asteroid_placed");
        assertKey(ModPlacedFeatures.SPACE_WRECK_PLACED, Registries.PLACED_FEATURE, "space_wreck_placed");
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

        ModConfiguredFeatures.bootstrapWith(configuredContext, asteroid, wreck, iceComet);

        ArgumentCaptor<ConfiguredFeature<?, ?>> captor = ArgumentCaptor.forClass(ConfiguredFeature.class);
        verify(configuredContext).register(eq(ModConfiguredFeatures.ASTEROID), captor.capture());
        verify(configuredContext).register(eq(ModConfiguredFeatures.SPACE_WRECK), any());
        verify(configuredContext).register(eq(ModConfiguredFeatures.ICE_COMET), any());

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
    @DisplayName("Plasma-Nebel: violetter Nebel #7F00FF, glühende Partikel, keine Spawns, keine Features")
    void plasmaNebulaIsGlowingVioletZone() {
        when(biomeContext.lookup(Registries.PLACED_FEATURE)).thenReturn(placedGetter);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ASTEROID_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ASTEROID_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.SPACE_WRECK_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.SPACE_WRECK_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ICE_COMET_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ICE_COMET_PLACED);

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
    @DisplayName("Multi-Noise-Verteilung routet Temperatur 3-wege: kalt=Frozen, mittel=Void, heiß=Nebel")
    void spaceBiomeDistributionRoutesTemperature() {
        Climate.ParameterList<ResourceKey<Biome>> distribution = ModDimensions.spaceBiomeDistribution();

        assertSame(ModBiomes.FROZEN_EXPANSE,
                distribution.findValue(new Climate.TargetPoint(-8000L, 0L, 0L, 0L, 0L, 0L)));
        assertSame(ModDimensions.SPACE_BIOME,
                distribution.findValue(new Climate.TargetPoint(0L, 0L, 0L, 0L, 0L, 0L)));
        assertSame(ModBiomes.PLASMA_NEBULA,
                distribution.findValue(new Climate.TargetPoint(8000L, 0L, 0L, 0L, 0L, 0L)));
        assertSame(ModDimensions.SPACE_BIOME,
                distribution.findValue(new Climate.TargetPoint(1000L, 0L, 0L, 0L, 0L, 0L)));
        assertSame(ModBiomes.FROZEN_EXPANSE,
                distribution.findValue(new Climate.TargetPoint(-5000L, 0L, 0L, 0L, 0L, 0L)));
    }

    @Test
    @DisplayName("Space-Noise-Settings: reiner Void mit negativer Dichte und Temperatur-Noise für Biome")
    void spaceNoiseSettingsAreVoidWithTemperatureNoise() {
        Holder<NormalNoise.NoiseParameters> temperatureNoise =
                Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0));

        NoiseGeneratorSettings settings = ModNoiseSettings.spaceNoiseSettings(temperatureNoise);

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

        Holder<NoiseGeneratorSettings> settingsHolder =
                Holder.direct(ModNoiseSettings.spaceNoiseSettings(Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0))));
        Holder<DimensionType> typeHolder = Holder.direct(ModDimensions.spaceDimensionType());

        LevelStem stem = ModDimensions.spaceLevelStem(biomeGetter, settingsHolder, typeHolder);

        assertSame(typeHolder, stem.type());
        NoiseBasedChunkGenerator generator = assertType(NoiseBasedChunkGenerator.class, stem.generator());
        assertSame(settingsHolder, generator.generatorSettings());

        Set<ResourceKey<Biome>> possibleBiomes = generator.getBiomeSource().possibleBiomes().stream()
                .map(holder -> holder.unwrapKey().orElseThrow())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of(ModDimensions.SPACE_BIOME, ModBiomes.PLASMA_NEBULA, ModBiomes.FROZEN_EXPANSE), possibleBiomes);
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

        // Hochdichte Eiskometen-Felder als einziges Feature in Deko-Stufe 0
        List<HolderSet<PlacedFeature>> featureSteps = frozen.getGenerationSettings().features();
        assertEquals(1, featureSteps.size());
        List<Holder<PlacedFeature>> stepZero = featureSteps.get(0).stream().toList();
        assertEquals(1, stepZero.size());
        assertTrue(stepZero.stream().anyMatch(h -> h.unwrapKey().orElseThrow().equals(ModPlacedFeatures.ICE_COMET_PLACED)));
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
