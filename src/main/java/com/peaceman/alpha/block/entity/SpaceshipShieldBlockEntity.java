package com.peaceman.alpha.block.entity;

import com.peaceman.alpha.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity für den Raumschiff-Schildgenerator.
 * Erbt von AbstractSpaceshipNodeBlockEntity für natives Data Attachment Tracking.
 */
public class SpaceshipShieldBlockEntity extends AbstractSpaceshipNodeBlockEntity {

    public SpaceshipShieldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPACESHIP_SHIELD_BE.get(), pos, state);
    }
}