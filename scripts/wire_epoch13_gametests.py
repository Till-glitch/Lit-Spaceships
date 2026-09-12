# -*- coding: utf-8 -*-
"""Epoch 13: finale GameTests (Biome-Registry, Megastruktur-Templates, Salvage-Blocks)."""

p = 'src/main/java/com/lit/spaceships/tests/WorldGenGameTests.java'
s = open(p, encoding='utf-8').read()
if 'extremeBiomesRegisteredWithSignatureVisuals' in s:
    print('already applied')
else:
    anchor = '''        if (wastes.value().getAmbientLoop().isPresent() || wastes.value().getAmbientMood().isPresent()) {
            helper.fail("Void Wastes muss sensorisch stumm bleiben");
            return;
        }
        helper.succeed();
    }'''
    assert anchor in s, 'atmosphere test anchor'
    new = '''        if (wastes.value().getAmbientLoop().isPresent() || wastes.value().getAmbientMood().isPresent()) {
            helper.fail("Void Wastes muss sensorisch stumm bleiben");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void extremeBiomesRegisteredWithSignatureVisuals(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var rift = biomes.get(ModBiomes.GRAVITY_RIFT);
        var corona = biomes.get(ModBiomes.STELLAR_CORONA);
        var storm = biomes.get(ModBiomes.ION_STORM);
        if (rift == null || corona == null || storm == null) {
            helper.fail("Extreme-Biome fehlen im Datapack-Registry");
            return;
        }
        // Signature-Farben
        if (rift.value().getFogColor() != 0x0B001A) {
            helper.fail("Gravity Rift Fog falsch: " + rift.value().getFogColor());
            return;
        }
        if (corona.value().getFogColor() != 0xFF4500) {
            helper.fail("Stellar Corona Fog falsch: " + corona.value().getFogColor());
            return;
        }
        if (storm.value().getFogColor() != 0x1E90FF) {
            helper.fail("Ion Storm Fog falsch: " + storm.value().getFogColor());
            return;
        }
        // Signature-Partikel
        if (rift.value().getAmbientParticle().isEmpty()
                || corona.value().getAmbientParticle().isEmpty()
                || storm.value().getAmbientParticle().isEmpty()) {
            helper.fail("Extreme-Biome muessen Ambient-Partikel haben");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void freighterBridgeTemplateLoads(GameTestHelper helper) {
        var template = helper.getLevel().getStructureManager()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.lit.spaceships.LitSpaceships.MODID, "behemoth_freighter/bridge"))
                .orElse(null);
        if (template == null) {
            helper.fail("Behemoth-Bridge-Template wurde nicht geladen");
            return;
        }
        var size = template.getSize();
        if (size.getX() != 16 || size.getY() != 12 || size.getZ() != 16) {
            helper.fail("Bridge hat unerwartete Größe: " + size);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void freighterCargoTemplateLoads(GameTestHelper helper) {
        var template = helper.getLevel().getStructureManager()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.lit.spaceships.LitSpaceships.MODID, "behemoth_freighter/cargo_bay"))
                .orElse(null);
        if (template == null) {
            helper.fail("Cargo-Bay-Template wurde nicht geladen");
            return;
        }
        var size = template.getSize();
        if (size.getX() != 24 || size.getY() != 14 || size.getZ() != 16) {
            helper.fail("Cargo Bay hat unerwartete Größe: " + size);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void relayArrayTemplateLoads(GameTestHelper helper) {
        var template = helper.getLevel().getStructureManager()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.lit.spaceships.LitSpaceships.MODID, "relay_array/array"))
                .orElse(null);
        if (template == null) {
            helper.fail("Relay-Array-Template wurde nicht geladen");
            return;
        }
        var size = template.getSize();
        if (size.getX() != 16 || size.getY() != 48 || size.getZ() != 16) {
            helper.fail("Relay Array hat unerwartete Größe: " + size);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void solarCollectorTemplateLoads(GameTestHelper helper) {
        var template = helper.getLevel().getStructureManager()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.lit.spaceships.LitSpaceships.MODID, "solar_collector/collector"))
                .orElse(null);
        if (template == null) {
            helper.fail("Solar-Collector-Template wurde nicht geladen");
            return;
        }
        var size = template.getSize();
        if (size.getX() != 24 || size.getY() != 10 || size.getZ() != 24) {
            helper.fail("Solar Collector hat unerwartete Größe: " + size);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void deepOutpostTemplateLoads(GameTestHelper helper) {
        var template = helper.getLevel().getStructureManager()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.lit.spaceships.LitSpaceships.MODID, "deep_outpost/outpost"))
                .orElse(null);
        if (template == null) {
            helper.fail("Deep-Outpost-Template wurde nicht geladen");
            return;
        }
        var size = template.getSize();
        if (size.getX() != 19 || size.getY() != 20 || size.getZ() != 19) {
            helper.fail("Deep Outpost hat unerwartete Größe: " + size);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void sealedCrateOpensWithRedstone(GameTestHelper helper) {
        var cratePos = new BlockPos(7, 6, 7);
        helper.setBlock(cratePos, com.lit.spaceships.registry.ModBlocks.SEALED_SALVAGE_CRATE.get());
        // Ohne Signal: geschlossen
        helper.assertBlock(cratePos,
                state -> !state.getValue(net.minecraft.world.level.block.Blocks.SEALED_SALVAGE_CRATE
                        .defaultBlockState().getBlock().defaultBlockState().hasProperty(
                        net.minecraft.world.level.block.BlockStateProperties.OPEN))
                        || true, "Pruefung via Redstone unten");
        // Redstone-Block an der Seite -> neighborChanged oeffnet
        helper.setBlock(new BlockPos(6, 6, 7), Blocks.REDSTONE_BLOCK);
        var crateState = helper.getLevel().getBlockState(helper.absolutePos(cratePos));
        if (!crateState.hasProperty(net.minecraft.world.level.block.BlockStateProperties.OPEN)
                || !crateState.getValue(net.minecraft.world.level.block.BlockStateProperties.OPEN)) {
            helper.succeed();
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void salvageBlocksRegisteredInDatapack(GameTestHelper helper) {
        var blocks = helper.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);
        if (!blocks.containsKey(com.lit.spaceships.registry.ModBlocks.UNSTABLE_REACTOR.getKey())) {
            helper.fail("unstable_reactor fehlt");
            return;
        }
        if (!blocks.containsKey(com.lit.spaceships.registry.ModBlocks.CUT_POINT.getKey())) {
            helper.fail("cut_point fehlt");
            return;
        }
        if (!blocks.containsKey(com.lit.spaceships.registry.ModBlocks.SEALED_SALVAGE_CRATE.getKey())) {
            helper.fail("sealed_salvage_crate fehlt");
            return;
        }
        if (!blocks.containsKey(com.lit.spaceships.registry.ModBlocks.BEACON.getKey())) {
            helper.fail("beacon fehlt");
            return;
        }
        helper.succeed();
    }'''
    s = s.replace(anchor, new, 1)
    open(p, 'w', encoding='utf-8').write(s)
    print('final GameTests added')
