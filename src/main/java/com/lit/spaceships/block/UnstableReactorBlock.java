package com.lit.spaceships.block;

import com.lit.spaceships.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Instabiler Reaktor (Epoch 12): BlockEntity-getriebener Meltdown-Countdown
 * (Stufe 0..4, 90 s). Zwei benachbarte Kuehlventile resetten den Countdown und
 * schalten die Kernkammer (reactor_core_salvage) frei; Versagen loest eine
 * Explosion aus.
 */
public class UnstableReactorBlock extends BaseEntityBlock {

    public static final MapCodec<UnstableReactorBlock> CODEC = simpleCodec(UnstableReactorBlock::new);

    public UnstableReactorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UnstableReactorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.UNSTABLE_REACTOR_BE.get(),
                        UnstableReactorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos)
                instanceof UnstableReactorBlockEntity reactor) {
            // Spieler ohne Item = Kuehlversuch (Ventil oeffnen)
            reactor.cool();
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

}
