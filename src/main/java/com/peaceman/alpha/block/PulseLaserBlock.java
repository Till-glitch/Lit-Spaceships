package com.peaceman.alpha.block;

import com.mojang.serialization.MapCodec;
import com.peaceman.alpha.block.entity.PulseLaserBlockEntity;
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
 * Block für den Impulslaser (Pulse-Laser).
 * Kann in alle 6 Richtungen (Up/Down/North/South/East/West) ausgerichtet
 * werden.
 */
public class PulseLaserBlock extends BaseEntityBlock {
    public static final MapCodec<PulseLaserBlock> CODEC = simpleCodec(PulseLaserBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public PulseLaserBlock(Properties properties) {
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

    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE_UP = Block.box(0, 0, 0, 16, 4, 16);
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE_DOWN = Block.box(0, 12, 0, 16, 16, 16);
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0, 16, 16, 4);
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE_NORTH = Block.box(0, 0, 12, 16, 16, 16);
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE_EAST = Block.box(0, 0, 0, 4, 16, 16);
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE_WEST = Block.box(12, 0, 0, 16, 16, 16);

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        switch (state.getValue(FACING)) {
            case DOWN: return SHAPE_DOWN;
            case SOUTH: return SHAPE_SOUTH;
            case NORTH: return SHAPE_NORTH;
            case EAST: return SHAPE_EAST;
            case WEST: return SHAPE_WEST;
            case UP:
            default: return SHAPE_UP;
        }
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
        return new PulseLaserBlockEntity(pos, state);
    }

    @Override
    public net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof PulseLaserBlockEntity laserBE) {
                if (laserBE.isOccupied()) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("Dieser Geschützturm ist bereits belegt!"),
                            true);
                    return net.minecraft.world.InteractionResult.CONSUME;
                }

                com.peaceman.alpha.entity.TurretSeatEntity seat = new com.peaceman.alpha.entity.TurretSeatEntity(level,
                        pos, laserBE.getShipId());
                level.addFreshEntity(seat);
                player.startRiding(seat, true);
                laserBE.setOccupied(true);
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PulseLaserBlockEntity laserBE) {
                laserBE.setOccupied(false);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> blockEntityType) {
        if (level.isClientSide())
            return null;
        return createTickerHelper(blockEntityType, ModBlockEntities.PULSE_LASER_BE.get(),
                (lvl, pos, st, be) -> be.serverTick(lvl, pos, st));
    }
}
