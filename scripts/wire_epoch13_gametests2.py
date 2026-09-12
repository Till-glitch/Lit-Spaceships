# -*- coding: utf-8 -*-
"""Epoch 13: finale GameTests - haengt an der Datei-Tail (reactorMeltdownExplodes...)."""

p = 'src/main/java/com/lit/spaceships/tests/WorldGenGameTests.java'
s = open(p, encoding='utf-8').read()
if 'extremeBiomesRegisteredWithSignatureVisuals' in s:
    print('already applied')
else:
    anchor = '''        helper.succeed();
    }
}'''
    # Der letzte Test ist reactorMeltdownExplodesWithoutCooling — der Anker ist die Datei-Ende
    idx = s.rfind(anchor)
    assert idx > 0, 'tail anchor missing'
    new = '''        helper.succeed();
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
        if (rift.value().getFogColor() != 0x0B001A || corona.value().getFogColor() != 0xFF4500
                || storm.value().getFogColor() != 0x1E90FF) {
            helper.fail("Extreme-Biome haben falsche Signature-Farben");
            return;
        }
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
        // Redstone-Block an der Seite -> neighborChanged oeffnet die Kiste
        helper.setBlock(new BlockPos(6, 6, 7), Blocks.REDSTONE_BLOCK);
        // Beute muss ausgeschuettet werden (Items in der Naehe)
        helper.startSequence().thenExecuteAfter(5, () -> {
            var items = helper.getLevel().getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class,
                    net.minecraft.world.phys.AABB.ofSize(
                            net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(cratePos)),
                            6, 6, 6));
            if (items.isEmpty()) {
                helper.fail("Redstone-geoeffnete Kiste muss Fracht auswerfen");
                return;
            }
            helper.succeed();
        }).start();
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
    s = s[:idx] + new + s[idx + len(anchor):]
    open(p, 'w', encoding='utf-8').write(s)
    print('final GameTests added')
