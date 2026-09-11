# -*- coding: utf-8 -*-
"""Epoch 6 tests: ambient feature GameTests + JUnit updates."""

p = 'src/main/java/com/lit/spaceships/tests/WorldGenGameTests.java'
s = open(p, encoding='utf-8').read()
if 'ambientScatterPlacesWeightedBlocks' not in s:
    anchor = '''        helper.assertBlockState(new BlockPos(10, 8, 7),
                state -> state.is(Blocks.OBSIDIAN) || state.is(Blocks.MAGMA_BLOCK)
                        || state.is(Blocks.BLACKSTONE) || state.is(Blocks.DEEPSLATE)
                        || state.is(Blocks.IRON_BLOCK) || state.is(Blocks.AIR),
                () -> "Cluster-Rand muss verkohlt oder Luft sein");

        helper.succeed();
    }
}'''
    new = '''        helper.assertBlockState(new BlockPos(10, 8, 7),
                state -> state.is(Blocks.OBSIDIAN) || state.is(Blocks.MAGMA_BLOCK)
                        || state.is(Blocks.BLACKSTONE) || state.is(Blocks.DEEPSLATE)
                        || state.is(Blocks.IRON_BLOCK) || state.is(Blocks.AIR),
                () -> "Cluster-Rand muss verkohlt oder Luft sein");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void ambientScatterPlacesWeightedBlocks(GameTestHelper helper) {
        RandomSource random = RandomSource.create(51L);
        var palette = com.lit.spaceships.world.feature.ModAmbientPalettes.DEBRIS_FIELD;
        com.lit.spaceships.world.feature.ScatterBlockFeature.placeScatter(
                helper.getLevel(), helper.absolutePos(new BlockPos(7, 8, 7)),
                random, palette, 6, 3);

        // Mindestens ein Streublock muss in Reichweite liegen (Palette: Eisen/Beton)
        boolean any = false;
        for (int dx = -3; dx <= 3 && !any; dx++) {
            for (int dy = -1; dy <= 1 && !any; dy++) {
                for (int dz = -3; dz <= 3 && !any; dz++) {
                    var st = helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(7 + dx, 8 + dy, 7 + dz)));
                    if (st.is(Blocks.IRON_BLOCK) || st.is(Blocks.GRAY_CONCRETE)) {
                        any = true;
                    }
                }
            }
        }
        if (!any) {
            helper.fail("Streufeature muss Palette-Bloecke platzieren");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void ambientPillarPlacesColumnAndCap(GameTestHelper helper) {
        RandomSource random = RandomSource.create(53L);
        com.lit.spaceships.world.feature.PillarFeature.placePillar(
                helper.getLevel(), helper.absolutePos(new BlockPos(7, 6, 7)),
                random, com.lit.spaceships.world.feature.ModAmbientPalettes.FROST_PILLAR);

        // Saeule (min Hoehe 2) + Cap (max Hoehe 4+1) — beide Enden pruefen
        helper.assertBlock(new BlockPos(7, 6, 7), Blocks.PACKED_ICE::equals, "Saeulenbasis muss Packeis sein");
        helper.assertBlock(new BlockPos(7, 7, 7), Blocks.PACKED_ICE::equals, "Saeule muss Packeis sein");
        helper.assertBlock(new BlockPos(7, 10, 7), Blocks.BLUE_ICE::equals, "Maximale Saeule muss Blau-Eis-Kappe haben");
        helper.assertBlock(new BlockPos(7, 11, 7), Blocks.AIR::equals, "Ueber der Kappe muss Luft sein");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void ambientOrbPlacesShellAndCore(GameTestHelper helper) {
        com.lit.spaceships.world.feature.OrbFeature.placeOrb(
                helper.getLevel(), helper.absolutePos(new BlockPos(7, 8, 7)),
                com.lit.spaceships.world.feature.ModAmbientPalettes.CRYO_GEODE);

        // Kern (Zentrum) + Huelle (am Aequator-Rand)
        helper.assertBlock(new BlockPos(7, 8, 7), Blocks.ICE::equals, "Orb-Kern muss Eis sein");
        helper.assertBlock(new BlockPos(9, 8, 7), Blocks.BLUE_ICE::equals, "Orb-Huelle muss Blau-Eis sein");
        helper.assertBlock(new BlockPos(7, 11, 7), Blocks.BLUE_ICE::equals, "Orb-Scheitel muss Blau-Eis sein");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void ambientPodPlacesChestWithLoot(GameTestHelper helper) {
        RandomSource random = RandomSource.create(57L);
        com.lit.spaceships.world.feature.PodFeature.placePod(
                helper.getLevel(), helper.absolutePos(new BlockPos(6, 8, 6)),
                random, com.lit.spaceships.world.feature.PodFeature.CARGO_POD_LOOT);

        // Kiste an der unteren Ecke, Eisenschale, Kupferversiegelung diagonal
        helper.assertBlock(new BlockPos(6, 8, 6), Blocks.CHEST::equals, "Pod muss die Fracht-Kiste enthalten");
        helper.assertBlock(new BlockPos(7, 8, 6), Blocks.IRON_BLOCK::equals, "Pod-Schale muss Eisen sein");
        helper.assertBlock(new BlockPos(7, 9, 7), Blocks.COPPER_BLOCK::equals, "Pod-Diagonale muss Kupferversiegelung sein");

        helper.succeed();
    }
}'''
    assert anchor in s, 'tail anchor'
    s = s.replace(anchor, new)
    open(p, 'w', encoding='utf-8').write(s)
print('GameTests added')

p2 = 'src/test/java/com/lit/spaceships/world/ModSpaceWorldGenTest.java'
s = open(p2, encoding='utf-8').read()
if 'ambientFeaturesHaveCorrectKeys' not in s:
    # keys + loot entry
    s = s.replace('assertKey(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED, Registries.PLACED_FEATURE, "ancient_battlefield_placed");',
'''assertKey(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED, Registries.PLACED_FEATURE, "ancient_battlefield_placed");
        for (String[] name : new String[][]{
                {"debris_field", "meteor_shower", "void_crystal_spike", "beacon_pylon", "cargo_pod"},
                {"nebula_spore_drift", "plasma_ember", "nebula_gas_bloom", "crystal_lattice", "nebula_arc"},
                {"ice_shard_field", "glacier_floe", "frost_pillar", "snow_bloom", "cryo_geode"},
                {"bone_debris", "scrap_wasteland", "dust_drift", "ash_vent", "void_cyst"}}) {
            for (String feature : name) {
                ResourceKey<ConfiguredFeature<?, ?>> cfg = ModAmbientFeatures.cfgForTest(feature);
                assertKey(cfg, Registries.CONFIGURED_FEATURE, feature);
                assertKey(ModAmbientFeatures.placedForTest(feature), Registries.PLACED_FEATURE, feature + "_placed");
            }
        }''')
    # loot test entry
    s = s.replace('assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.BATTLEFIELD_SALVAGE));',
'''assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.BATTLEFIELD_SALVAGE));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.CARGO_POD));''')
    s = s.replace('assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.BATTLEFIELD_SALVAGE).build());',
'''assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.BATTLEFIELD_SALVAGE).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.CARGO_POD).build());''')
    # biome stepZero: append the 5 new per biome. space:
    s = s.replace('''                ModPlacedFeatures.COSMIC_JELLYFISH_PLACED, ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED), stepZeroKeys);''',
'''                ModPlacedFeatures.COSMIC_JELLYFISH_PLACED, ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED,
                ModAmbientFeatures.DEBRIS_FIELD_PLACED, ModAmbientFeatures.METEOR_SHOWER_PLACED,
                ModAmbientFeatures.VOID_CRYSTAL_SPIKE_PLACED, ModAmbientFeatures.BEACON_PYLON_PLACED,
                ModAmbientFeatures.CARGO_POD_PLACED), stepZeroKeys);''')
    # nebula (ends JELLYFISH only)
    s = s.replace('''        assertTrue(nebulaSteps.get(0).stream().anyMatch(h -> h.unwrapKey().orElseThrow()
                .equals(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED)));''',
'''        List<ResourceKey<PlacedFeature>> nebulaKeys = nebulaSteps.get(0).stream()
                .map(h -> h.unwrapKey().orElseThrow()).toList();
        assertEquals(List.of(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED,
                ModAmbientFeatures.NEBULA_SPORE_DRIFT_PLACED, ModAmbientFeatures.PLASMA_EMBER_PLACED,
                ModAmbientFeatures.NEBULA_GAS_BLOOM_PLACED, ModAmbientFeatures.CRYSTAL_LATTICE_PLACED,
                ModAmbientFeatures.NEBULA_ARC_PLACED), nebulaKeys);''')
    # frozen (ends BELT)
    s = s.replace('''        assertEquals(List.of(ModPlacedFeatures.ICE_COMET_PLACED, ModPlacedFeatures.MEGA_ASTEROID_PLACED,
                ModPlacedFeatures.PLANETARY_RING_PLACED, ModPlacedFeatures.ASTEROID_BELT_PLACED), stepZeroKeys);''',
'''        assertEquals(List.of(ModPlacedFeatures.ICE_COMET_PLACED, ModPlacedFeatures.MEGA_ASTEROID_PLACED,
                ModPlacedFeatures.PLANETARY_RING_PLACED, ModPlacedFeatures.ASTEROID_BELT_PLACED,
                ModAmbientFeatures.ICE_SHARD_FIELD_PLACED, ModAmbientFeatures.GLACIER_FLOE_PLACED,
                ModAmbientFeatures.FROST_PILLAR_PLACED, ModAmbientFeatures.SNOW_BLOOM_PLACED,
                ModAmbientFeatures.CRYO_GEODE_PLACED), stepZeroKeys);''')
    # void (ends GRAVEYARD + BATTLEFIELD)
    s = s.replace('''                ModPlacedFeatures.ASTEROID_BELT_PLACED, ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED,
                ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED), stepZeroKeys);''',
'''                ModPlacedFeatures.ASTEROID_BELT_PLACED, ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED,
                ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED, ModAmbientFeatures.BONE_DEBRIS_PLACED,
                ModAmbientFeatures.SCRAP_WASTELAND_PLACED, ModAmbientFeatures.DUST_DRIFT_PLACED,
                ModAmbientFeatures.ASH_VENT_PLACED, ModAmbientFeatures.VOID_CYST_PLACED), stepZeroKeys);''')
    # import
    s = s.replace('import com.lit.spaceships.world.feature.MegaAsteroidFeature;',
'''import com.lit.spaceships.world.feature.MegaAsteroidFeature;
import com.lit.spaceships.world.ModAmbientFeatures;''')
    open(p2, 'w', encoding='utf-8').write(s)
print('JUnit updated')
