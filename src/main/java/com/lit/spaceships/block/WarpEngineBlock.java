package com.lit.spaceships.block;

import com.lit.spaceships.block.entity.WarpEngineBlockEntity;
import com.lit.spaceships.registry.ModBlockEntities;
import com.lit.spaceships.registry.ModI18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class WarpEngineBlock extends BaseEntityBlock {

    public static final com.mojang.serialization.MapCodec<WarpEngineBlock> CODEC = simpleCodec(WarpEngineBlock::new);

    public WarpEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WarpEngineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.WARP_ENGINE_BE.get(),
                level.isClientSide() ? null : WarpEngineBlockEntity::serverTick);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // UI wird clientseitig über das RightClickBlock-Event geöffnet (wie bei SpaceshipControlBlock)
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Dev-Cheat: Rechtsklick mit Redstone lädt den Warpantrieb sofort auf 100.000 FE
        if (stack.is(Items.REDSTONE)) {
            if (!level.isClientSide()) {
                if (level.getBlockEntity(pos) instanceof WarpEngineBlockEntity be) {
                    be.receiveEnergy(100000, false);
                    be.setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);
                    player.sendSystemMessage(Component.translatable(ModI18n.Message.DEV_CHEAT_ENERGY, "100,000"));
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !isMoving) {
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof WarpEngineBlockEntity warpBe) {
                    warpBe.abortCountdown(Component.translatable(ModI18n.Message.WARP_COUNTDOWN_ABORTED));
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
