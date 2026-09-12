package com.lit.spaceships.block;

import com.lit.spaceships.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Versiegelte Bergungskiste (Epoch 12): oeffnet sich nur mit konstantem
 * Redstone-Signal oder per Access Cipher. Beim Oeffnen wird die Beute als
 * Items ausgegeben (Fracht: Legierungen, seltene Technologie).
 */
public class SealedSalvageCrateBlock extends Block {

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public SealedSalvageCrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    /** Redstone: konstantes Signal oeffnet die Kiste. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide() && !state.getValue(OPEN)
                && level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.setValue(OPEN, Boolean.TRUE), 3);
            dropLoot(level, pos);
        }
    }

    /** Rechtsklick: nur mit Access Cipher entschluesselbar. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(OPEN)) {
            return InteractionResult.CONSUME;
        }
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.is(ModItems.ACCESS_CIPHER.get())) {
            level.setBlock(pos, state.setValue(OPEN, Boolean.TRUE), 3);
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel
                    && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                held.hurtAndBreak(1, serverLevel, serverPlayer,
                        item -> player.level().playSound(null, pos,
                                net.minecraft.sounds.SoundEvents.ITEM_BREAK,
                                net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.8F));
            }
            dropLoot(level, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private void dropLoot(Level level, BlockPos pos) {
        // Konzentrierte Bergungsfracht (spiegelte Legacy-Salvage wider)
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 5)));
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5, new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 3)));
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5, new ItemStack(net.minecraft.world.item.Items.ECHO_SHARD, 2)));
    }
}
