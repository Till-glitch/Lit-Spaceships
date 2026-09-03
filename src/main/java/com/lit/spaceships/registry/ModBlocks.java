package com.lit.spaceships.registry;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.block.SpaceshipControlBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    // 1. Das Register für Blöcke erstellen
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LitSpaceships.MODID);

    // 2. Deine Blöcke eintragen
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));

    public static final DeferredBlock<Block> SPACESHIP_CONTROL = BLOCKS.register("spaceship_control",
            () -> new SpaceshipControlBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f)));

    public static final DeferredBlock<Block> SPACESHIP_HELM = BLOCKS.register("spaceship_helm",
            () -> new com.lit.spaceships.block.SpaceshipHelmBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0f)));

    public static final DeferredBlock<Block> SPACESHIP_REACTOR = BLOCKS.register("spaceship_reactor",
            () -> new com.lit.spaceships.block.SpaceshipReactorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(4.0f)));

    public static final DeferredBlock<Block> SPACESHIP_SHIELD = BLOCKS.register("spaceship_shield",
            () -> new com.lit.spaceships.block.SpaceshipShieldBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(4.0f)));

    public static final DeferredBlock<Block> PULSE_LASER = BLOCKS.register("pulse_laser",
            () -> new com.lit.spaceships.block.PulseLaserBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.5f).noOcclusion()));

    public static final DeferredBlock<Block> HEAVY_BEAM = BLOCKS.register("heavy_beam",
            () -> new com.lit.spaceships.block.HeavyBeamBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(4.0f).noOcclusion()));

    public static final DeferredBlock<Block> MINING_LASER = BLOCKS.register("mining_laser",
            () -> new com.lit.spaceships.block.MiningLaserBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(3.0f).noOcclusion()));

    // 3. Diese Methode ruft unsere Hauptklasse später auf
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}