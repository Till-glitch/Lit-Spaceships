# -*- coding: utf-8 -*-
"""Wires AncientBattlefieldFeature into registries, placed features, biomes and loot."""
mf = 'src/main/java/com/lit/spaceships/registry/ModFeatures.java'
s = open(mf, encoding='utf-8').read()
if 'ANCIENT_BATTLEFIELD' not in s:
    s = s.replace('import com.lit.spaceships.world.feature.AsteroidBeltFeature;',
                  'import com.lit.spaceships.world.feature.AncientBattlefieldFeature;\nimport com.lit.spaceships.world.feature.AsteroidBeltFeature;')
    s = s.replace(
        '    public static final DeferredHolder<Feature<?>, CosmicJellyfishFeature> COSMIC_JELLYFISH =\n'
        '            FEATURES.register("cosmic_jellyfish", () -> new CosmicJellyfishFeature(NoneFeatureConfiguration.CODEC));',
        '    public static final DeferredHolder<Feature<?>, CosmicJellyfishFeature> COSMIC_JELLYFISH =\n'
        '            FEATURES.register("cosmic_jellyfish", () -> new CosmicJellyfishFeature(NoneFeatureConfiguration.CODEC));\n\n'
        '    public static final DeferredHolder<Feature<?>, AncientBattlefieldFeature> ANCIENT_BATTLEFIELD =\n'
        '            FEATURES.register("ancient_battlefield", () -> new AncientBattlefieldFeature(NoneFeatureConfiguration.CODEC));')
    open(mf, 'w', encoding='utf-8').write(s)
print('ModFeatures done')

cf = 'src/main/java/com/lit/spaceships/world/ModConfiguredFeatures.java'
s = open(cf, encoding='utf-8').read()
if 'ANCIENT_BATTLEFIELD' not in s:
    s = s.replace('    public static final ResourceKey<ConfiguredFeature<?, ?>> COSMIC_JELLYFISH = createKey("cosmic_jellyfish");',
                  '    public static final ResourceKey<ConfiguredFeature<?, ?>> COSMIC_JELLYFISH = createKey("cosmic_jellyfish");\n'
                  '    public static final ResourceKey<ConfiguredFeature<?, ?>> ANCIENT_BATTLEFIELD = createKey("ancient_battlefield");')
    s = s.replace('                ModFeatures.SATELLITE_GRAVEYARD.get(), ModFeatures.COSMIC_JELLYFISH.get());',
                  '                ModFeatures.SATELLITE_GRAVEYARD.get(), ModFeatures.COSMIC_JELLYFISH.get(),\n'
                  '                ModFeatures.ANCIENT_BATTLEFIELD.get());')
    s = s.replace('                              Feature<NoneFeatureConfiguration> cosmicJellyfish) {',
                  '                              Feature<NoneFeatureConfiguration> cosmicJellyfish,\n'
                  '                              Feature<NoneFeatureConfiguration> ancientBattlefield) {')
    s = s.replace('        context.register(COSMIC_JELLYFISH, new ConfiguredFeature<>(cosmicJellyfish, NoneFeatureConfiguration.INSTANCE));\n    }',
                  '        context.register(COSMIC_JELLYFISH, new ConfiguredFeature<>(cosmicJellyfish, NoneFeatureConfiguration.INSTANCE));\n'
                  '        context.register(ANCIENT_BATTLEFIELD, new ConfiguredFeature<>(ancientBattlefield, NoneFeatureConfiguration.INSTANCE));\n    }')
    open(cf, 'w', encoding='utf-8').write(s)
print('ModConfiguredFeatures done')

pf = 'src/main/java/com/lit/spaceships/world/ModPlacedFeatures.java'
s = open(pf, encoding='utf-8').read()
if 'ANCIENT_BATTLEFIELD_PLACED' not in s:
    s = s.replace('    public static final ResourceKey<PlacedFeature> COSMIC_JELLYFISH_PLACED = createKey("cosmic_jellyfish_placed");',
                  '    public static final ResourceKey<PlacedFeature> COSMIC_JELLYFISH_PLACED = createKey("cosmic_jellyfish_placed");\n'
                  '    public static final ResourceKey<PlacedFeature> ANCIENT_BATTLEFIELD_PLACED = createKey("ancient_battlefield_placed");')
    s = s.replace('        context.register(COSMIC_JELLYFISH_PLACED, new PlacedFeature(\n'
                  '                configuredFeatures.getOrThrow(ModConfiguredFeatures.COSMIC_JELLYFISH), cosmicJellyfishPlacement()));\n    }',
                  '        context.register(COSMIC_JELLYFISH_PLACED, new PlacedFeature(\n'
                  '                configuredFeatures.getOrThrow(ModConfiguredFeatures.COSMIC_JELLYFISH), cosmicJellyfishPlacement()));\n'
                  '        context.register(ANCIENT_BATTLEFIELD_PLACED, new PlacedFeature(\n'
                  '                configuredFeatures.getOrThrow(ModConfiguredFeatures.ANCIENT_BATTLEFIELD), ancientBattlefieldPlacement()));\n    }')
    s = s.replace('    /**\n     * Comet Fields der Frozen Expanse',
                  '    /**\n     * Ancient Battlefield: 2 Versuche pro Chunk (Zellen-Spec: 30% der Zellen).\n     */\n'
                  '    static List<PlacementModifier> ancientBattlefieldPlacement() {\n'
                  '        return List.of(\n'
                  '                CountPlacement.of(2),\n'
                  '                InSquarePlacement.spread(),\n'
                  '                BiomeFilter.biome()\n'
                  '        );\n'
                  '    }\n\n'
                  '    /**\n     * Comet Fields der Frozen Expanse')
    open(pf, 'w', encoding='utf-8').write(s)
print('ModPlacedFeatures done')

mb = 'src/main/java/com/lit/spaceships/world/ModBiomes.java'
s = open(mb, encoding='utf-8').read()
changed = False
if 'ANCIENT_BATTLEFIELD_PLACED' not in s:
    # void wastes chain (ends with SATELLITE_GRAVEYARD inline)
    old_void = ('                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,\n'
                '                                placedFeatures.getOrThrow(ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED))\n'
                '                        .build())\n'
                '                .build();')
    new_void = ('                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,\n'
                '                                placedFeatures.getOrThrow(ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED))\n'
                '                        .addFeature(GenerationStep.Decoration.RAW_GENERATION,\n'
                '                                placedFeatures.getOrThrow(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED))\n'
                '                        .build())\n'
                '                .build();')
    if old_void in s:
        s = s.replace(old_void, new_void)
        changed = True
    # space chain (separate method, ends with COSMIC_JELLYFISH)
    old_space = ('                .addFeature(GenerationStep.Decoration.RAW_GENERATION,\n'
                 '                        placedFeatures.getOrThrow(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED))\n'
                 '                .build();\n'
                 '    }\n\n'
                 '    private static ResourceKey<Biome> createKey(String name) {')
    new_space = ('                .addFeature(GenerationStep.Decoration.RAW_GENERATION,\n'
                 '                        placedFeatures.getOrThrow(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED))\n'
                 '                .addFeature(GenerationStep.Decoration.RAW_GENERATION,\n'
                 '                        placedFeatures.getOrThrow(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED))\n'
                 '                .build();\n'
                 '    }\n\n'
                 '    private static ResourceKey<Biome> createKey(String name) {')
    if old_space in s:
        s = s.replace(old_space, new_space)
        changed = True
    if changed:
        open(mb, 'w', encoding='utf-8').write(s)
print('ModBiomes done (changed=%s)' % changed)

lo = 'src/main/java/com/lit/spaceships/datagen/provider/ModChestLootTableProvider.java'
s = open(lo, encoding='utf-8').read()
if 'BATTLEFIELD_SALVAGE' not in s:
    s = s.replace('    public static final ResourceKey<LootTable> JELLY_HEART = ResourceKey.create(\n'
                  '            Registries.LOOT_TABLE,\n'
                  '            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/jelly_heart"));',
                  '    public static final ResourceKey<LootTable> JELLY_HEART = ResourceKey.create(\n'
                  '            Registries.LOOT_TABLE,\n'
                  '            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/jelly_heart"));\n\n'
                  '    public static final ResourceKey<LootTable> BATTLEFIELD_SALVAGE = ResourceKey.create(\n'
                  '            Registries.LOOT_TABLE,\n'
                  '            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/battlefield_salvage"));')
    loot_body = (
        '        // Battlefield Salvage: Waffen und Munition einer alten Schlacht\n'
        '        consumer.accept(BATTLEFIELD_SALVAGE, LootTable.lootTable()\n'
        '                .withPool(LootPool.lootPool()\n'
        '                        .setRolls(UniformGenerator.between(1.0F, 2.0F))\n'
        '                        .add(LootItem.lootTableItem(Items.IRON_SWORD).setWeight(4))\n'
        '                        .add(LootItem.lootTableItem(Items.SHIELD).setWeight(3))\n'
        '                        .add(LootItem.lootTableItem(Items.NETHERITE_SCRAP).setWeight(1))\n'
        '                        .add(LootItem.lootTableItem(Items.CROSSBOW).setWeight(2)))\n'
        '                .withPool(LootPool.lootPool()\n'
        '                        .setRolls(UniformGenerator.between(2.0F, 5.0F))\n'
        '                        .add(LootItem.lootTableItem(Items.ARROW).setWeight(10)\n'
        '                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 14.0F))))\n'
        '                        .add(LootItem.lootTableItem(Items.TNT).setWeight(3)\n'
        '                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))\n'
        '                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(8)\n'
        '                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))\n'
        '                        .add(LootItem.lootTableItem(Items.COAL).setWeight(8)\n'
        '                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 9.0F))))\n'
        '                        .add(LootItem.lootTableItem(Items.OBSIDIAN).setWeight(5)\n'
        '                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))));\n'
        '    }\n'
        '}')
    s = s.replace('                        .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(10)\n'
                  '                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))));\n'
                  '    }\n'
                  '}', '                        .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(10)\n'
                  '                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))));\n\n'
                  + loot_body)
    open(lo, 'w', encoding='utf-8').write(s)
print('loot done')
