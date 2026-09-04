package com.lit.spaceships.registry;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.item.BackflipToolItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LitSpaceships.MODID);

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
    public static final DeferredItem<BlockItem> WARP_ENGINE_ITEM = ITEMS.registerSimpleBlockItem("warp_engine",
            ModBlocks.WARP_ENGINE);

    // Items
    public static final DeferredItem<Item> BACKFLIP_TOOL = ITEMS.register("backflip_tool",
            () -> new BackflipToolItem(new Item.Properties()
                    .durability(250)
                    .attributes(BackflipToolItem.createAttributes())));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
