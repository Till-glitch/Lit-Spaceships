package com.peaceman.alpha.block;

import com.mojang.serialization.MapCodec;
import com.peaceman.alpha.block.entity.MiningLaserBlockEntity;
import com.peaceman.alpha.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Block für den Mining-Laser (Abbauwerkzeug).
 * Kann in alle 6 Richtungen montiert werden.
 */
public class MiningLaserBlock extends BaseEntityBlock {
    public static final MapCodec<MiningLaserBlock> CODEC = simpleCodec(MiningLaserBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MiningLaserBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MiningLaserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;
        return createTickerHelper(blockEntityType, ModBlockEntities.MINING_LASER_BE.get(),
                (lvl, pos, st, be) -> be.serverTick(lvl, pos, st));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                if (level.getBlockEntity(pos) instanceof MiningLaserBlockEntity be && be.isMining()) {
                    boolean isMovingShip = be.getShipId() != null && com.peaceman.alpha.ship.service.ShipMovementService.isShipMoving(be.getShipId());
                    if (!isMovingShip) {
                        be.setMining(false);
                        if (be.getShipId() != null) {
                            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(
                                    serverLevel, new net.minecraft.world.level.ChunkPos(pos),
                                    new com.peaceman.alpha.network.LaserStateSyncPayload(be.getShipId(), pos, false, com.peaceman.alpha.ship.combat.LaserWeaponTier.MINING_LASER)
                            );
                        }
                    } else {
                        be.clearDrillProgress(level);
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
