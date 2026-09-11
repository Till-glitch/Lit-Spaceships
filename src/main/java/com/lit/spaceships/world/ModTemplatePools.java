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
    }

    private static ResourceKey<StructureTemplatePool> createKey(String name) {
        return ResourceKey.create(Registries.TEMPLATE_POOL,
                ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, name));
    }
}
