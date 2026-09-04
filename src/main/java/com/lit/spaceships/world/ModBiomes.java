package com.lit.spaceships.world;

import com.lit.spaceships.LitSpaceships;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.joml.Vector3f;

/**
 * Bootstrap für alle Biome des {@link ModDimensions#SPACE_LEVEL Weltraum-Dimension}.
 * Ersetzt manuelle JSON-Dateien unter {@code worldgen/biome/}.
 *
 * <p>Der Weltraum ist eine sensorische Leere: schwarzer Himmel und Nebel, keine
 * natürlichen Mob-Spawns, keine Höhlenklänge. Alle dekorativen Effekte (Nebel- und
 * Himmelsfarben, Partikel) werden strikt über die Biome-Daten gesteuert
 * (server-authoritativ).</p>
 */
public final class ModBiomes {

    /**
     * Grund-Biome: absolute Dunkelheit und Leere zwischen den Nebelzonen.
     */
    public static final ResourceKey<Biome> SPACE_BIOME = ModDimensions.SPACE_BIOME;

    /**
     * Plasma-Nebel: violett leuchtende kosmische Nebelzonen mit schwebenden
     * Plasmaglanzpartikeln und fehlender Sichtweite (Nebel #7F00FF).
     */
    public static final ResourceKey<Biome> PLASMA_NEBULA = createKey("plasma_nebula");

    /**
     * Frozen Expanse: eisige Weltraumzone mit bleich-cyanfarbener Atmosphäre
     * (#00FFFF) und hochdichten Eiskometen-Feldern.
     */
    public static final ResourceKey<Biome> FROZEN_EXPANSE = createKey("frozen_expanse");

    private ModBiomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        context.register(SPACE_BIOME, spaceBiome(placedFeatures));
        context.register(PLASMA_NEBULA, plasmaNebula());
        context.register(FROZEN_EXPANSE, frozenExpanse(placedFeatures));
    }

    static void bootstrapWith(BootstrapContext<Biome> context, HolderGetter<PlacedFeature> placedFeatures) {
        context.register(SPACE_BIOME, spaceBiome(placedFeatures));
        context.register(PLASMA_NEBULA, plasmaNebula());
        context.register(FROZEN_EXPANSE, frozenExpanse(placedFeatures));
    }

    /**
     * Der Grund-Biome der Dimension: absolute Dunkelheit (Farbe 0) und dunkelblaues
     * Wasser-Nebellicht, ohne Spawns, ohne Höhlenklänge, mit Asteroiden und Wracks.
     */
    static Biome spaceBiome(HolderGetter<PlacedFeature> placedFeatures) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.0F)
                .downfall(0.0F)
                .temperatureAdjustment(Biome.TemperatureModifier.NONE)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .fogColor(0)
                        .skyColor(0)
                        .waterColor(328981)
                        .waterFogColor(328981)
                        .grassColorOverride(0)
                        .foliageColorOverride(0)
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(spaceGenerationSettings(placedFeatures))
                .build();
    }

    /**
     * Plasma-Nebel: dichter violettfarbener Nebel (#7F00FF) über dunkelviolettem
     * Himmel, schwebende glühende Plasmastaub-Partikel, keine Mob-Spawns und
     * bewusst keine Features — der Nebel ist eine reine Atmosphären-/Gefahrenzone.
     */
    static Biome plasmaNebula() {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5F)
                .downfall(0.0F)
                .temperatureAdjustment(Biome.TemperatureModifier.NONE)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .fogColor(0x7F00FF)
                        .skyColor(0x1A0033)
                        .waterColor(0x1A0033)
                        .waterFogColor(0x1A0033)
                        .ambientParticle(new AmbientParticleSettings(
                                new DustParticleOptions(new Vector3f(0.498F, 0.0F, 1.0F), 0.8F), 0.006F))
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }

    /**
     * Frozen Expanse: bleich-cyanfarbene Atmosphäre (#00FFFF) über dunklem
     * Cyan-Himmel, fallende Schneeflocken-Partikelströme, keine Mob-Spawns.
     * Generation: hochdichte Eiskometen-Felder (blaue Eis-Kerne, Packeis-Mäntel).
     */
    static Biome frozenExpanse(HolderGetter<PlacedFeature> placedFeatures) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(-0.5F)
                .downfall(0.0F)
                .temperatureAdjustment(Biome.TemperatureModifier.NONE)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .fogColor(0x00FFFF)
                        .skyColor(0x003344)
                        .waterColor(0x003344)
                        .waterFogColor(0x003344)
                        .ambientParticle(new AmbientParticleSettings(ParticleTypes.SNOWFLAKE, 0.015F))
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(new BiomeGenerationSettings.PlainBuilder()
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModPlacedFeatures.ICE_COMET_PLACED))
                        .build())
                .build();
    }

    private static BiomeGenerationSettings spaceGenerationSettings(HolderGetter<PlacedFeature> placedFeatures) {
        return new BiomeGenerationSettings.PlainBuilder()
                .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                        placedFeatures.getOrThrow(ModPlacedFeatures.ASTEROID_PLACED))
                .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                        placedFeatures.getOrThrow(ModPlacedFeatures.SPACE_WRECK_PLACED))
                .build();
    }

    private static ResourceKey<Biome> createKey(String name) {
        return ResourceKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, name));
    }
}
