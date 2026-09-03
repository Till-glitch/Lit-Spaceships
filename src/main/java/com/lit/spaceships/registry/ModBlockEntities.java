package com.lit.spaceships.registry;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.block.entity.SpaceshipControlBlockEntity;
import com.lit.spaceships.block.entity.SpaceshipHelmBlockEntity;
import com.lit.spaceships.block.entity.SpaceshipReactorBlockEntity;
import com.lit.spaceships.block.entity.SpaceshipShieldBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    // 1. Das Register für BlockEntities
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, LitSpaceships.MODID);

    // 2. Unsere BlockEntity eintragen und mit unserem Raumschiff-Block verknüpfen!
    public static final Supplier<BlockEntityType<SpaceshipControlBlockEntity>> SPACESHIP_CONTROL_BE =
            BLOCK_ENTITIES.register("spaceship_control_be", () ->
                    BlockEntityType.Builder.of(SpaceshipControlBlockEntity::new, ModBlocks.SPACESHIP_CONTROL.get()).build(null));

    public static final Supplier<BlockEntityType<SpaceshipHelmBlockEntity>> SPACESHIP_HELM_BE =
            BLOCK_ENTITIES.register("spaceship_helm_be", () ->
                    BlockEntityType.Builder.of(SpaceshipHelmBlockEntity::new, ModBlocks.SPACESHIP_HELM.get()).build(null));

    public static final Supplier<BlockEntityType<SpaceshipReactorBlockEntity>> SPACESHIP_REACTOR_BE =
            BLOCK_ENTITIES.register("spaceship_reactor_be", () ->
                    BlockEntityType.Builder.of(SpaceshipReactorBlockEntity::new, ModBlocks.SPACESHIP_REACTOR.get()).build(null));

    public static final Supplier<BlockEntityType<SpaceshipShieldBlockEntity>> SPACESHIP_SHIELD_BE =
            BLOCK_ENTITIES.register("spaceship_shield_be", () ->
                    BlockEntityType.Builder.of(SpaceshipShieldBlockEntity::new, ModBlocks.SPACESHIP_SHIELD.get()).build(null));

    public static final Supplier<BlockEntityType<com.lit.spaceships.block.entity.PulseLaserBlockEntity>> PULSE_LASER_BE =
            BLOCK_ENTITIES.register("pulse_laser_be", () ->
                    BlockEntityType.Builder.of(com.lit.spaceships.block.entity.PulseLaserBlockEntity::new, ModBlocks.PULSE_LASER.get()).build(null));

    public static final Supplier<BlockEntityType<com.lit.spaceships.block.entity.HeavyBeamBlockEntity>> HEAVY_BEAM_BE =
            BLOCK_ENTITIES.register("heavy_beam_be", () ->
                    BlockEntityType.Builder.of(com.lit.spaceships.block.entity.HeavyBeamBlockEntity::new, ModBlocks.HEAVY_BEAM.get()).build(null));

    public static final Supplier<BlockEntityType<com.lit.spaceships.block.entity.MiningLaserBlockEntity>> MINING_LASER_BE =
            BLOCK_ENTITIES.register("mining_laser_be", () ->
                    BlockEntityType.Builder.of(com.lit.spaceships.block.entity.MiningLaserBlockEntity::new, ModBlocks.MINING_LASER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}