# -*- coding: utf-8 -*-
"""Epoch 8: adds the 3 extreme-biome GameTests (JUnit part already applied)."""

p = 'src/main/java/com/lit/spaceships/tests/WorldGenGameTests.java'
s = open(p, encoding='utf-8').read()
if 'gravityRiftPlacesAccretionBands' in s:
    print('GameTests already applied')
else:
    anchor = '''        helper.assertBlock(new BlockPos(7, 9, 7), Blocks.COPPER_BLOCK::equals, "Pod-Diagonale muss Kupferversiegelung sein");

        helper.succeed();
    }
}'''
    assert anchor in s, 'pod tail anchor missing'
    new = '''        helper.assertBlock(new BlockPos(7, 9, 7), Blocks.COPPER_BLOCK::equals, "Pod-Diagonale muss Kupferversiegelung sein");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void gravityRiftPlacesAccretionBands(GameTestHelper helper) {
        // Scheiben-Spec: Zentrum weit weg (X+1000) -> Spalte im Testbereich liegt
        // zwischen den Ringen; Singularitaeten-Spalte separat mit nahem Zentrum.
        var farSpec = new com.lit.spaceships.world.feature.GravityRiftFeature.RiftSpec(
                helper.absolutePos(new BlockPos(0, 0, 0)).getX() + 1000.5D,
                helper.absolutePos(new BlockPos(0, 0, 0)).getZ() + 0.5D, 8);
        RandomSource random = RandomSource.create(61L);

        // Zwischen den Banden (dist ~1000): keine Platzierung
        boolean placed = com.lit.spaceships.world.feature.GravityRiftFeature.placeRiftColumn(
                helper.getLevel(), helper.absolutePos(new BlockPos(7, 0, 7)).getX(),
                helper.absolutePos(new BlockPos(7, 0, 7)).getZ(), farSpec, random);
        if (placed) {
            helper.fail("Spalte weit weg vom Zentrum darf keine Scheibe platzieren");
            return;
        }

        // Singularitaet (dist 0): Obsidian/Crying Obsidian auf der Rift-Ebene
        var nearSpec = new com.lit.spaceships.world.feature.GravityRiftFeature.RiftSpec(
                helper.absolutePos(new BlockPos(7, 0, 7)).getX() + 0.5D,
                helper.absolutePos(new BlockPos(7, 0, 7)).getZ() + 0.5D, 8);
        com.lit.spaceships.world.feature.GravityRiftFeature.placeRiftColumn(
                helper.getLevel(), helper.absolutePos(new BlockPos(7, 0, 7)).getX(),
                helper.absolutePos(new BlockPos(7, 0, 7)).getZ(), nearSpec, random);
        helper.assertBlockState(new BlockPos(7, 8, 7),
                state -> state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.OBSIDIAN),
                () -> "Singularitaet muss Obsidian/Crying Obsidian sein");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void stellarCoronaPlacesFlareHearth(GameTestHelper helper) {
        RandomSource random = RandomSource.create(63L);
        var c = helper.absolutePos(new BlockPos(7, 7, 7));
        com.lit.spaceships.world.feature.StellarCoronaFeature.placeFlare(
                helper.getLevel(), c.getX(), c.getY(), c.getZ(), random);

        helper.assertBlock(new BlockPos(7, 7, 7), Blocks.MAGMA_BLOCK::equals, "Flare-Kern muss Magma sein");
        helper.assertBlock(new BlockPos(8, 7, 7), Blocks.SMOOTH_BASALT::equals, "Flare-Ring muss glatter Basalt sein");
        helper.assertBlock(new BlockPos(7, 9, 7), Blocks.LAVA_CAULDRON::equals, "Thermaler Kamin muss Lava-Kessel tragen");
        helper.assertBlock(new BlockPos(9, 8, 9), Blocks.MAGMA_BLOCK::equals, "Bogenarm-Spitze muss Magma sein");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void ionStormPlacesChargedPylon(GameTestHelper helper) {
        RandomSource random = RandomSource.create(67L);
        var c = helper.absolutePos(new BlockPos(7, 6, 7));
        com.lit.spaceships.world.feature.IonStormFeature.placePylon(
                helper.getLevel(), c.getX(), c.getY(), c.getZ(), 3, true, random);

        helper.assertBlock(new BlockPos(7, 6, 7), Blocks.COPPER_BLOCK::equals, "Pylon-Fundament muss Kupfer sein");
        helper.assertBlock(new BlockPos(7, 10, 7), Blocks.LIGHTNING_ROD::equals, "Pylon-Spitze muss Blitzableiter sein");
        helper.assertBlock(new BlockPos(8, 9, 8), Blocks.COPPER_BULB::equals, "Pylon-Fuss muss Kupferbirne tragen");
        helper.assertBlock(new BlockPos(6, 5, 7), Blocks.CHAIN::equals, "Geladener Pylon muss Kettennetz haben");
        helper.assertBlock(new BlockPos(7, 6, 6), Blocks.COPPER_GRATE::equals, "Pylon-Rahmen muss Kupfergitter sein");
        helper.succeed();
    }
}'''
    s = s.replace(anchor, new)
    open(p, 'w', encoding='utf-8').write(s)
    print('GameTests added')
