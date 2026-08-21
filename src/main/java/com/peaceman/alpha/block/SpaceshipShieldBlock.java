package com.peaceman.alpha.block;

import com.peaceman.alpha.block.entity.SpaceshipShieldBlockEntity;
import com.peaceman.alpha.ship.SpaceshipShieldHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SpaceshipShieldBlock extends Block implements EntityBlock {

    public SpaceshipShieldBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpaceshipShieldBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !isMoving) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SpaceshipShieldBlockEntity shieldBE) {
                UUID shipId = shieldBE.getShipId();
                if (shipId != null) {
                    SpaceshipShieldHandler.onShieldBlockDestroyed(level, pos, shipId);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}