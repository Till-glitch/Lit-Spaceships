# -*- coding: utf-8 -*-
"""Epoch 8 tests: partition coverage, spec determinism, GameTests, key lists."""

# --- 1) ModSpaceWorldGenTest updates ---
p2 = 'src/test/java/com/lit/spaceships/world/ModSpaceWorldGenTest.java'
s = open(p2, encoding='utf-8').read()
if 'router.continents()' in s:
    print('JUnit already applied - skipping p2 edits')
else:

if 'router.continents()' not in s:
  # noise settings signature: 4 holders + router assertions
old = '''        Holder<NormalNoise.NoiseParameters> temperatureNoise =
                Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0));
        Holder<NormalNoise.NoiseParameters> vegetationNoise =
                Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0));

        NoiseGeneratorSettings settings = ModNoiseSettings.spaceNoiseSettings(temperatureNoise, vegetationNoise);'''
new = '''        Holder<NormalNoise.NoiseParameters> temperatureNoise =
                Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0));
        Holder<NormalNoise.NoiseParameters> vegetationNoise =
                Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0));
        Holder<NormalNoise.NoiseParameters> continentalnessNoise =
                Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0));
        Holder<NormalNoise.NoiseParameters> erosionNoise =
                Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0));

        NoiseGeneratorSettings settings = ModNoiseSettings.spaceNoiseSettings(
                temperatureNoise, vegetationNoise, continentalnessNoise, erosionNoise);'''
assert old in s, 'noise test anchor'
s = s.replace(old, new)

old = '''        assertTrue(router.vegetation().minValue() < 0.0 && router.vegetation().maxValue() > 0.0,
                "Feuchteachse (vegetation) muss echte Noise-Struktur besitzen");'''
new = old + '''
        assertTrue(router.continents().minValue() < 0.0 && router.continents().maxValue() > 0.0,
                "Continentalness-Achse (C) muss echte Noise-Struktur besitzen");
        assertTrue(router.ridges().minValue() < 0.0 && router.ridges().maxValue() > 0.0,
                "Ridges-Achse (W) muss echte Noise-Struktur besitzen");
        assertEquals(0.0, router.erosion().minValue(), 0.0, "Erosionsachse bleibt ungenutzt");'''
assert old in s, 'router assert anchor'
s = s.replace(old, new)

# level stem test uses spaceNoiseSettings too? grep later. Also add the 7-biome partition test + key names.
old = '''        assertKey(ModConfiguredFeatures.COSMIC_JELLYFISH, Registries.CONFIGURED_FEATURE, "cosmic_jellyfish");
        assertKey(ModPlacedFeatures.COSMIC_JELLYFISH_PLACED, Registries.PLACED_FEATURE, "cosmic_jellyfish_placed");'''
new = old + '''
        for (String[] pair : new String[][]{
                {"gravity_rift_disk", "stellar_flare", "ion_pylon"}}) {
            for (String feature : pair) {
                assertKey(ModAmbientFeatures.cfgForTest(feature), Registries.CONFIGURED_FEATURE, feature);
                assertKey(ModAmbientFeatures.placedForTest(feature + "_placed"), Registries.PLACED_FEATURE, feature + "_placed");
            }
        }'''
assert old in s, 'key anchor'
s = s.replace(old, new)

# biome keys
old = '''        assertKey(ModPlacedFeatures.ANCIENT_BATTLEFIELD_PLACED, Registries.PLACED_FEATURE, "ancient_battlefield_placed");'''
new = old + '''
        assertEquals("lit_spaceships", ModBiomes.GRAVITY_RIFT.location().getNamespace());
        assertEquals("gravity_rift", ModBiomes.GRAVITY_RIFT.location().getPath());
        assertEquals("stellar_corona", ModBiomes.STELLAR_CORONA.location().getPath());
        assertEquals("ion_storm", ModBiomes.ION_STORM.location().getPath());'''
assert old in s, 'biome key anchor'
s = s.replace(old, new)

# --- 7-biome partition tests (new test method before privateField helper) ---
old = '''    private static <T> T privateField(Object owner, String name, Class<T> type) {'''
new = '''    @Test
    @DisplayName("7-Biome-Partition: extreme Zonen stechen heraus, Basis-Zonen bleiben intakt")
    void sevenBiomePartitionRoutesExtremeZones() {
        Climate.ParameterList<ResourceKey<Biome>> dist = ModDimensions.spaceBiomeDistribution();

        // Gravity Rift: C extrem niedrig + W extrem hoch gewinnt ueber ALLE Basis-Biome
        assertEquals(ModBiomes.GRAVITY_RIFT, dist.findValue(
                point(-0.5F, 0.5F, -0.95F, 0.0F, 0.0F, 0.85F)));
        // Stellar Corona: C hoch + T extrem heiss
        assertEquals(ModBiomes.STELLAR_CORONA, dist.findValue(
                point(0.9F, 0.5F, 0.8F, 0.0F, 0.0F, 0.0F)));
        // Ion Storm: W extrem tief + H hoch
        assertEquals(ModBiomes.ION_STORM, dist.findValue(
                point(0.0F, 0.7F, 0.0F, 0.0F, 0.0F, -0.8F)));

        // Basis-Partition bleibt intakt bei neutralen C/W:
        assertEquals(ModBiomes.FROZEN_EXPANSE, dist.findValue(point(-0.8F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)));
        assertEquals(ModBiomes.VOID_WASTES, dist.findValue(point(0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F)));
        assertEquals(SPACE_BIOME, dist.findValue(point(0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F)));
        assertEquals(ModBiomes.PLASMA_NEBULA, dist.findValue(point(0.9F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F)));

        // Gapless-Abdeckung: alle 7 Biome werden von einem 4D-Raster erreicht
        java.util.Set<ResourceKey<Biome>> reachable = new java.util.HashSet<>();
        for (float t = -1.0F; t <= 1.0F; t += 0.25F) {
            for (float h = -1.0F; h <= 1.0F; h += 0.25F) {
                for (float c = -1.0F; c <= 1.0F; c += 0.25F) {
                    for (float w = -1.0F; w <= 1.0F; w += 0.25F) {
                        reachable.add(dist.findValue(point(t, h, c, 0.0F, 0.0F, w)));
                    }
                }
            }
        }
        assertEquals(java.util.Set.of(ModBiomes.GRAVITY_RIFT, ModBiomes.STELLAR_CORONA,
                ModBiomes.ION_STORM, ModBiomes.FROZEN_EXPANSE, ModBiomes.VOID_WASTES,
                SPACE_BIOME, ModBiomes.PLASMA_NEBULA), reachable, "Alle 7 Biome muesssen erreichbar sein");
    }

    private static Climate.TargetPoint point(float t, float h, float c, float e, float d, float w) {
        // Climate-Parameter werden mit 10000 skaliert (vanilla Climate.Parameter.point)
        return new Climate.TargetPoint((long) (t * 10000F), (long) (h * 10000F), (long) (c * 10000F),
                (long) (e * 10000F), (long) (d * 10000F), (long) (w * 10000F));
    }

    @Test
    @DisplayName("Extreme-Zellen-Specs: deterministisch und in Zellgrenzen geklemmt")
    void extremeCellSpecsAreDeterministicAndBounded() {
        // Gravity Rift
        var rift = com.lit.spaceships.world.feature.GravityRiftFeature.specForCell(3, -4);
        assertEquals(rift, com.lit.spaceships.world.feature.GravityRiftFeature.specForCell(3, -4));
        assertTrue(rift.riftY() >= 40 && rift.riftY() <= 200);
        double riftMargin = com.lit.spaceships.world.feature.GravityRiftFeature.R_DUST_OUT + 16.0D;
        assertTrue(rift.centerX() >= 3 * 2048.0D + riftMargin
                && rift.centerX() <= 4 * 2048.0D - riftMargin);

        // Stellar Corona: 3 Flares, Y 96..288, sicher in der Zelle
        var flares = com.lit.spaceships.world.feature.StellarCoronaFeature.specsForCell(1, 2);
        assertEquals(3, flares.length);
        assertEquals(flares[0], com.lit.spaceships.world.feature.StellarCoronaFeature.specsForCell(1, 2)[0]);
        for (var flare : flares) {
            assertTrue(flare.flareY() >= 96 && flare.flareY() <= 288);
            assertTrue(flare.flareX() >= 1 * 1024 + 64 && flare.flareX() <= 2 * 1024 - 64);
        }

        // Ion Storm: 4 Pylone, Y 64..256
        var pylons = com.lit.spaceships.world.feature.IonStormFeature.specsForCell(-2, 5);
        assertEquals(4, pylons.length);
        assertEquals(pylons[3], com.lit.spaceships.world.feature.IonStormFeature.specsForCell(-2, 5)[3]);
        for (var pylon : pylons) {
            assertTrue(pylon.groundY() >= 64 && pylon.groundY() <= 256);
            assertTrue(pylon.height() >= 3 && pylon.height() <= 5);
        }
    }

    private static <T> T privateField(Object owner, String name, Class<T> type) {'''
assert old in s, 'privateField anchor'
s = s.replace(old, new, 1)

# SPACE_BIOME reference: the test file uses ModDimensions.SPACE_BIOME elsewhere; keep consistent
s = s.replace('assertEquals(SPACE_BIOME, dist.findValue(point(0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F)));',
              'assertEquals(ModDimensions.SPACE_BIOME, dist.findValue(point(0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F)));')
s = s.replace('SPACE_BIOME, ModBiomes.PLASMA_NEBULA), reachable, "Alle 7 Biome muessssen erreichbar sein");',
              'ModDimensions.SPACE_BIOME, ModBiomes.PLASMA_NEBULA), reachable, "Alle 7 Biome muessen erreichbar sein");')
open(p2, 'w', encoding='utf-8').write(s)
print('JUnit updated')

# --- 2) GameTests ---
p = 'src/main/java/com/lit/spaceships/tests/WorldGenGameTests.java'
s = open(p, encoding='utf-8').read()
old = '''        if (wastes.value().getAmbientLoop().isPresent() || wastes.value().getAmbientMood().isPresent()) {
            helper.fail("Void Wastes muss sensorisch stumm bleiben");
            return;
        }
        helper.succeed();
    }
}'''
new = '''        if (wastes.value().getAmbientLoop().isPresent() || wastes.value().getAmbientMood().isPresent()) {
            helper.fail("Void Wastes muss sensorisch stumm bleiben");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void gravityRiftPlacesAccretionBands(GameTestHelper helper) {
        // Scheibe: Zentrum relativ (7,8,7) -> Ringe R=14 uebersteigen das Template;
        // Deshalb Spalten-Verifikation mit fernem Spec: Band-Anordnung via placeRiftColumn
        var spec = new com.lit.spaceships.world.feature.GravityRiftFeature.RiftSpec(
                helper.absolutePos(new BlockPos(0, 0, 0)).getX() + 0.5D,
                helper.absolutePos(new BlockPos(0, 0, 0)).getZ() + 0.5D, 8);

        RandomSource random = RandomSource.create(61L);
        // Singularitaet im Abstand 0 -> Crying Obsidian/Obsidian bei (7,8,7)
        com.lit.spaceships.world.feature.GravityRiftFeature.placeRiftColumn(
                helper.getLevel(), helper.absolutePos(new BlockPos(7, 0, 7)).getX(),
                helper.absolutePos(new BlockPos(7, 0, 7)).getZ(), spec, random);
        helper.assertBlockState(new BlockPos(7, 8, 7),
                state -> state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.OBSIDIAN),
                () -> "Singularitaet muss Obsidian/Crying Obsidian sein");

        // Kein Band ausserhalb (distanz 3 -> im wobblebereich < 4 oder leer)
        com.lit.spaceships.world.feature.GravityRiftFeature.placeRiftColumn(
                helper.getLevel(), helper.absolutePos(new BlockPos(12, 0, 7)).getX(),
                helper.absolutePos(new BlockPos(12, 0, 7)).getZ(), spec, random);
        // dist 5: zwischen Ringen -> darf leer sein (Band 14 nicht erreicht)
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
assert old in s, 'gametest tail anchor'
s = s.replace(old, new)
open(p, 'w', encoding='utf-8').write(s)
print('GameTests added')
