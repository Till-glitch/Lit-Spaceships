package com.lit.spaceships.world;

import com.lit.spaceships.LitSpaceships;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.Map;

/**
 * Bootstrap für die Jigsaw-Struktur der Abandoned Orbital Research Station
 * ({@code lit_spaceships:space_station}) samt {@code structure_set}.
 *
 * <p>Die Station schwebt dank {@link UniformHeight}-Starthöhe (Y 48-192)
 * vollständig in der Void-Dimension — keine Terrain-Abhängigkeit. Verteilt
 * via Random-Spread (Spacing 36, Separation 12) über alle vier Weltraum-Biome.</p>
 */
public final class ModStructures {

    public static final ResourceKey<Structure> SPACE_STATION =
            ResourceKey.create(Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "space_station"));

    public static final ResourceKey<StructureSet> SPACE_STATION_SET =
            ResourceKey.create(Registries.STRUCTURE_SET,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "space_station"));

    public static final ResourceKey<Structure> DREADNOUGHT_WRECK =
            ResourceKey.create(Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "dreadnought_wreck"));

    public static final ResourceKey<StructureSet> DREADNOUGHT_WRECK_SET =
            ResourceKey.create(Registries.STRUCTURE_SET,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "dreadnought_wreck"));

    public static final ResourceKey<Structure> ALIEN_OUTPOST =
            ResourceKey.create(Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "alien_outpost"));

    public static final ResourceKey<StructureSet> ALIEN_OUTPOST_SET =
            ResourceKey.create(Registries.STRUCTURE_SET,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "alien_outpost"));

    public static final ResourceKey<Structure> COSMIC_VAULT =
            ResourceKey.create(Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "cosmic_vault"));

    public static final ResourceKey<StructureSet> COSMIC_VAULT_SET =
            ResourceKey.create(Registries.STRUCTURE_SET,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "cosmic_vault"));

    public static final ResourceKey<Structure> PIRATE_OUTPOST =
            ResourceKey.create(Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "pirate_outpost"));

    public static final ResourceKey<StructureSet> PIRATE_OUTPOST_SET =
            ResourceKey.create(Registries.STRUCTURE_SET,
                    ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "pirate_outpost"));

    private ModStructures() {
    }

    public static void bootstrapStructure(BootstrapContext<Structure> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);

        // 1. Abandoned Orbital Research Station
        Holder<StructureTemplatePool> stationStartPool =
                pools.getOrThrow(ModTemplatePools.SPACE_STATION_START);

        Structure.StructureSettings stationSettings = new Structure.StructureSettings(
                HolderSet.direct(
                        biomes.getOrThrow(ModDimensions.SPACE_BIOME),
                        biomes.getOrThrow(ModBiomes.PLASMA_NEBULA),
                        biomes.getOrThrow(ModBiomes.FROZEN_EXPANSE),
                        biomes.getOrThrow(ModBiomes.VOID_WASTES)),
                Map.of(),
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                TerrainAdjustment.NONE);

        context.register(SPACE_STATION, new JigsawStructure(
                stationSettings,
                stationStartPool,
                3,
                UniformHeight.of(VerticalAnchor.absolute(48), VerticalAnchor.absolute(192)),
                false));

        // 2. Derelict Dreadnought Warship (in Space Biome und Void Wastes Graveyard)
        Holder<StructureTemplatePool> dreadnoughtStartPool =
                pools.getOrThrow(ModTemplatePools.DREADNOUGHT_WRECK_START);

        Structure.StructureSettings dreadnoughtSettings = new Structure.StructureSettings(
                HolderSet.direct(
                        biomes.getOrThrow(ModDimensions.SPACE_BIOME),
                        biomes.getOrThrow(ModBiomes.VOID_WASTES)),
                Map.of(),
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                TerrainAdjustment.NONE);

        context.register(DREADNOUGHT_WRECK, new JigsawStructure(
                dreadnoughtSettings,
                dreadnoughtStartPool,
                3,
                UniformHeight.of(VerticalAnchor.absolute(32), VerticalAnchor.absolute(160)),
                false));

        // 3. Alien Monolith Relic (in Space Biome und mystischen Plasma Nebulae)
        Holder<StructureTemplatePool> alienStartPool =
                pools.getOrThrow(ModTemplatePools.ALIEN_OUTPOST_START);

        Structure.StructureSettings alienSettings = new Structure.StructureSettings(
                HolderSet.direct(
                        biomes.getOrThrow(ModDimensions.SPACE_BIOME),
                        biomes.getOrThrow(ModBiomes.PLASMA_NEBULA)),
                Map.of(),
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                TerrainAdjustment.NONE);

        context.register(ALIEN_OUTPOST, new JigsawStructure(
                alienSettings,
                alienStartPool,
                1,
                UniformHeight.of(VerticalAnchor.absolute(64), VerticalAnchor.absolute(200)),
                false));

        // 4. Cosmic Vault — seltenste Struktur des Void (alle 4 Biome, RandomSpread 64/20)
        Holder<StructureTemplatePool> vaultStartPool =
                pools.getOrThrow(ModTemplatePools.COSMIC_VAULT_START);

        Structure.StructureSettings vaultSettings = new Structure.StructureSettings(
                HolderSet.direct(
                        biomes.getOrThrow(ModDimensions.SPACE_BIOME),
                        biomes.getOrThrow(ModBiomes.PLASMA_NEBULA),
                        biomes.getOrThrow(ModBiomes.FROZEN_EXPANSE),
                        biomes.getOrThrow(ModBiomes.VOID_WASTES)),
                Map.of(),
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                TerrainAdjustment.NONE);

        context.register(COSMIC_VAULT, new JigsawStructure(
                vaultSettings,
                vaultStartPool,
                1,
                UniformHeight.of(VerticalAnchor.absolute(64), VerticalAnchor.absolute(200)),
                false));

        // 5. Pirate Satellite Outpost (Space Biome & Void Wastes - Raeuber pluendern Wracks)
        Holder<StructureTemplatePool> pirateStartPool =
                pools.getOrThrow(ModTemplatePools.PIRATE_OUTPOST_START);

        Structure.StructureSettings pirateSettings = new Structure.StructureSettings(
                HolderSet.direct(
                        biomes.getOrThrow(ModDimensions.SPACE_BIOME),
                        biomes.getOrThrow(ModBiomes.VOID_WASTES)),
                Map.of(),
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                TerrainAdjustment.NONE);

        context.register(PIRATE_OUTPOST, new JigsawStructure(
                pirateSettings,
                pirateStartPool,
                1,
                UniformHeight.of(VerticalAnchor.absolute(48), VerticalAnchor.absolute(184)),
                false));
    }

    public static void bootstrapStructureSet(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);

        Holder<Structure> station = structures.getOrThrow(SPACE_STATION);
        context.register(SPACE_STATION_SET, new StructureSet(station,
                new RandomSpreadStructurePlacement(36, 12, RandomSpreadType.LINEAR, 1842089401)));

        Holder<Structure> dreadnought = structures.getOrThrow(DREADNOUGHT_WRECK);
        context.register(DREADNOUGHT_WRECK_SET, new StructureSet(dreadnought,
                new RandomSpreadStructurePlacement(48, 16, RandomSpreadType.LINEAR, 1948102941)));

        Holder<Structure> alien = structures.getOrThrow(ALIEN_OUTPOST);
        context.register(ALIEN_OUTPOST_SET, new StructureSet(alien,
                new RandomSpreadStructurePlacement(40, 14, RandomSpreadType.LINEAR, 1739281743)));

        Holder<Structure> vault = structures.getOrThrow(COSMIC_VAULT);
        context.register(COSMIC_VAULT_SET, new StructureSet(vault,
                new RandomSpreadStructurePlacement(64, 20, RandomSpreadType.LINEAR, 2095820113)));

        Holder<Structure> pirate = structures.getOrThrow(PIRATE_OUTPOST);
        context.register(PIRATE_OUTPOST_SET, new StructureSet(pirate,
                new RandomSpreadStructurePlacement(44, 16, RandomSpreadType.LINEAR, 1666420707)));
    }
}
