package com.peaceman.alpha.registry;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.item.BackflipToolItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Alpha.MODID);

    // Block-Items
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block",
            ModBlocks.EXAMPLE_BLOCK);
    public static final DeferredItem<BlockItem> SPACESHIP_CONTROL_ITEM = ITEMS
            .registerSimpleBlockItem("spaceship_control", ModBlocks.SPACESHIP_CONTROL);
    public static final DeferredItem<BlockItem> SPACESHIP_HELM_ITEM = ITEMS.registerSimpleBlockItem("spaceship_helm",
            ModBlocks.SPACESHIP_HELM);
    public static final DeferredItem<BlockItem> SPACESHIP_REACTOR_ITEM = ITEMS.registerSimpleBlockItem("spaceship_reactor",
            ModBlocks.SPACESHIP_REACTOR);
    public static final DeferredItem<BlockItem> SPACESHIP_SHIELD_ITEM = ITEMS.registerSimpleBlockItem("spaceship_shield",
            ModBlocks.SPACESHIP_SHIELD);
    public static final DeferredItem<BlockItem> PULSE_LASER_ITEM = ITEMS.registerSimpleBlockItem("pulse_laser",
            ModBlocks.PULSE_LASER);
    public static final DeferredItem<BlockItem> HEAVY_BEAM_ITEM = ITEMS.registerSimpleBlockItem("heavy_beam",
            ModBlocks.HEAVY_BEAM);
    public static final DeferredItem<BlockItem> MINING_LASER_ITEM = ITEMS.registerSimpleBlockItem("mining_laser",
            ModBlocks.MINING_LASER);

    // Items
    public static final DeferredItem<Item> BACKFLIP_TOOL = ITEMS.register("backflip_tool",
            () -> new BackflipToolItem(new Item.Properties()
                    .durability(250)
                    .attributes(BackflipToolItem.createAttributes())));

    public static final DeferredItem<Item> SPACE_SUIT_HELMET = ITEMS.register("space_suit_helmet",
            () -> new com.peaceman.alpha.item.SpaceSuitItem(ModArmorMaterials.SPACE_SUIT, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().durability( net.minecraft.world.item.ArmorItem.Type.HELMET.getDurability(15))));

    public static final DeferredItem<Item> SPACE_SUIT_CHESTPLATE = ITEMS.register("space_suit_chestplate",
            () -> new com.peaceman.alpha.item.SpaceSuitItem(ModArmorMaterials.SPACE_SUIT, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE.getDurability(15))));

    public static final DeferredItem<Item> SPACE_SUIT_LEGGINGS = ITEMS.register("space_suit_leggings",
            () -> new com.peaceman.alpha.item.SpaceSuitItem(ModArmorMaterials.SPACE_SUIT, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().durability(net.minecraft.world.item.ArmorItem.Type.LEGGINGS.getDurability(15))));

    public static final DeferredItem<Item> SPACE_SUIT_BOOTS = ITEMS.register("space_suit_boots",
            () -> new com.peaceman.alpha.item.SpaceSuitItem(ModArmorMaterials.SPACE_SUIT, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().durability(net.minecraft.world.item.ArmorItem.Type.BOOTS.getDurability(15))));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
