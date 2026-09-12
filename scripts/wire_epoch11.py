# -*- coding: utf-8 -*-
"""Epoch 11+12 wiring: pools, structures, structure sets, loot tables, i18n."""

# ---------- ModTemplatePools ----------
tp = 'src/main/java/com/lit/spaceships/world/ModTemplatePools.java'
s = open(tp, encoding='utf-8').read()
if 'BEHEMOTH_BRIDGE' not in s:
    s = s.replace('''    public static final ResourceKey<StructureTemplatePool> COLONY_DOME_START =
            createKey("colony_dome/start");''',
'''    public static final ResourceKey<StructureTemplatePool> COLONY_DOME_START =
            createKey("colony_dome/start");
    public static final ResourceKey<StructureTemplatePool> BEHEMOTH_BRIDGE =
            createKey("behemoth_freighter/start");
    public static final ResourceKey<StructureTemplatePool> BEHEMOTH_SECTIONS =
            createKey("behemoth_freighter/sections");
    public static final ResourceKey<StructureTemplatePool> BEHEMOTH_END =
            createKey("behemoth_freighter/end");
    public static final ResourceKey<StructureTemplatePool> RELAY_ARRAY_START =
            createKey("relay_array/start");
    public static final ResourceKey<StructureTemplatePool> SOLAR_COLLECTOR_START =
            createKey("solar_collector/start");
    public static final ResourceKey<StructureTemplatePool> DEEP_OUTPOST_START =
            createKey("deep_outpost/start");''')
    anchor = '''        // Cosmic Vault (seltenste Struktur des Void)
        context.register(COSMIC_VAULT_START, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:cosmic_vault/vault"), 1)
        ), StructureTemplatePool.Projection.RIGID));'''
    assert anchor in s
    add = anchor + '''

        // Behemoth Freighter (Bridge -> Sections -> Engineering)
        context.register(BEHEMOTH_BRIDGE, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:behemoth_freighter/bridge"), 1)
        ), StructureTemplatePool.Projection.RIGID));
        context.register(BEHEMOTH_SECTIONS, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:behemoth_freighter/cargo_bay"), 3),
                Pair.of(StructurePoolElement.single("lit_spaceships:behemoth_freighter/corridor_fractured"), 2)
        ), StructureTemplatePool.Projection.RIGID));
        context.register(BEHEMOTH_END, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:behemoth_freighter/engineering_bay"), 1)
        ), StructureTemplatePool.Projection.RIGID));

        // Relay Array (Antennen-Gitter mit Research-Beacon)
        context.register(RELAY_ARRAY_START, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:relay_array/array"), 1)
        ), StructureTemplatePool.Projection.RIGID));

        // Solar Collector (Thermal-Plattform, stellar corona)
        context.register(SOLAR_COLLECTOR_START, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:solar_collector/collector"), 1)
        ), StructureTemplatePool.Projection.RIGID));

        // Deep Outpost (hohler Asteroid, gravity rift)
        context.register(DEEP_OUTPOST_START, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:deep_outpost/outpost"), 1)
        ), StructureTemplatePool.Projection.RIGID));'''
    s = s.replace(anchor, add, 1)
    open(tp, 'w', encoding='utf-8').write(s)
print('pools done')

# ---------- ModStructures ----------
st = 'src/main/java/com/lit/spaceships/world/ModStructures.java'
s = open(st, encoding='utf-8').read()
if 'BEHEMOTH_FREIGHTER' not in s:
    # keys after COSMIC_VAULT_SET block
    anchor = '''    public static final ResourceKey<StructureSet> COSMIC_VAULT_SET =
            ResourceKey.create(Registries.STRUCTURE_SET,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "cosmic_vault"));'''
    assert anchor in s
    add = anchor + '''

    public static final ResourceKey<Structure> BEHEMOTH_FREIGHTER =
            ResourceKey.create(Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "behemoth_freighter"));
    public static final ResourceKey<StructureSet> BEHEMOTH_FREIGHTER_SET =
            ResourceKey.create(Registries.STRUCTURE_SET,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "behemoth_freighter"));
    public static final ResourceKey<Structure> RELAY_ARRAY =
            ResourceKey.create(Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "relay_array"));
    public static final ResourceKey<StructureSet> RELAY_ARRAY_SET =
            ResourceKey.create(Registries.STRUCTURE_SET,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "relay_array"));
    public static final ResourceKey<Structure> SOLAR_COLLECTOR =
            ResourceKey.create(Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "solar_collector"));
    public static final ResourceKey<StructureSet> SOLAR_COLLECTOR_SET =
            ResourceKey.create(Registries.STRUCTURE_SET,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "solar_collector"));
    public static final ResourceKey<Structure> DEEP_OUTPOST =
            ResourceKey.create(Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "deep_outpost"));
    public static final ResourceKey<StructureSet> DEEP_OUTPOST_SET =
            ResourceKey.create(Registries.STRUCTURE_SET,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "deep_outpost"));'''
    s = s.replace(anchor, add, 1)

    # structure bootstraps: append after ion storm era (anchor: end of bootstrapStructure after freighter? use the vault register block end)
    anchor2 = '''        context.register(COSMIC_VAULT, new JigsawStructure(
                vaultSettings,
                vaultStartPool,
                1,
                UniformHeight.of(VerticalAnchor.absolute(64), VerticalAnchor.absolute(200)),
                false));'''
    assert anchor2 in s
    add2 = anchor2 + '''

        // 10. Behemoth Freighter (Space Biome & Void Wastes - schwerer Frachter)
        Holder<StructureTemplatePool> freighterBridge =
                pools.getOrThrow(ModTemplatePools.BEHEMOTH_BRIDGE);
        Structure.StructureSettings freighterSettings = new Structure.StructureSettings(
                HolderSet.direct(
                        biomes.getOrThrow(ModDimensions.SPACE_BIOME),
                        biomes.getOrThrow(ModBiomes.VOID_WASTES)),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE);
        context.register(BEHEMOTH_FREIGHTER, new JigsawStructure(
                freighterSettings, freighterBridge, 4,
                UniformHeight.of(VerticalAnchor.absolute(48), VerticalAnchor.absolute(184)), false));

        // 11. Relay Array (Space Biome & Plasma Nebula - Antennen-Relais)
        Holder<StructureTemplatePool> relayPool =
                pools.getOrThrow(ModTemplatePools.RELAY_ARRAY_START);
        Structure.StructureSettings relaySettings = new Structure.StructureSettings(
                HolderSet.direct(
                        biomes.getOrThrow(ModDimensions.SPACE_BIOME),
                        biomes.getOrThrow(ModBiomes.PLASMA_NEBULA)),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE);
        context.register(RELAY_ARRAY, new JigsawStructure(
                relaySettings, relayPool, 1,
                UniformHeight.of(VerticalAnchor.absolute(96), VerticalAnchor.absolute(240)), false));

        // 12. Solar Collector (nur Stellar Corona - thermische Ernte)
        Holder<StructureTemplatePool> solarPool =
                pools.getOrThrow(ModTemplatePools.SOLAR_COLLECTOR_START);
        Structure.StructureSettings solarSettings = new Structure.StructureSettings(
                HolderSet.direct(biomes.getOrThrow(ModBiomes.STELLAR_CORONA)),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE);
        context.register(SOLAR_COLLECTOR, new JigsawStructure(
                solarSettings, solarPool, 1,
                UniformHeight.of(VerticalAnchor.absolute(120), VerticalAnchor.absolute(288)), false));

        // 13. Deep Outpost (nur Gravity Rift - Horchposten im Asteroiden)
        Holder<StructureTemplatePool> outpostPool =
                pools.getOrThrow(ModTemplatePools.DEEP_OUTPOST_START);
        Structure.StructureSettings outpostSettings = new Structure.StructureSettings(
                HolderSet.direct(biomes.getOrThrow(ModBiomes.GRAVITY_RIFT)),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE);
        context.register(DEEP_OUTPOST, new JigsawStructure(
                outpostSettings, outpostPool, 1,
                UniformHeight.of(VerticalAnchor.absolute(48), VerticalAnchor.absolute(184)), false));'''
    s = s.replace(anchor2, add2, 1)

    # structure sets
    anchor3 = '''        Holder<Structure> vault = structures.getOrThrow(COSMIC_VAULT);
        context.register(COSMIC_VAULT_SET, new StructureSet(vault,
                new RandomSpreadStructurePlacement(64, 20, RandomSpreadType.LINEAR, 2095820113)));'''
    assert anchor3 in s
    add3 = anchor3 + '''

        Holder<Structure> freighter = structures.getOrThrow(BEHEMOTH_FREIGHTER);
        context.register(BEHEMOTH_FREIGHTER_SET, new StructureSet(freighter,
                new RandomSpreadStructurePlacement(56, 20, RandomSpreadType.LINEAR, 2153091247)));

        Holder<Structure> relay = structures.getOrThrow(RELAY_ARRAY);
        context.register(RELAY_ARRAY_SET, new StructureSet(relay,
                new RandomSpreadStructurePlacement(46, 16, RandomSpreadType.LINEAR, 2207113411)));

        Holder<Structure> solar = structures.getOrThrow(SOLAR_COLLECTOR);
        context.register(SOLAR_COLLECTOR_SET, new StructureSet(solar,
                new RandomSpreadStructurePlacement(52, 18, RandomSpreadType.LINEAR, 2267116123)));

        Holder<Structure> outpost = structures.getOrThrow(DEEP_OUTPOST);
        context.register(DEEP_OUTPOST_SET, new StructureSet(outpost,
                new RandomSpreadStructurePlacement(58, 22, RandomSpreadType.LINEAR, 2319022117)));'''
    s = s.replace(anchor3, add3, 1)
    open(st, 'w', encoding='utf-8').write(s)
print('structures done')

# ---------- Loot tables (Epoch 12.4) ----------
lo = 'src/main/java/com/lit/spaceships/datagen/provider/ModChestLootTableProvider.java'
s = open(lo, encoding='utf-8').read()
if 'BEHEMOTH_MANIFEST' not in s:
    s = s.replace('''    public static final ResourceKey<LootTable> CARGO_POD = PodFeature.CARGO_POD_LOOT;''',
'''    public static final ResourceKey<LootTable> CARGO_POD = PodFeature.CARGO_POD_LOOT;

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
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/deep_outpost_archive"));''')

    tables = '''        // Behemoth Manifest: Industrie-Templates, Mineralbloecke, Schwermaschinen-Teile
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
}'''
    old_tail = '''                        .add(LootItem.lootTableItem(Items.OBSIDIAN).setWeight(5)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))));
    }
}'''
    assert old_tail in s
    s = s.replace(old_tail, '''                        .add(LootItem.lootTableItem(Items.OBSIDIAN).setWeight(5)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))));

''' + tables)
    open(lo, 'w', encoding='utf-8').write(s)
print('loot tables done')

# ---------- i18n structure names ----------
mi = 'src/main/java/com/lit/spaceships/registry/ModI18n.java'
s = open(mi, encoding='utf-8').read()
if 'BEHEMOTH' not in s:
    s = s.replace('        public static final String COLONY_DOME = "structure." + LitSpaceships.MODID + ".colony_dome";',
'''        public static final String COLONY_DOME = "structure." + LitSpaceships.MODID + ".colony_dome";
        public static final String BEHEMOTH_FREIGHTER = "structure." + LitSpaceships.MODID + ".behemoth_freighter";
        public static final String RELAY_ARRAY = "structure." + LitSpaceships.MODID + ".relay_array";
        public static final String SOLAR_COLLECTOR = "structure." + LitSpaceships.MODID + ".solar_collector";
        public static final String DEEP_OUTPOST = "structure." + LitSpaceships.MODID + ".deep_outpost";''')
    open(mi, 'w', encoding='utf-8').write(s)

en = 'src/main/java/com/lit/spaceships/datagen/provider/ModEnglishLanguageProvider.java'
s = open(en, encoding='utf-8').read()
if 'Behemoth Freighter' not in s:
    anchor = 'add(ModI18n.Structure.COLONY_DOME, "Abandoned Colony Dome");'
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
    anchor = 'add(ModI18n.Structure.COLONY_DOME, "Verlassene Koloniekuppel");'
    assert anchor in s
    s = s.replace(anchor, anchor + '''
        add(ModI18n.Structure.BEHEMOTH_FREIGHTER, "Behemoth-Frachter");
        add(ModI18n.Structure.RELAY_ARRAY, "Relais-Array");
        add(ModI18n.Structure.SOLAR_COLLECTOR, "Solarkollektor");
        add(ModI18n.Structure.DEEP_OUTPOST, "Tiefer Horchposten");''')
    open(de, 'w', encoding='utf-8').write(s)
print('i18n done')
