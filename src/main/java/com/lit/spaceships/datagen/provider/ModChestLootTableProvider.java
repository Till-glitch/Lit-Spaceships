package com.lit.spaceships.datagen.provider;

import com.lit.spaceships.LitSpaceships;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

/**
 * DataGen-SubProvider für alle Kisten-Loot-Tables des Weltraums:
 * Abandoned Orbital Research Station, Derelict Dreadnought Warship und Alien Monolith Relic.
 */
public class ModChestLootTableProvider implements LootTableSubProvider {

    public static final ResourceKey<LootTable> SPACE_STATION_CORE = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/space_station_core"));

    public static final ResourceKey<LootTable> DREADNOUGHT_ARMORY = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/dreadnought_armory"));

    public static final ResourceKey<LootTable> ALIEN_MONOLITH = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/alien_monolith"));

    public static final ResourceKey<LootTable> COSMIC_VAULT = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/cosmic_vault"));

    public static final ResourceKey<LootTable> PIRATE_CACHE = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/pirate_cache"));

    public static final ResourceKey<LootTable> LEVIATHAN_HOARD = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/leviathan_hoard"));

    public static final ResourceKey<LootTable> MONOLITH_SECRET = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/monolith_secret"));

    public static final ResourceKey<LootTable> GATE_CACHE = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "chests/gate_cache"));

    private final HolderLookup.Provider registries;

    public ModChestLootTableProvider(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> consumer) {
        // Space Station Core: Waffen-Smithing-Templates, Hochkapazitäts-Energiekomponenten, Diamanten
        consumer.accept(SPACE_STATION_CORE, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(LootItem.lootTableItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).setWeight(3))
                        .add(LootItem.lootTableItem(Items.DIAMOND_BLOCK).setWeight(2))
                        .add(LootItem.lootTableItem(Items.REDSTONE_BLOCK).setWeight(5)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).setWeight(1)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(3.0F, 5.0F))
                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                        .add(LootItem.lootTableItem(Items.COPPER_INGOT).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
                        .add(LootItem.lootTableItem(Items.REDSTONE).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 12.0F))))
                        .add(LootItem.lootTableItem(Items.QUARTZ).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 6.0F))))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))));

        // Dreadnought Armory: Netherite Scrap/Ingot, Pulslaser-Komponenten (Echo Shards), Schwere Strahllinsen (Blaze Rods/Quarz)
        consumer.accept(DREADNOUGHT_ARMORY, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(LootItem.lootTableItem(Items.NETHERITE_SCRAP).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.NETHERITE_INGOT).setWeight(1))
                        .add(LootItem.lootTableItem(Items.ECHO_SHARD).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .add(LootItem.lootTableItem(Items.BLAZE_ROD).setWeight(6)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(5)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(3.0F, 6.0F))
                        .add(LootItem.lootTableItem(Items.IRON_BLOCK).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .add(LootItem.lootTableItem(Items.GOLD_BLOCK).setWeight(3)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .add(LootItem.lootTableItem(Items.TNT).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                        .add(LootItem.lootTableItem(Items.FIRE_CHARGE).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 12.0F))))));

        // Alien Monolith: Lodestone, Nether-Stern, Dimensionsartefakte (Enderperlen, Amethyst, Crying Obsidian)
        consumer.accept(ALIEN_MONOLITH, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(LootItem.lootTableItem(Items.LODESTONE).setWeight(4))
                        .add(LootItem.lootTableItem(Items.NETHER_STAR).setWeight(1))
                        .add(LootItem.lootTableItem(Items.ECHO_SHARD).setWeight(5)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.ENDER_EYE).setWeight(6)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(3.0F, 5.0F))
                        .add(LootItem.lootTableItem(Items.ENDER_PEARL).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                        .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 10.0F))))
                        .add(LootItem.lootTableItem(Items.CRYING_OBSIDIAN).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
                        .add(LootItem.lootTableItem(Items.CHORUS_FRUIT).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F))))));

        // Cosmic Vault: Endgame-Beute im versiegelten Obsidian-Gewoelb (seltenste Struktur)
        consumer.accept(COSMIC_VAULT, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 1.0F))
                        .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).setWeight(2))
                        .add(LootItem.lootTableItem(Items.NETHERITE_INGOT).setWeight(1))
                        .add(LootItem.lootTableItem(Items.END_CRYSTAL).setWeight(1))
                        .add(LootItem.lootTableItem(Items.TOTEM_OF_UNDYING).setWeight(1))
                        .add(LootItem.lootTableItem(Items.DIAMOND_BLOCK).setWeight(3)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 4.0F))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(3)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.EMERALD).setWeight(5)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                        .add(LootItem.lootTableItem(Items.ENDER_PEARL).setWeight(5)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(6)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))));

        // Pirate Cache: Waffen, Gluecksgut und ein Hauch Chaos (Kiste sitzt auf TNT!)
        consumer.accept(PIRATE_CACHE, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(LootItem.lootTableItem(Items.CROSSBOW).setWeight(3))
                        .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(3))
                        .add(LootItem.lootTableItem(Items.EMERALD).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(3.0F, 6.0F))
                        .add(LootItem.lootTableItem(Items.ARROW).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(6.0F, 16.0F))))
                        .add(LootItem.lootTableItem(Items.GUNPOWDER).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 12.0F))))
                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(6)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.OBSIDIAN).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))));

        // Leviathan Hoard: Herzensgabe der Bestie - Herz des Meeres, Diamanten, Eis-Schaetze
        consumer.accept(LEVIATHAN_HOARD, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 1.0F))
                        .add(LootItem.lootTableItem(Items.HEART_OF_THE_SEA).setWeight(2))
                        .add(LootItem.lootTableItem(Items.DIAMOND_BLOCK).setWeight(3)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 4.0F))
                        .add(LootItem.lootTableItem(Items.BLUE_ICE).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                        .add(LootItem.lootTableItem(Items.PRISMARINE_CRYSTALS).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.EMERALD).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.GOLD_BLOCK).setWeight(2))));

        // Monolith Secret: Belohnung fuers Graben - Netherit-Splitter, Echo-Shards, Endaugen
        consumer.accept(MONOLITH_SECRET, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(LootItem.lootTableItem(Items.NETHERITE_SCRAP).setWeight(3)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).setWeight(1))
                        .add(LootItem.lootTableItem(Items.NAME_TAG).setWeight(2))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 4.0F))
                        .add(LootItem.lootTableItem(Items.ENDER_EYE).setWeight(6)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.ECHO_SHARD).setWeight(6)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .add(LootItem.lootTableItem(Items.OBSIDIAN).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.EMERALD).setWeight(6)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))));

        // Gate Cache: Warp-Essenz - Enderperlen, Endaugen, Chorus, Amethyst
        consumer.accept(GATE_CACHE, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 4.0F))
                        .add(LootItem.lootTableItem(Items.ENDER_PEARL).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                        .add(LootItem.lootTableItem(Items.ENDER_EYE).setWeight(5)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.CHORUS_FRUIT).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 7.0F))))
                        .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 9.0F))))
                        .add(LootItem.lootTableItem(Items.PURPUR_BLOCK).setWeight(4)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0.0F, 1.0F))
                        .add(LootItem.lootTableItem(Items.END_CRYSTAL).setWeight(1))
                        .add(LootItem.lootTableItem(Items.DIAMOND_BLOCK).setWeight(2))));
    }
}
