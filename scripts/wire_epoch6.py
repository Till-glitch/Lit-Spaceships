# -*- coding: utf-8 -*-
"""Epoch 6 wiring: biome feature chains + cargo pod loot."""

mb = 'src/main/java/com/lit/spaceships/world/ModBiomes.java'
s = open(mb, encoding='utf-8').read()

# space biome (separate method, ends BATTLEFIELD)
old = ('''                .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                        placedFeatures.getOrThrow(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED))
                .build();''')
new = ('''                .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                        placedFeatures.getOrThrow(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED))
                .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                        placedFeatures.getOrThrow(ModAmbientFeatures.DEBRIS_FIELD_PLACED))
                .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                        placedFeatures.getOrThrow(ModAmbientFeatures.METEOR_SHOWER_PLACED))
                .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                        placedFeatures.getOrThrow(ModAmbientFeatures.VOID_CRYSTAL_SPIKE_PLACED))
                .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                        placedFeatures.getOrThrow(ModAmbientFeatures.BEACON_PYLON_PLACED))
                .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                        placedFeatures.getOrThrow(ModAmbientFeatures.CARGO_POD_PLACED))
                .build();''')
assert old in s, 'space chain anchor'
s = s.replace(old, new)

# nebula (ends JELLYFISH)
old = ('''                .generationSettings(new BiomeGenerationSettings.PlainBuilder()
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED))
                        .build())''')
new = ('''                .generationSettings(new BiomeGenerationSettings.PlainBuilder()
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.NEBULA_SPORE_DRIFT_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.PLASMA_EMBER_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.NEBULA_GAS_BLOOM_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.CRYSTAL_LATTICE_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.NEBULA_ARC_PLACED))
                        .build())''')
assert old in s, 'nebula chain anchor'
s = s.replace(old, new)

# frozen (ends BELT)
old = ('''                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModPlacedFeatures.ASTEROID_BELT_PLACED))
                        .build())
                .build();''')
new = ('''                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModPlacedFeatures.ASTEROID_BELT_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.ICE_SHARD_FIELD_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.GLACIER_FLOE_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.FROST_PILLAR_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.SNOW_BLOOM_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.CRYO_GEODE_PLACED))
                        .build())
                .build();''')
assert old in s, 'frozen chain anchor'
s = s.replace(old, new)

# void (ends BATTLEFIELD)
old = ('''                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED))
                        .build())
                .build();''')
new = ('''                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.BONE_DEBRIS_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.SCRAP_WASTELAND_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.DUST_DRIFT_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.ASH_VENT_PLACED))
                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,
                                placedFeatures.getOrThrow(ModAmbientFeatures.VOID_CYST_PLACED))
                        .build())
                .build();''')
assert old in s, 'void chain anchor'
s = s.replace(old, new)
open(mb, 'w', encoding='utf-8').write(s)
print('biomes wired')

lo = 'src/main/java/com/lit/spaceships/datagen/provider/ModChestLootTableProvider.java'
s = open(lo, encoding='utf-8').read()
if 'CARGO_POD' not in s:
    import_line = 'import com.lit.spaceships.world.feature.PodFeature;'
    base = 'import com.lit.spaceships.LitSpaceships;'
    s = s.replace(base, base + '\n' + import_line)
    s = s.replace('''    public static final ResourceKey<LootTable> BATTLEFIELD_SALVAGE = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/battlefield_salvage"));''',
'''    public static final ResourceKey<LootTable> BATTLEFIELD_SALVAGE = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/battlefield_salvage"));

    public static final ResourceKey<LootTable> CARGO_POD = PodFeature.CARGO_POD_LOOT;''')
    tail_old = '''                        .add(LootItem.lootTableItem(Items.OBSIDIAN).setWeight(5)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))));
    }
}'''
    tail_new = '''                        .add(LootItem.lootTableItem(Items.OBSIDIAN).setWeight(5)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))));

        // Cargo Pod: verlorene Fracht - lebenswichtige Vorrraete
        consumer.accept(CARGO_POD, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 4.0F))
                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.COPPER_INGOT).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
                        .add(LootItem.lootTableItem(Items.REDSTONE).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.COAL).setWeight(6)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 7.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0.0F, 1.0F))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(2)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(1))
                        .add(LootItem.lootTableItem(Items.MUSIC_DISC_OTHERSIDE).setWeight(1))));
    }
}'''
    assert tail_old in s, 'loot tail anchor'
    s = s.replace(tail_old, tail_new)
    open(lo, 'w', encoding='utf-8').write(s)
print('loot wired')
