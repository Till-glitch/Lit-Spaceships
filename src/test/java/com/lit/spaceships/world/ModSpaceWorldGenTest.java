package com.lit.spaceships.world;

import com.lit.spaceships.world.feature.AsteroidFeature;
import com.lit.spaceships.world.feature.SpaceWreckFeature;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
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
import net.minecraft.world.level.levelgen.GenerationStep;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
    @DisplayName("ConfiguredFeature-Bootstrap registriert Asteroid und Wrack mit NoneFeatureConfiguration")
    void configuredFeatureBootstrapRegistersBothFeatures() {
        AsteroidFeature asteroid = new AsteroidFeature(NoneFeatureConfiguration.CODEC);
        SpaceWreckFeature wreck = new SpaceWreckFeature(NoneFeatureConfiguration.CODEC);

        ModConfiguredFeatures.bootstrapWith(configuredContext, asteroid, wreck);

        ArgumentCaptor<ConfiguredFeature<?, ?>> captor = ArgumentCaptor.forClass(ConfiguredFeature.class);
        verify(configuredContext).register(eq(ModConfiguredFeatures.ASTEROID), captor.capture());
        verify(configuredContext).register(eq(ModConfiguredFeatures.SPACE_WRECK), any());

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
}
