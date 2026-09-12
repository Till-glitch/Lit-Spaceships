# -*- coding: utf-8 -*-
"""Epoch 11: loot tables + i18n (pools/structures already wired)."""

lo = 'src/main/java/com/lit/spaceships/datagen/provider/ModChestLootTableProvider.java'
s = open(lo, encoding='utf-8').read()
if 'BEHEMOTH_MANIFEST' not in s:
    anchor = '    public static final ResourceKey<LootTable> CARGO_POD = PodFeature.CARGO_POD_LOOT;'
    assert anchor in s
    keys = anchor + '''

    public static final ResourceKey<LootTable> BEHEMOTH_MANIFEST = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/behemoth_manifest"));

    public static final ResourceKey<LootTable> REACTOR_CORE_SALVAGE = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/reactor_core_salvage"));

    public static final ResourceKey<LootTable> RELAY_INTERCEPT = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/relay_intercept"));

    public static final ResourceKey<LootTable> SOLAR_HARVEST = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/solar_harvest"));

    public static final ResourceKey<LootTable> DEEP_OUTPOST_ARCHIVE = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/deep_outpost_archive"));'''
    s = s.replace(anchor, keys, 1)

    old_tail = "                        .add(LootItem.lootTableItem(Items.MUSIC_DISC_OTHERSIDE).setWeight(1))));\n    }\n}"
    assert old_tail in s, 'music disc tail'
    new_tail = """                        .add(LootItem.lootTableItem(Items.MUSIC_DISC_OTHERSIDE).setWeight(1))));

        // Behemoth Manifest: Industrie-Templates, Mineralbloecke, Schwermaschinen-Teile
        consumer.accept(BEHEMOTH_MANIFEST, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 4.0F))
                        .add(LootItem.lootTableItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).setWeight(1))
                        .add(LootItem.lootTableItem(Items.IRON_BLOCK).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.COPPER_BLOCK).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.PISTON).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .add(LootItem.lootTableItem(Items.HOPPER).setWeight(3)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 9.0F))))
                        .add(LootItem.lootTableItem(Items.TUFF).setWeight(6)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))));

        // Reactor Core Salvage: Plasmakerne, Netherite, Hochvolt-Kondensatoren
        consumer.accept(REACTOR_CORE_SALVAGE, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(LootItem.lootTableItem(Items.NETHERITE_SCRAP).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.REDSTONE_BLOCK).setWeight(6)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 4.0F))
                        .add(LootItem.lootTableItem(Items.GLOWSTONE_DUST).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 10.0F))))
                        .add(LootItem.lootTableItem(Items.MAGMA_CREAM).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
                        .add(LootItem.lootTableItem(Items.CRYING_OBSIDIAN).setWeight(5)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))));

        // Relay Intercept: verschluesselte Logs, Echo Shards, Transponder
        consumer.accept(RELAY_INTERCEPT, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(LootItem.lootTableItem(Items.ECHO_SHARD).setWeight(6)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.COMPASS).setWeight(2))
                        .add(LootItem.lootTableItem(Items.RECOVERY_COMPASS).setWeight(1)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(LootItem.lootTableItem(Items.PAPER).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.BOOK).setWeight(6)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))));

        // Solar Harvest: Kollektor-Zellen, Prismarinkristalle, Strahllegierungen
        consumer.accept(SOLAR_HARVEST, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(LootItem.lootTableItem(Items.PRISMARINE_CRYSTALS).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.GLOWSTONE).setWeight(5)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 4.0F))
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                        .add(LootItem.lootTableItem(Items.COPPER_INGOT).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.DAYLIGHT_DETECTOR).setWeight(3))));

        // Deep Outpost Archive: Sternkarten, Nether Stars, verzauberte Buecher
        consumer.accept(DEEP_OUTPOST_ARCHIVE, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 1.0F))
                        .add(LootItem.lootTableItem(Items.NETHER_STAR).setWeight(1))
                        .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(3))
                        .add(LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(LootItem.lootTableItem(Items.ENDER_EYE).setWeight(6)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.ECHO_SHARD).setWeight(6)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .add(LootItem.lootTableItem(Items.OBSIDIAN).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))));
    }
}"""
    s = s.replace(old_tail, new_tail, 1)
    open(lo, 'w', encoding='utf-8').write(s)
print('loot tables done')

mi = 'src/main/java/com/lit/spaceships/registry/ModI18n.java'
s = open(mi, encoding='utf-8').read()
if 'BEHEMOTH' not in s:
    anchor = '        public static final String COLONY_DOME = "structure." + LitSpaceships.MODID + ".colony_dome";'
    assert anchor in s
    s = s.replace(anchor, anchor + '''
        public static final String BEHEMOTH_FREIGHTER = "structure." + LitSpaceships.MODID + ".behemoth_freighter";
        public static final String RELAY_ARRAY = "structure." + LitSpaceships.MODID + ".relay_array";
        public static final String SOLAR_COLLECTOR = "structure." + LitSpaceships.MODID + ".solar_collector";
        public static final String DEEP_OUTPOST = "structure." + LitSpaceships.MODID + ".deep_outpost";''')
    open(mi, 'w', encoding='utf-8').write(s)

en = 'src/main/java/com/lit/spaceships/datagen/provider/ModEnglishLanguageProvider.java'
s = open(en, encoding='utf-8').read()
if 'Behemoth Freighter' not in s:
    anchor = '        add(ModI18n.Structure.COLONY_DOME, "Abandoned Colony Dome");'
    assert anchor in s
    s = s.replace(anchor, anchor + '''
        add(ModI18n.Structure.BEHEMOTH_FREIGHTER, "Behemoth Freighter");
        add(ModI18n.Structure.RELAY_ARRAY, "Relay Array");
        add(ModI18n.Structure.SOLAR_COLLECTOR, "Solar Collector");
        add(ModI18n.Structure.DEEP_OUTPOST, "Deep Listening Outpost");''')
    open(en, 'w', encoding='utf-8').write(s)

de = 'src/main/java/com/lit/spaceships/datagen/provider/ModGermanLanguageProvider.java'
s = open(de, encoding='utf-8').read()
if 'Behemoth-Frachter' not in s:
    anchor = '        add(ModI18n.Structure.COLONY_DOME, "Verlassene Koloniekuppel");'
    assert anchor in s
    s = s.replace(anchor, anchor + '''
        add(ModI18n.Structure.BEHEMOTH_FREIGHTER, "Behemoth-Frachter");
        add(ModI18n.Structure.RELAY_ARRAY, "Relais-Array");
        add(ModI18n.Structure.SOLAR_COLLECTOR, "Solarkollektor");
        add(ModI18n.Structure.DEEP_OUTPOST, "Tiefer Horchposten");''')
    open(de, 'w', encoding='utf-8').write(s)
print('i18n done')
