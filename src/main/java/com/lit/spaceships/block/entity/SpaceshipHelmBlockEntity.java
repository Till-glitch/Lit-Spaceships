package com.peaceman.alpha.block.entity;

import com.peaceman.alpha.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class SpaceshipHelmBlockEntity extends AbstractSpaceshipNodeBlockEntity {

    public SpaceshipHelmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPACESHIP_HELM_BE.get(), pos, state);
    }

    // FERTIG!
}