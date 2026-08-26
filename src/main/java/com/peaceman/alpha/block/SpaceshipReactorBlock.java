package com.peaceman.alpha.block;

import com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SpaceshipReactorBlock extends BaseEntityBlock  {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT = net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;

    public static final com.mojang.serialization.MapCodec<SpaceshipReactorBlock> CODEC = simpleCodec(SpaceshipReactorBlock::new);
    public SpaceshipReactorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpaceshipReactorBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SpaceshipReactorBlockEntity reactorEntity) {
                player.openMenu(reactorEntity, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
                                                                  BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                                                  BlockHitResult hitResult) {

        // --- DER ENTWICKLER-CHEAT ---
        if (stack.is(net.minecraft.world.item.Items.REDSTONE)) {
            if (!level.isClientSide()) {
                if (level.getBlockEntity(pos) instanceof SpaceshipReactorBlockEntity be) {
                    be.getEnergyStorage().receiveEnergy(500000, false);
                    be.setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);
                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("DEV-CHEAT: 50.000 FE geladen!"));
                }
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Wenn es kein Redstone ist, geben wir an die Methode für leere Hände weiter!
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SpaceshipReactorBlockEntity) {
            return (MenuProvider) blockEntity;
        }
        return null;
    }

}
