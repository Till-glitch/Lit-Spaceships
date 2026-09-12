# -*- coding: utf-8 -*-
"""Epoch 8: adds gravity_rift, stellar_corona, ion_storm biomes to ModBiomes."""

mb = 'src/main/java/com/lit/spaceships/world/ModBiomes.java'
s = open(mb, encoding='utf-8').read()

# imports
if 'import net.minecraft.sounds.SoundEvents;' not in s:
    s = s.replace('import net.minecraft.core.particles.DustParticleOptions;',
                  'import net.minecraft.core.particles.DustParticleOptions;\nimport net.minecraft.sounds.SoundEvents;')

# 1) keys
anchor1 = '''    public static final ResourceKey<Biome> VOID_WASTES = createKey("void_wastes");'''
add1 = anchor1 + '''

    /**
     * Gravity Rift: Akkretionssingularitaet im tiefsten Raum — C extrem niedrig,
     * W extrem hoch. Umgekehrte Portalpartikel, Basalt-Rumble mit Sub-Bass.
     */
    public static final ResourceKey<Biome> GRAVITY_RIFT = createKey("gravity_rift");

    /**
     * Stellar Corona: Sonnennahe Chromosphaere — C hoch, T extrem heiss.
     * Magmaflares, Lava-Kessel und gluehende Bogenstrukturen.
     */
    public static final ResourceKey<Biome> STELLAR_CORONA = createKey("stellar_corona");

    /**
     * Ion Storm: geladene Rift-Taeler — W extrem tief, H hoch.
     * EMP-Pylone, Kupferbirnen und elektrisierte Kettennetze.
     */
    public static final ResourceKey<Biome> ION_STORM = createKey("ion_storm");'''
assert anchor1 in s
s = s.replace(anchor1, add1, 1)

# 2) register + bootstrapWith
for anchor in (
    '''        context.register(VOID_WASTES, voidWastes(placedFeatures));
    }''',
    '''        context.register(VOID_WASTES, voidWastes(placedFeatures));
    }''',
):
    replacement = '''        context.register(VOID_WASTES, voidWastes(placedFeatures));
        context.register(GRAVITY_RIFT, gravityRift(placedFeatures));
        context.register(STELLAR_CORONA, stellarCorona(placedFeatures));
        context.register(ION_STORM, ionStorm(placedFeatures));
    }'''
    if anchor in s:
        s = s.replace(anchor, replacement, 1)
        break

# 3) three factories before createKey
anchor3 = '''    private static ResourceKey<Biome> createKey(String name) {'''
factories = '''    /**
     * Gravity Rift: nahezu schwarzer Raum mit tiefblauem Schimmer (#0B001A),
     * umgekehrten Portalpartikeln und Basalt-Rumble. Signature:
     * Akkretionsscheibe aus Crying Obsidian, Gilded Blackstone und dunklem Staub.
     */
    static Biome gravityRift(HolderGetter<PlacedFeature> placedFeatures) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.0F)
                .downfall(0.0F)
                .temperatureAdjustment(Biome.TemperatureModifier.NONE)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .fogColor(0x0B001A)
                        .skyColor(0x020005)
                        .waterColor(0x0B001A)
                        .waterFogColor(0x020005)
                        .ambientParticle(new AmbientParticleSettings(ParticleTypes.REVERSE_PORTAL, 0.012F))
                        .ambientLoopSound(SoundEvents.AMBIENT_BASALT_DELTAS_LOOP)
                        .ambientAdditionsSound(new AmbientAdditionsSettings(
                                SoundEvents.AMBIENT_BASALT_DELTAS_ADDITIONS, 0.004D))
                        .ambientMoodSound(new AmbientMoodSettings(
                                SoundEvents.AMBIENT_BASALT_DELTAS_MOOD, 4500, 8, 2.0D))
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(new BiomeGenerationSettings.PlainBuilder()
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.GRAVITY_RIFT_DISK_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.DEBRIS_FIELD_PLACED))
                        .build())
                .build();
    }

    /**
     * Stellar Corona: Orange-gluehende Chromosphaere (#FF4500) mit Flammen-
     * partikeln und Nether-Wind. Signature: Sonnenflares aus Magma, glattem
     * Basalt und Lava-Kesseln.
     */
    static Biome stellarCorona(HolderGetter<PlacedFeature> placedFeatures) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(1.2F)
                .downfall(0.0F)
                .temperatureAdjustment(Biome.TemperatureModifier.NONE)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .fogColor(0xFF4500)
                        .skyColor(0x330A00)
                        .waterColor(0xFF4500)
                        .waterFogColor(0x330A00)
                        .ambientParticle(new AmbientParticleSettings(ParticleTypes.FLAME, 0.015F))
                        .ambientLoopSound(SoundEvents.AMBIENT_NETHER_WASTES_LOOP)
                        .ambientAdditionsSound(new AmbientAdditionsSettings(
                                SoundEvents.AMBIENT_BASALT_DELTAS_ADDITIONS, 0.010D))
                        .ambientMoodSound(new AmbientMoodSettings(
                                SoundEvents.AMBIENT_BASALT_DELTAS_MOOD, 5000, 8, 2.0D))
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(new BiomeGenerationSettings.PlainBuilder()
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.STELLAR_FLARE_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.PLASMA_EMBER_PLACED))
                        .build())
                .build();
    }

    /**
     * Ion Storm: elektrisch geladenes Blau (#1E90FF) mit Funkenpartikeln und
     * Karmesin-Hum. Signature: EMP-Pylone, Kupferbirnen und Kettennetze.
     */
    static Biome ionStorm(HolderGetter<PlacedFeature> placedFeatures) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.3F)
                .downfall(0.0F)
                .temperatureAdjustment(Biome.TemperatureModifier.NONE)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .fogColor(0x1E90FF)
                        .skyColor(0x001133)
                        .waterColor(0x001133)
                        .waterFogColor(0x001133)
                        .ambientParticle(new AmbientParticleSettings(ParticleTypes.ELECTRIC_SPARK, 0.020F))
                        .ambientLoopSound(SoundEvents.AMBIENT_CRIMSON_FOREST_LOOP)
                        .ambientAdditionsSound(new AmbientAdditionsSettings(
                                SoundEvents.AMBIENT_WARPED_FOREST_ADDITIONS, 0.0111D))
                        .ambientMoodSound(new AmbientMoodSettings(
                                SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD, 6000, 8, 2.0D))
                        .build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(new BiomeGenerationSettings.PlainBuilder()
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.ION_PYLON_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.SCRAP_WASTELAND_PLACED))
                        .build())
                .build();
    }

    private static ResourceKey<Biome> createKey(String name) {'''
assert anchor3 in s
s = s.replace(anchor3, factories, 1)
open(mb, 'w', encoding='utf-8').write(s)
print('ModBiomes: 3 extreme biomes added')
