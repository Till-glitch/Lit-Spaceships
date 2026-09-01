package com.peaceman.alpha.block;

import com.peaceman.alpha.block.entity.SpaceshipControlBlockEntity;
import com.peaceman.alpha.ship.service.ServerShipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SpaceshipControlBlock extends Block implements EntityBlock {

    public SpaceshipControlBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpaceshipControlBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Die GUI wird nun asynchron über das RightClickBlock-Event auf dem Client geöffnet.
        // Das stellt sicher, dass der Server-JVM diese Klasse fehlerfrei laden kann.
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof SpaceshipControlBlockEntity shipBe && shipBe.getShipId() != null) {
                    ServerShipManager.deleteShip(level, ServerShipManager.getShip(shipBe.getShipId()));
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}