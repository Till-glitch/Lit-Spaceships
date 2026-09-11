# -*- coding: utf-8 -*-
"""Adds battlefield GameTest + JUnit updates (cycle 21)."""
p = 'src/main/java/com/lit/spaceships/tests/WorldGenGameTests.java'
s = open(p, encoding='utf-8').read()
if 'ancientBattlefieldPlacesScorchedCluster' not in s:
    old_tail = '''        helper.assertBlock(new BlockPos(7, 7, 7), Blocks.GLASS::equals, "Kuppelscheitel muss Glas sein");

        helper.succeed();
    }
}'''
    new_tail = '''        helper.assertBlock(new BlockPos(7, 7, 7), Blocks.GLASS::equals, "Kuppelscheitel muss Glas sein");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void ancientBattlefieldPlacesScorchedCluster(GameTestHelper helper) {
        RandomSource random = RandomSource.create(43L);
        com.lit.spaceships.world.feature.AncientBattlefieldFeature.placeWreckCluster(
                helper.getLevel(), helper.absolutePos(new BlockPos(7, 8, 7)).getX(),
                helper.absolutePos(new BlockPos(7, 8, 7)).getY(),
                helper.absolutePos(new BlockPos(7, 8, 7)).getZ(), random, true);

        // Bergungs-Kiste obenauf (y = 8 + radius 2..4)
        helper.assertBlock(new BlockPos(7, 11, 7), Blocks.CHEST::equals, "Salvage-Kiste muss obenauf liegen");

        // Rand des Clusters: verkohlte Mischung oder Luft (Perturbation)
        helper.assertBlockState(new BlockPos(10, 8, 7),
                state -> state.is(Blocks.OBSIDIAN) || state.is(Blocks.MAGMA_BLOCK)
                        || state.is(Blocks.BLACKSTONE) || state.is(Blocks.DEEPSLATE)
                        || state.is(Blocks.IRON_BLOCK) || state.is(Blocks.AIR),
                () -> "Cluster-Rand muss verkohlt oder Luft sein");

        helper.succeed();
    }
}'''
    assert old_tail in s, 'tail anchor missing'
    s = s.replace(old_tail, new_tail)
    open(p, 'w', encoding='utf-8').write(s)
print('GameTest done')

p2 = 'src/test/java/com/lit/spaceships/world/ModSpaceWorldGenTest.java'
s = open(p2, encoding='utf-8').read()
if 'ANCIENT_BATTLEFIELD' not in s:
    s = s.replace('''        com.lit.spaceships.world.feature.CosmicJellyfishFeature cosmicJellyfish =
                new com.lit.spaceships.world.feature.CosmicJellyfishFeature(NoneFeatureConfiguration.CODEC);''',
'''        com.lit.spaceships.world.feature.CosmicJellyfishFeature cosmicJellyfish =
                new com.lit.spaceships.world.feature.CosmicJellyfishFeature(NoneFeatureConfiguration.CODEC);
        com.lit.spaceships.world.feature.AncientBattlefieldFeature ancientBattlefield =
                new com.lit.spaceships.world.feature.AncientBattlefieldFeature(NoneFeatureConfiguration.CODEC);''')
    s = s.replace('asteroidBelt, satelliteGraveyard, cosmicJellyfish);',
                  'asteroidBelt, satelliteGraveyard, cosmicJellyfish, ancientBattlefield);')
    s = s.replace('verify(configuredContext).register(eq(ModConfiguredFeatures.COSMIC_JELLYFISH), any());',
'''verify(configuredContext).register(eq(ModConfiguredFeatures.COSMIC_JELLYFISH), any());
        verify(configuredContext).register(eq(ModConfiguredFeatures.ANCIENT_BATTLEFIELD), any());''')
    s = s.replace('assertKey(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED, Registries.PLACED_FEATURE, "cosmic_jellyfish_placed");',
'''assertKey(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED, Registries.PLACED_FEATURE, "cosmic_jellyfish_placed");
        assertKey(ModConfiguredFeatures.ANCIENT_BATTLEFIELD, Registries.CONFIGURED_FEATURE, "ancient_battlefield");
        assertKey(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED, Registries.PLACED_FEATURE, "ancient_battlefield_placed");''')
    s = s.replace('''        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.COSMIC_JELLYFISH_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED);''',
'''        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.COSMIC_JELLYFISH_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED);
        doReturn(Holder.Reference.createStandAlone(placedOwner, ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED))
                .when(placedGetter).getOrThrow(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED);''')
    # space stepZero += battlefield
    s = s.replace('''                ModPlacedFeatures.ASTEROID_BELT_PLACED, ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED,
                ModPlacedFeatures.COSMIC_JELLYFISH_PLACED), stepZeroKeys);''',
'''                ModPlacedFeatures.ASTEROID_BELT_PLACED, ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED,
                ModPlacedFeatures.COSMIC_JELLYFISH_PLACED, ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED), stepZeroKeys);''')
    # void stepZero += battlefield
    s = s.replace('''                ModPlacedFeatures.ASTEROID_BELT_PLACED, ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED), stepZeroKeys);''',
'''                ModPlacedFeatures.ASTEROID_BELT_PLACED, ModPlacedFeatures.SATELLITE_GRAVEYARD_PLACED,
                ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED), stepZeroKeys);''')
    # loot test
    s = s.replace('assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.JELLY_HEART));',
'''assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.JELLY_HEART));
        assertTrue(tables.containsKey(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.BATTLEFIELD_SALVAGE));''')
    s = s.replace('assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.JELLY_HEART).build());',
'''assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.JELLY_HEART).build());
        assertNotNull(tables.get(com.lit.spaceships.datagen.provider.ModChestLootTableProvider.BATTLEFIELD_SALVAGE).build());''')
    open(p2, 'w', encoding='utf-8').write(s)
print('JUnit done')
