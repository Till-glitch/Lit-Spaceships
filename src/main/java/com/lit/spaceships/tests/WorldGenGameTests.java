package com.lit.spaceships.tests;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.world.ModStructures;
import com.lit.spaceships.world.ModTemplatePools;
import com.lit.spaceships.world.feature.AsteroidBeltFeature;
import com.lit.spaceships.world.feature.MegaAsteroidFeature;
import com.lit.spaceships.world.feature.PlanetaryRingFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * In-World GameTests für die Weltraum-Weltgen-Features (100% serverseitig).
 * Das 15x15x15 Template ({@code worldgengametests.empty}) bietet Platz für
 * einen verkleinerten Mega-Asteroiden mit Radius 6 — dieselbe radiale
 * Schichten-Mathe wie im produktiven Maßstab (Radius 20-35).
 */
@GameTestHolder(LitSpaceships.MODID)
public class WorldGenGameTests {

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void megaAsteroidPlacesHollowGeodeStructure(GameTestHelper helper) {
        // Deterministischer Seed: reproduzierbare Krusten-/Mantel-Würfe
        RandomSource random = RandomSource.create(42L);

        boolean placed = MegaAsteroidFeature.placeEllipsoid(
                helper.getLevel(), helper.absolutePos(new BlockPos(7, 7, 7)),
                random, 6, 6, 6, 3);

        if (!placed) {
            helper.fail("MegaAsteroidFeature.placeEllipsoid meldete keine Platzierung");
            return;
        }

        // Geodenkammer im Zentrum (dist 0 <= geodeOuter - 2): komplett hohl
        helper.assertBlock(new BlockPos(7, 7, 7), Blocks.AIR::equals, "Zentrum muss die hohle Geodenkammer sein");

        // Amethyst-Mantel (dist 2 == geodeOuter - 1): Amethystblock oder sprossender Amethyst
        helper.assertBlockState(new BlockPos(9, 7, 7),
                state -> state.is(Blocks.AMETHYST_BLOCK) || state.is(Blocks.BUDDING_AMETHYST),
                () -> "dist 2 muss Amethyst-Geodenmantel sein");

        // Kalzit-Geodenhülle (dist 3 == geodeOuter): exakt Kalzit
        helper.assertBlock(new BlockPos(10, 7, 7), Blocks.CALCITE::equals, "dist 3 muss die Kalzit-Hülle sein");

        // Hohler Kavernenraum (dist 4, norm 0.444 < 0.55): Luft trotz Perturbation
        helper.assertBlock(new BlockPos(11, 7, 7), Blocks.AIR::equals, "dist 4 liegt im hohlen Kavernenraum");

        // Erz-Mantel (dist 5, norm ~0.69): fest, nie Luft
        helper.assertBlockState(new BlockPos(12, 7, 7),
                state -> !state.isAir(),
                () -> "dist 5 liegt im festen Mantel und darf nicht Luft sein");

        // Außenraum bleibt unberührt (norm 3 > 1)
        helper.assertBlock(new BlockPos(1, 1, 1), Blocks.AIR::equals, "Außenraum darf nicht verändert werden");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void planetaryRingPlacesSeamlessAnnulus(GameTestHelper helper) {
        // Verkleinerter Ring: R = 5, Dicke 1, Ring-Ebene Y 7 — Zentrum (7.5, 7.5)
        BlockPos center = helper.absolutePos(new BlockPos(7, 7, 7));
        PlanetaryRingFeature.RingSpec spec = new PlanetaryRingFeature.RingSpec(
                center.getX() + 0.5D, center.getZ() + 0.5D, 5.0D, center.getY(), 1);
        RandomSource random = RandomSource.create(11L);

        // Kardinalpunkte liegen exakt auf dem Ring (dist = 5.0, Delta 0)
        boolean anyColumn = false;
        for (int[] offset : new int[][]{{5, 0}, {-5, 0}, {0, 5}, {0, -5}}) {
            boolean placed = PlanetaryRingFeature.placeRingColumn(helper.getLevel(),
                    center.getX() + offset[0], center.getZ() + offset[1], spec, random);
            anyColumn |= placed;
            if (!placed) {
                helper.fail("Kardinalpunkt (" + offset[0] + "," + offset[1] + ") lag nicht im Annulus");
                return;
            }
        }
        if (!anyColumn) {
            helper.fail("Kein Ring-Segment platziert");
            return;
        }

        // Ringblock an den Kardinalpunkten (Palette: Eis / Glas / Staub)
        java.util.function.Predicate<BlockState> ringPalette = state -> state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.PACKED_ICE) || state.is(Blocks.ICE)
                || state.is(Blocks.LIGHT_BLUE_STAINED_GLASS) || state.is(Blocks.CYAN_STAINED_GLASS)
                || state.is(Blocks.WHITE_STAINED_GLASS)
                || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL);
        helper.assertBlockState(new BlockPos(12, 7, 7), ringPalette, () -> "Ost-Ringpunkt muss Ringpalette haben");
        helper.assertBlockState(new BlockPos(2, 7, 7), ringPalette, () -> "West-Ringpunkt muss Ringpalette haben");
        helper.assertBlockState(new BlockPos(7, 7, 12), ringPalette, () -> "Süd-Ringpunkt muss Ringpalette haben");
        helper.assertBlockState(new BlockPos(7, 7, 2), ringPalette, () -> "Nord-Ringpunkt muss Ringpalette haben");

        // Ringzentrum bleibt leer (dist 0, Delta 5 > 0.5)
        helper.assertBlock(new BlockPos(7, 7, 7), Blocks.AIR::equals, "Ringzentrum darf keine Ringblöcke haben");

        // Diagonale (dist ~4.24, Delta ~0.76 > 0.5) liegt außerhalb des 1-blöckigen Bands
        helper.assertBlock(new BlockPos(10, 7, 10), Blocks.AIR::equals, "Diagonale außerhalb der Bandbreite muss Luft bleiben");

        // Vertikale Dicke 1: oberhalb des Rings keine Blöcke
        helper.assertBlock(new BlockPos(12, 8, 7), Blocks.AIR::equals, "Ringdicke 1: keine zweite Ebene");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void asteroidBeltFragmentIsSolidOreBlob(GameTestHelper helper) {
        // Verkleinertes Fragment (Radius 3) im 15x15x15-Template
        RandomSource random = RandomSource.create(23L);
        AsteroidBeltFeature.placeFragment(helper.getLevel(),
                helper.absolutePos(new BlockPos(7, 7, 7)).getX(),
                helper.absolutePos(new BlockPos(7, 7, 7)).getY(),
                helper.absolutePos(new BlockPos(7, 7, 7)).getZ(), random, 3);

        // Kern (norm 0): fest, nie Luft
        helper.assertBlockState(new BlockPos(7, 7, 7),
                state -> !state.isAir(), () -> "Fragment-Kern darf nicht Luft sein");

        // Kruste (dist 3 am Achsenrand, norm 1.0 - Perturbation): Punkt im Rudel muss fest sein
        helper.assertBlockState(new BlockPos(9, 7, 7),
                state -> !state.isAir(), () -> "Fragment-Rand (dist 3 auf der Achse) muss fest sein");

        // Crust-/Ore-Palette (dist 2 vertikal, norm 0.444 +- 0.1 < 1): garantiert fest
        helper.assertBlockState(new BlockPos(7, 9, 7),
                state -> !state.isAir(), () -> "Fragment-Oberteil (dist 2) muss fest sein");

        // Außerhalb des Fragments (dist 5 > Radius 3 + Perturbation): Luft
        helper.assertBlock(new BlockPos(12, 7, 7), Blocks.AIR::equals, "Außerhalb des Fragments muss Luft bleiben");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void spaceStationDockingHubTemplateLoadsAndPlaces(GameTestHelper helper) {
        // Registry-Verifikation: Struktur + Set + Pools sind registriert
        var registryAccess = helper.getLevel().registryAccess();
        var structureRegistry = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var structureSetRegistry = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET);
        var poolRegistry = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL);
        if (!structureRegistry.containsKey(ModStructures.SPACE_STATION)) {
            helper.fail("Struktur lit_spaceships:space_station ist nicht registriert");
            return;
        }
        if (!structureSetRegistry.containsKey(ModStructures.SPACE_STATION_SET)) {
            helper.fail("StructureSet lit_spaceships:space_station ist nicht registriert");
            return;
        }
        if (!poolRegistry.containsKey(ModTemplatePools.SPACE_STATION_START)
                || !poolRegistry.containsKey(ModTemplatePools.SPACE_STATION_ROOMS)) {
            helper.fail("Template-Pools der Station sind nicht registriert");
            return;
        }

        // Template-Verifikation: Docking-Hub lädt (11x7x11) und platziert sich korrekt
        var template = helper.getLevel().getStructureManager()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.lit.spaceships.LitSpaceships.MODID, "space_station/docking_hub"))
                .orElse(null);
        if (template == null) {
            helper.fail("Template lit_spaceships:space_station/docking_hub wurde nicht geladen");
            return;
        }
        var size = template.getSize();
        if (size.getX() != 11 || size.getY() != 7 || size.getZ() != 11) {
            helper.fail("Docking-Hub hat unerwartete Größe: " + size);
            return;
        }

        // Platzierung im Testbereich (11x7x11 passt in das 15er-Template)
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        boolean placed = template.placeInWorld(helper.getLevel(), origin, origin,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                RandomSource.create(7L), 2);
        if (!placed) {
            helper.fail("Docking-Hub-Template konnte nicht platziert werden");
            return;
        }

        // Eisenwand (0,4,5), Glasfenster (0,3,5), Luft-Inneres (5,3,5), Jigsaw-Connector (10,2,5 relativ)
        helper.assertBlock(new BlockPos(2, 6, 7), Blocks.IRON_BLOCK::equals, "Westwand oben muss Eisen sein");
        helper.assertBlock(new BlockPos(2, 5, 7), Blocks.GLASS::equals, "Westwand Mitte muss Glasfenster sein");
        helper.assertBlock(new BlockPos(7, 5, 7), Blocks.AIR::equals, "Inneres muss Luft sein");
        helper.assertBlock(new BlockPos(12, 4, 7), Blocks.JIGSAW::equals, "Ost-Connector muss ein Jigsaw-Block sein");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void dreadnoughtWreckTemplateLoadsAndPlaces(GameTestHelper helper) {
        // Registry-Verifikation: Struktur + Set + Pools sind registriert
        var registryAccess = helper.getLevel().registryAccess();
        var structureRegistry = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var structureSetRegistry = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET);
        var poolRegistry = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL);
        if (!structureRegistry.containsKey(ModStructures.DREADNOUGHT_WRECK)) {
            helper.fail("Struktur lit_spaceships:dreadnought_wreck ist nicht registriert");
            return;
        }
        if (!structureSetRegistry.containsKey(ModStructures.DREADNOUGHT_WRECK_SET)) {
            helper.fail("StructureSet lit_spaceships:dreadnought_wreck ist nicht registriert");
            return;
        }
        if (!poolRegistry.containsKey(ModTemplatePools.DREADNOUGHT_WRECK_START)
                || !poolRegistry.containsKey(ModTemplatePools.DREADNOUGHT_WRECK_SECTIONS)) {
            helper.fail("Template-Pools des Dreadnoughts sind nicht registriert");
            return;
        }

        // Template-Verifikation: Command-Bridge lädt (13x7x13) und platziert sich korrekt
        var template = helper.getLevel().getStructureManager()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.lit.spaceships.LitSpaceships.MODID, "dreadnought_wreck/command_bridge"))
                .orElse(null);
        if (template == null) {
            helper.fail("Template lit_spaceships:dreadnought_wreck/command_bridge wurde nicht geladen");
            return;
        }
        var size = template.getSize();
        if (size.getX() != 13 || size.getY() != 7 || size.getZ() != 13) {
            helper.fail("Command-Bridge hat unerwartete Größe: " + size);
            return;
        }

        // Platzierung im Testbereich (13x7x13 passt in das 15er-Template bei Origin 1,2,1)
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        boolean placed = template.placeInWorld(helper.getLevel(), origin, origin,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                RandomSource.create(13L), 2);
        if (!placed) {
            helper.fail("Command-Bridge-Template konnte nicht platziert werden");
            return;
        }

        // Westwand (0,3,6), Nordfenster (6,2,0), Innenluft (6,3,6), Jigsaw-Connector (6,2,12 relativ)
        helper.assertBlock(new BlockPos(1, 5, 7), Blocks.POLISHED_BLACKSTONE_BRICKS::equals, "Westwand muss Polished Blackstone Bricks sein");
        helper.assertBlock(new BlockPos(7, 4, 1), Blocks.RED_STAINED_GLASS::equals, "Nordfenster muss rotes Glas sein");
        helper.assertBlock(new BlockPos(7, 5, 7), Blocks.AIR::equals, "Inneres muss Luft sein");
        helper.assertBlock(new BlockPos(7, 4, 13), Blocks.JIGSAW::equals, "Süd-Connector muss ein Jigsaw-Block sein");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void alienOutpostTemplateLoadsAndPlaces(GameTestHelper helper) {
        // Registry-Verifikation: Struktur + Set + Pools sind registriert
        var registryAccess = helper.getLevel().registryAccess();
        var structureRegistry = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var structureSetRegistry = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET);
        var poolRegistry = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL);
        if (!structureRegistry.containsKey(ModStructures.ALIEN_OUTPOST)) {
            helper.fail("Struktur lit_spaceships:alien_outpost ist nicht registriert");
            return;
        }
        if (!structureSetRegistry.containsKey(ModStructures.ALIEN_OUTPOST_SET)) {
            helper.fail("StructureSet lit_spaceships:alien_outpost ist nicht registriert");
            return;
        }
        if (!poolRegistry.containsKey(ModTemplatePools.ALIEN_OUTPOST_START)) {
            helper.fail("Template-Pool alien_outpost/start ist nicht registriert");
            return;
        }

        // Template-Verifikation: Monolith lädt (11x15x11) und platziert sich korrekt
        var template = helper.getLevel().getStructureManager()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.lit.spaceships.LitSpaceships.MODID, "alien_outpost/monolith"))
                .orElse(null);
        if (template == null) {
            helper.fail("Template lit_spaceships:alien_outpost/monolith wurde nicht geladen");
            return;
        }
        var size = template.getSize();
        if (size.getX() != 11 || size.getY() != 15 || size.getZ() != 11) {
            helper.fail("Alien-Monolith hat unerwartete Größe: " + size);
            return;
        }

        // Platzierung im Testbereich (11x15x11 passt in das 15er-Template bei Origin 2,0,2)
        BlockPos origin = helper.absolutePos(new BlockPos(2, 0, 2));
        boolean placed = template.placeInWorld(helper.getLevel(), origin, origin,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                RandomSource.create(42L), 2);
        if (!placed) {
            helper.fail("Monolith-Template konnte nicht platziert werden");
            return;
        }

        // Purpur-Säule (2,5,2), Lodestone-Kern (5,3,5), Seelaterne (5,4,5), Relikt-Kiste (5,2,3 relativ)
        helper.assertBlock(new BlockPos(4, 5, 4), Blocks.PURPUR_PILLAR::equals, "Ecksäule muss Purpursäule sein");
        helper.assertBlock(new BlockPos(7, 3, 7), Blocks.LODESTONE::equals, "Fokus-Kern muss Lodestone sein");
        helper.assertBlock(new BlockPos(7, 4, 7), Blocks.SEA_LANTERN::equals, "Kranz darüber muss Seelaterne sein");
        helper.assertBlock(new BlockPos(7, 2, 5), Blocks.CHEST::equals, "Altar muss Reliktkiste tragen");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void cosmicVaultTemplateLoadsAndPlaces(GameTestHelper helper) {
        var registryAccess = helper.getLevel().registryAccess();
        var structureRegistry = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var poolRegistry = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL);
        if (!structureRegistry.containsKey(ModStructures.COSMIC_VAULT)) {
            helper.fail("Struktur lit_spaceships:cosmic_vault ist nicht registriert");
            return;
        }
        if (!poolRegistry.containsKey(ModTemplatePools.COSMIC_VAULT_START)) {
            helper.fail("Template-Pool cosmic_vault/start ist nicht registriert");
            return;
        }

        var template = helper.getLevel().getStructureManager()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.lit.spaceships.LitSpaceships.MODID, "cosmic_vault/vault"))
                .orElse(null);
        if (template == null) {
            helper.fail("Template lit_spaceships:cosmic_vault/vault wurde nicht geladen");
            return;
        }
        var size = template.getSize();
        if (size.getX() != 9 || size.getY() != 7 || size.getZ() != 9) {
            helper.fail("Cosmic Vault hat unerwartete Größe: " + size);
            return;
        }

        BlockPos origin = helper.absolutePos(new BlockPos(3, 2, 2));
        boolean placed = template.placeInWorld(helper.getLevel(), origin, origin,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                RandomSource.create(9L), 2);
        if (!placed) {
            helper.fail("Vault-Template konnte nicht platziert werden");
            return;
        }

        // Versiegelte Obsidianhülle (Template 0,0,0 -> relativ 3,2,2), Fenster, Beutekiste
        helper.assertBlock(new BlockPos(3, 4, 2), Blocks.OBSIDIAN::equals, "Vault-Hülle muss Obsidian sein");
        helper.assertBlock(new BlockPos(7, 4, 10), Blocks.CYAN_STAINED_GLASS::equals, "Blickfenster muss Cyan-Glas sein");
        helper.assertBlock(new BlockPos(7, 3, 6), Blocks.CHEST::equals, "Vault-Kiste muss im Zentrum stehen");
        helper.assertBlock(new BlockPos(7, 7, 6), Blocks.SEA_LANTERN::equals, "Deckenleuchte ueber der Beute");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void pirateOutpostTemplateLoadsAndTraps(GameTestHelper helper) {
        var structureRegistry = helper.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        if (!structureRegistry.containsKey(ModStructures.PIRATE_OUTPOST)) {
            helper.fail("Struktur lit_spaceships:pirate_outpost ist nicht registriert");
            return;
        }

        var template = helper.getLevel().getStructureManager()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.lit.spaceships.LitSpaceships.MODID, "pirate_outpost/platform"))
                .orElse(null);
        if (template == null) {
            helper.fail("Template lit_spaceships:pirate_outpost/platform wurde nicht geladen");
            return;
        }
        var size = template.getSize();
        if (size.getX() != 11 || size.getY() != 6 || size.getZ() != 11) {
            helper.fail("Piraten-Plattform hat unerwartete Größe: " + size);
            return;
        }

        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        boolean placed = template.placeInWorld(helper.getLevel(), origin, origin,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                RandomSource.create(13L), 2);
        if (!placed) {
            helper.fail("Piraten-Plattform konnte nicht platziert werden");
            return;
        }

        // Cache-Kiste (Template 5,3,5 -> relativ 7,5,7) mit Falle: TNT direkt darunter!
        helper.assertBlock(new BlockPos(7, 5, 7), Blocks.CHEST::equals, "Beutekiste muss auf dem Podest stehen");
        helper.assertBlock(new BlockPos(7, 4, 7), Blocks.TNT::equals, "Unter der Kiste muss die TNT-Falle lauern");
        helper.assertBlock(new BlockPos(2, 5, 2), Blocks.SOUL_LANTERN::equals, "Seelenlaterne muss auf der Ecke stehen");
        helper.assertBlock(new BlockPos(5, 5, 2), Blocks.IRON_BARS::equals, "Gelaender muss Eisenstangen sein");

        helper.succeed();
    }
}
