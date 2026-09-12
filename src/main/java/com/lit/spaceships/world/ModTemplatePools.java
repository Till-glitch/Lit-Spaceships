package com.lit.spaceships.world;

import com.lit.spaceships.LitSpaceships;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.List;

/**
 * Bootstrap für die {@code template_pool}s der Weltraum-Strukturen.
 * Die Abandoned Orbital Research Station besteht aus einem Start-Hub
 * (Docking-Hub mit zwei Anschlussstellen) und einem Raum-Pool (Solarflügel,
 * Labor, Reaktorraum), der an die Hub-Anschlüsse wächst.
 */
public final class ModTemplatePools {

    public static final ResourceKey<StructureTemplatePool> SPACE_STATION_START =
            createKey("space_station/start");
    public static final ResourceKey<StructureTemplatePool> SPACE_STATION_ROOMS =
            createKey("space_station/rooms");
    public static final ResourceKey<StructureTemplatePool> DREADNOUGHT_WRECK_START =
            createKey("dreadnought_wreck/start");
    public static final ResourceKey<StructureTemplatePool> DREADNOUGHT_WRECK_SECTIONS =
            createKey("dreadnought_wreck/sections");
    public static final ResourceKey<StructureTemplatePool> ALIEN_OUTPOST_START =
            createKey("alien_outpost/start");
    public static final ResourceKey<StructureTemplatePool> COSMIC_VAULT_START =
            createKey("cosmic_vault/start");
    public static final ResourceKey<StructureTemplatePool> PIRATE_OUTPOST_START =
            createKey("pirate_outpost/start");
    public static final ResourceKey<StructureTemplatePool> LEVIATHAN_BONES_START =
            createKey("leviathan_bones/start");
    public static final ResourceKey<StructureTemplatePool> THE_MONOLITH_START =
            createKey("the_monolith/start");
    public static final ResourceKey<StructureTemplatePool> JUMP_GATE_START =
            createKey("jump_gate/start");
    public static final ResourceKey<StructureTemplatePool> COLONY_DOME_START =
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
            createKey("deep_outpost/start");

    private ModTemplatePools() {
    }

    public static void bootstrap(BootstrapContext<StructureTemplatePool> context) {
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);

        // Abandoned Orbital Research Station
        context.register(SPACE_STATION_START, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:space_station/docking_hub"), 1)
        ), StructureTemplatePool.Projection.RIGID));

        context.register(SPACE_STATION_ROOMS, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:space_station/solar_wing"), 3),
                Pair.of(StructurePoolElement.single("lit_spaceships:space_station/laboratory"), 3),
                Pair.of(StructurePoolElement.single("lit_spaceships:space_station/reactor_room"), 2),
                Pair.of(StructurePoolElement.empty(), 1)
        ), StructureTemplatePool.Projection.RIGID));

        // Derelict Dreadnought Warship
        context.register(DREADNOUGHT_WRECK_START, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:dreadnought_wreck/command_bridge"), 1)
        ), StructureTemplatePool.Projection.RIGID));

        context.register(DREADNOUGHT_WRECK_SECTIONS, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:dreadnought_wreck/corridor_breached"), 3),
                Pair.of(StructurePoolElement.single("lit_spaceships:dreadnought_wreck/engineering_core"), 2),
                Pair.of(StructurePoolElement.empty(), 1)
        ), StructureTemplatePool.Projection.RIGID));

        // Alien Monolith Relic
        context.register(ALIEN_OUTPOST_START, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:alien_outpost/monolith"), 1)
        ), StructureTemplatePool.Projection.RIGID));

        // Cosmic Vault (seltenste Struktur des Void)
        context.register(COSMIC_VAULT_START, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:cosmic_vault/vault"), 1)
        ), StructureTemplatePool.Projection.RIGID));

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
        ), StructureTemplatePool.Projection.RIGID));

        // Pirate Satellite Outpost (mit Fallen-Kiste)
        context.register(PIRATE_OUTPOST_START, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:pirate_outpost/platform"), 1)
        ), StructureTemplatePool.Projection.RIGID));

        // Frozen Leviathan (Kolossales Skelett im Frozen Expanse)
        context.register(LEVIATHAN_BONES_START, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:leviathan_bones/skeleton"), 1)
        ), StructureTemplatePool.Projection.RIGID));

        // The Silent Monolith (Geheimkammer im treibenden Void-Rock)
        context.register(THE_MONOLITH_START, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:the_monolith/monolith"), 1)
        ), StructureTemplatePool.Projection.RIGID));

        // Jump Gate Ruins (zerbrochenes Ringtor mit Void-Kristall)
        context.register(JUMP_GATE_START, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:jump_gate/gate"), 1)
        ), StructureTemplatePool.Projection.RIGID));

        // Abandoned Colony Dome (ueberwucherte Siedlerkuppel)
        context.register(COLONY_DOME_START, new StructureTemplatePool(empty, List.of(
                Pair.of(StructurePoolElement.single("lit_spaceships:colony_dome/dome"), 1)
        ), StructureTemplatePool.Projection.RIGID));
    }

    private static ResourceKey<StructureTemplatePool> createKey(String name) {
        return ResourceKey.create(Registries.TEMPLATE_POOL,
                ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, name));
    }
}
