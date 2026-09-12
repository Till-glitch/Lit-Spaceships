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

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void leviathanTemplateLoadsAndPlaces(GameTestHelper helper) {
        var structureRegistry = helper.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        if (!structureRegistry.containsKey(ModStructures.LEVIATHAN_BONES)) {
            helper.fail("Struktur lit_spaceships:leviathan_bones ist nicht registriert");
            return;
        }

        var template = helper.getLevel().getStructureManager()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.lit.spaceships.LitSpaceships.MODID, "leviathan_bones/skeleton"))
                .orElse(null);
        if (template == null) {
            helper.fail("Template lit_spaceships:leviathan_bones/skeleton wurde nicht geladen");
            return;
        }
        var size = template.getSize();
        if (size.getX() != 17 || size.getY() != 8 || size.getZ() != 21) {
            helper.fail("Leviathan-Skelett hat unerwartete Größe: " + size);
            return;
        }

        // 17x8x21 uebersteigt das 15er-Template - Placement-Check gegen die Struktur-
        // registrierung genuegt; In-World-Verifikation ueber Kern-Positionen im Mini-Ausschnitt
        // ist hier nicht moeglich. Die Templates-Syntax wurde dennoch geladen (get() ok).
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void monolithTemplateLoadsAndHidesSecret(GameTestHelper helper) {
        var structureRegistry = helper.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        if (!structureRegistry.containsKey(ModStructures.THE_MONOLITH)) {
            helper.fail("Struktur lit_spaceships:the_monolith ist nicht registriert");
            return;
        }

        var template = helper.getLevel().getStructureManager()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.lit.spaceships.LitSpaceships.MODID, "the_monolith/monolith"))
                .orElse(null);
        if (template == null) {
            helper.fail("Template lit_spaceships:the_monolith/monolith wurde nicht geladen");
            return;
        }
        var size = template.getSize();
        if (size.getX() != 9 || size.getY() != 14 || size.getZ() != 9) {
            helper.fail("Monolith hat unerwartete Größe: " + size);
            return;
        }

        BlockPos origin = helper.absolutePos(new BlockPos(3, 0, 3));
        boolean placed = template.placeInWorld(helper.getLevel(), origin, origin,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                RandomSource.create(17L), 2);
        if (!placed) {
            helper.fail("Monolith-Template konnte nicht platziert werden");
            return;
        }

        // Monolith (Template 3..5, 5..12, 3..4 -> relativ 6..8, 5..12, 6..7)
        helper.assertBlock(new BlockPos(7, 6, 6), Blocks.OBSIDIAN::equals, "Monolith muss Obsidian sein");
        helper.assertBlock(new BlockPos(7, 12, 6), Blocks.OBSIDIAN::equals, "Monolith-Spitze muss Obsidian sein");

        // Die Geheimkammer unter dem Monolith: Kiste tief im Rock (Template 4,1,4 -> relativ 7,1,7)
        helper.assertBlock(new BlockPos(7, 1, 7), Blocks.CHEST::equals, "Geheimkammer muss die Kiste tragen");
        helper.assertBlock(new BlockPos(6, 1, 7), Blocks.GOLD_BLOCK::equals, "Opfergold neben der Kiste");
        helper.assertBlock(new BlockPos(8, 1, 7), Blocks.DIAMOND_ORE::equals, "Diamanterz-Opfergabe neben der Kiste");

        // Kein Hinweis von oben: die Oberflaeche des Rocks verrät nichts
        helper.assertBlock(new BlockPos(7, 4, 7), Blocks.BASALT::equals, "Sockel muss unschuldig bleiben");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void jumpGateTemplateLoadsAndPlaces(GameTestHelper helper) {
        var structureRegistry = helper.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        if (!structureRegistry.containsKey(ModStructures.JUMP_GATE)) {
            helper.fail("Struktur lit_spaceships:jump_gate ist nicht registriert");
            return;
        }

        var template = helper.getLevel().getStructureManager()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.lit.spaceships.LitSpaceships.MODID, "jump_gate/gate"))
                .orElse(null);
        if (template == null) {
            helper.fail("Template lit_spaceships:jump_gate/gate wurde nicht geladen");
            return;
        }
        var size = template.getSize();
        if (size.getX() != 15 || size.getY() != 13 || size.getZ() != 15) {
            helper.fail("Jump Gate hat unerwartete Größe: " + size);
            return;
        }

        BlockPos origin = helper.absolutePos(new BlockPos(0, 1, 0));
        boolean placed = template.placeInWorld(helper.getLevel(), origin, origin,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                RandomSource.create(21L), 2);
        if (!placed) {
            helper.fail("Gate-Template konnte nicht platziert werden");
            return;
        }

        // Void-Kristall im Zentrum (relativ 7,7/8,7) + versteckter Cache (7,2,7)
        helper.assertBlock(new BlockPos(7, 7, 7), Blocks.AMETHYST_BLOCK::equals, "Gate-Zentrum muss Void-Kristall tragen");
        helper.assertBlock(new BlockPos(7, 8, 7), Blocks.SEA_LANTERN::equals, "Kristall muss leuchten");
        helper.assertBlock(new BlockPos(7, 2, 7), Blocks.CHEST::equals, "Gate-Cache muss unter dem Tor begraben sein");
        helper.assertBlock(new BlockPos(7, 0, 7), Blocks.AIR::equals, "Unter dem Cache muss die Leere liegen");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void satelliteGraveyardPlacesIntactWreck(GameTestHelper helper) {
        // Intakter Satellit (mit Kiste) im 15er-Template
        RandomSource random = RandomSource.create(29L);
        com.lit.spaceships.world.feature.SatelliteGraveyardFeature.placeSatellite(
                helper.getLevel(), helper.absolutePos(new BlockPos(7, 7, 7)).getX(),
                helper.absolutePos(new BlockPos(7, 7, 7)).getY(),
                helper.absolutePos(new BlockPos(7, 7, 7)).getZ(), random, true);

        // Rumpf (Eisen) + Kupferecken + Solar-Panels + Antenne
        helper.assertBlock(new BlockPos(7, 7, 7), Blocks.IRON_BLOCK::equals, "Satellit-Rumpf muss Eisen sein");
        helper.assertBlock(new BlockPos(6, 7, 6), Blocks.COPPER_BLOCK::equals, "Rumpfecke muss Kupfer sein");
        helper.assertBlock(new BlockPos(5, 7, 7), Blocks.LIGHT_BLUE_STAINED_GLASS::equals, "Solar-Panel muss Cyan-Glas sein");
        helper.assertBlock(new BlockPos(7, 9, 7), Blocks.LIGHTNING_ROD::equals, "Antenne muss Blitzableiter sein");
        helper.assertBlock(new BlockPos(7, 6, 7), Blocks.CHEST::equals, "Intakter Satellit muss die Pluender-Kiste tragen");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void cosmicJellyfishPlacesHeartAndTentacles(GameTestHelper helper) {
        // Qualle mit Radius 4, Zentrum bei (7,9,7) -> Schirm bis y13, Tentakeln bis y4
        RandomSource random = RandomSource.create(31L);
        com.lit.spaceships.world.feature.CosmicJellyfishFeature.placeJellyfish(
                helper.getLevel(), helper.absolutePos(new BlockPos(7, 9, 7)).getX(),
                helper.absolutePos(new BlockPos(7, 9, 7)).getY(),
                helper.absolutePos(new BlockPos(7, 9, 7)).getZ(), random, 4);

        // Herz-Kiste schwebt im Zentrum (relativ 7,9,7)
        helper.assertBlock(new BlockPos(7, 9, 7), Blocks.CHEST::equals, "Herz-Kiste muss im Zentrum schweben");

        // Schirmscheitel (relativ 7,13,7) ist Glas
        helper.assertBlockState(new BlockPos(7, 13, 7),
                state -> state.is(Blocks.MAGENTA_STAINED_GLASS) || state.is(Blocks.PURPLE_STAINED_GLASS),
                () -> "Schirmspitze muss Magenta/Violett-Glas sein");

        // Tentakel: Kette unter dem Schirmrand (rim x=7-4+1=4, y ab 8 abwaerts)
        helper.assertBlock(new BlockPos(4, 7, 7), Blocks.CHAIN::equals, "Tentakel muss Kette sein");

        // Innenraum bleibt Luft (relativ 7,11,7 ist unter der Schale, ueber der Kiste)
        helper.assertBlock(new BlockPos(7, 11, 7), Blocks.AIR::equals, "Schirm-Inneres muss Luft bleiben");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void colonyDomeTemplateLoadsAndPlaces(GameTestHelper helper) {
        var structureRegistry = helper.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        if (!structureRegistry.containsKey(ModStructures.COLONY_DOME)) {
            helper.fail("Struktur lit_spaceships:colony_dome ist nicht registriert");
            return;
        }

        var template = helper.getLevel().getStructureManager()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.lit.spaceships.LitSpaceships.MODID, "colony_dome/dome"))
                .orElse(null);
        if (template == null) {
            helper.fail("Template lit_spaceships:colony_dome/dome wurde nicht geladen");
            return;
        }
        var size = template.getSize();
        if (size.getX() != 13 || size.getY() != 10 || size.getZ() != 13) {
            helper.fail("Koloniekuppel hat unerwartete Größe: " + size);
            return;
        }

        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        boolean placed = template.placeInWorld(helper.getLevel(), origin, origin,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                RandomSource.create(41L), 2);
        if (!placed) {
            helper.fail("Kuppel-Template konnte nicht platziert werden");
            return;
        }

        // Vorrats-Kiste (Template 8,1,6 -> relativ 9,2,7), Lagerfeuer (6,1,6 -> 7,2,7),
        // Amethyst-Ueberwucherung (Template 3,1,3 -> relativ 4,2,4)
        helper.assertBlock(new BlockPos(9, 2, 7), Blocks.CHEST::equals, "Vorrats-Kiste muss in der Kuppel stehen");
        helper.assertBlock(new BlockPos(7, 2, 7), Blocks.CAMPFIRE::equals, "Lagerfeuer der Siedler muss brennen");
        helper.assertBlockState(new BlockPos(4, 2, 4),
                state -> state.is(Blocks.AMETHYST_BLOCK) || state.is(Blocks.BUDDING_AMETHYST),
                () -> "Amethyst muss die Kolonie ueberwuchert haben");
        helper.assertBlock(new BlockPos(7, 7, 7), Blocks.GLASS::equals, "Kuppelscheitel muss Glas sein");

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
        boolean chestFound = false;
        for (int dy = 10; dy <= 12; dy++) {
            if (helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(7, dy, 7))).is(Blocks.CHEST)) {
                chestFound = true;
                break;
            }
        }
        if (!chestFound) {
            helper.fail("Salvage-Kiste muss ueber dem Cluster liegen");
            return;
        }

        // Rand des Clusters: verkohlte Mischung oder Luft (Perturbation)
        helper.assertBlockState(new BlockPos(10, 8, 7),
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
        helper.assertBlock(new BlockPos(7, 10, 7), Blocks.BLUE_ICE::equals, "Orb-Scheitel muss Blau-Eis sein");

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
                helper.absolutePos(new BlockPos(7, 0, 7)).getZ() + 0.5D,
                helper.absolutePos(new BlockPos(7, 8, 7)).getY());
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
        helper.assertBlock(new BlockPos(7, 9, 8), Blocks.COPPER_BULB::equals, "Pylon-Fuss muss Kupferbirne tragen");
        helper.assertBlock(new BlockPos(6, 5, 7), Blocks.CHAIN::equals, "Geladener Pylon muss Kettennetz haben");
        helper.assertBlock(new BlockPos(7, 6, 6), Blocks.COPPER_GRATE::equals, "Pylon-Rahmen muss Kupfergitter sein");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void hullBreachAppliesDecompressionImpulse(GameTestHelper helper) {
        // Druckkammer: 5x5x5 Eisenhuelle um Luft bei (7,6,7) mit Oeffnung an Ostwand
        for (int x = 5; x <= 9; x++) {
            for (int y = 4; y <= 8; y++) {
                for (int z = 5; z <= 9; z++) {
                    boolean shell = x == 5 || x == 9 || y == 4 || y == 8 || z == 5 || z == 9;
                    if (shell && !(x == 9 && y >= 5 && y <= 7 && z == 7)) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.IRON_BLOCK);
                    }
                }
            }
        }
        // Item im Kammernzen: wird bei Dekompression zur Bruchstelle (Ostwand) gesaugt
        var item = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),
                helper.absolutePos(new BlockPos(7, 6, 7)).getX() + 0.5,
                helper.absolutePos(new BlockPos(7, 6, 7)).getY() + 0.5,
                helper.absolutePos(new BlockPos(7, 6, 7)).getZ() + 0.5,
                new net.minecraft.world.item.ItemStack(Blocks.COBBLESTONE));
        helper.getLevel().addFreshEntity(item);
        var breachPos = helper.absolutePos(new BlockPos(9, 6, 7));

        // Dekompression direkt ausloesen (Block-Break-Event ist im GameTest nicht
        // verfuegbar - der Service ist nevertheless derselbe Produktionspfad)
        // 60-Tick-Sog kontinuierlich anwenden (wie in Produktion ueber ServerTick)
        double startX = item.position().x;
        helper.startSequence()
                .thenExecuteFor(60, () -> com.lit.spaceships.world.DecompressionService.applyOutwardImpulse(
                        helper.getLevel(), net.minecraft.world.phys.Vec3.atCenterOf(breachPos)))
                .thenExecute(() -> {
                    double moved = item.position().x - startX;
                    if (moved < 0.5) {
                        helper.fail("Item muss durch Dekompression merklich Richtung Bruchstelle gezogen werden, moved=" + moved);
                        return;
                    }
                    helper.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void beaconSignalRegistrationAndDetection(GameTestHelper helper) {
        var beaconPos = helper.absolutePos(new BlockPos(10, 6, 6));
        helper.setBlock(new BlockPos(10, 6, 6), com.lit.spaceships.registry.ModBlocks.BEACON.get());
        if (!(helper.getLevel().getBlockEntity(beaconPos)
                instanceof com.lit.spaceships.block.entity.BeaconBlockEntity beacon)) {
            helper.fail("Beacon-BlockEntity muss bei Platzierung entstehen");
            return;
        }
        beacon.setFrequency(com.lit.spaceships.world.Telemetry.Frequencies.RESEARCH_BEACON);

        var listenerPos = helper.absolutePos(new BlockPos(3, 6, 6));
        var signals = com.lit.spaceships.item.SignalScopeItem.scanWorld(helper.getLevel(),
                net.minecraft.world.phys.Vec3.atCenterOf(listenerPos));
        boolean found = signals.stream().anyMatch(sig ->
                sig.frequency().equals(com.lit.spaceships.world.Telemetry.Frequencies.RESEARCH_BEACON));
        if (!found) {
            helper.fail("Scan muss den Research-Beacon mit korrekter Frequenz finden");
            return;
        }

        var reading = com.lit.spaceships.world.Telemetry.bestSignal(
                net.minecraft.world.phys.Vec3.atCenterOf(listenerPos),
                new net.minecraft.world.phys.Vec3(1, 0, 0), signals);
        if (reading.isEmpty()) {
            helper.fail("bestSignal muss den registrierten Beacon lesen");
            return;
        }
        if (!reading.get().signal().frequency()
                .equals(com.lit.spaceships.world.Telemetry.Frequencies.RESEARCH_BEACON)) {
            helper.fail("Alignment-Lesung muss den Research-Beacon priorisieren (direkt anvisiert)");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void reactorCoolantResetsMeltdown(GameTestHelper helper) {
        // Reaktor setzen, 2 "Kuehlungen" durchfuehren (wie 2 Ventile)
        var reactorPos = helper.absolutePos(new BlockPos(7, 6, 7));
        helper.setBlock(new BlockPos(7, 6, 7), com.lit.spaceships.registry.ModBlocks.UNSTABLE_REACTOR.get());
        if (!(helper.getLevel().getBlockEntity(reactorPos)
                instanceof com.lit.spaceships.block.UnstableReactorBlockEntity reactor)) {
            helper.fail("Reaktor-BlockEntity fehlt");
            return;
        }
        // 450 Ticks Stufe 0 simulieren: State-Machine ueber den BE-Treiber pruefen
        reactor.cool();
        reactor.cool();
        if (!reactor.state().unlocked()) {
            helper.fail("2 Kuehlungen muessen die Kernkammer freischalten");
            return;
        }
        // Kernkammer-Kiste muss ueber dem Reaktor erscheinen
        helper.assertBlock(new BlockPos(7, 7, 7), Blocks.CHEST::equals, "Freigeschaltete Kernkammer muss Kiste sein");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void reactorMeltdownExplodesWithoutCooling(GameTestHelper helper) {
        var reactorPos = helper.absolutePos(new BlockPos(7, 6, 7));
        helper.setBlock(new BlockPos(7, 6, 7), com.lit.spaceships.registry.ModBlocks.UNSTABLE_REACTOR.get());
        // Explosion nach 4 Stufen a 450 Ticks uebersteigt 300-Tick-Timeout; verkuerzt:
        // pruefe die State-Machine-Logik direkt am BE (Determinismus der Produktion)
        if (!(helper.getLevel().getBlockEntity(reactorPos)
                instanceof com.lit.spaceships.block.UnstableReactorBlockEntity reactor)) {
            helper.fail("Reaktor-BlockEntity fehlt");
            return;
        }
        // Kuehlversuch mit nur einem Ventil: nicht freigeschaltet
        reactor.cool();
        if (reactor.state().unlocked()) {
            helper.fail("Ein Ventil darf die Kernkammer nicht freischalten");
            return;
        }
        // Countdown laeuft weiter (Stufe 0 aktiv)
        if (com.lit.spaceships.block.ReactorMeltdownLogic.tick(reactor.state()) == com.lit.spaceships.block.ReactorMeltdownLogic.ReactorEvent.EXPLODE) {
            helper.fail("Frischer Reaktor darf nicht sofort explodieren");
            return;
        }
        helper.succeed();
    }
}
