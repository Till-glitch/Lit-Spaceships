package com.peaceman.alpha.block;

import com.peaceman.alpha.block.entity.SpaceshipHelmBlockEntity;
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

public class SpaceshipHelmBlock extends Block implements EntityBlock {

    public SpaceshipHelmBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpaceshipHelmBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        // Die GUI wird nun asynchron über das RightClickBlock-Event auf dem Client geöffnet.
        // Das stellt sicher, dass der Server-JVM diese Klasse (und das Block-Objekt)
        // fehlerfrei laden kann.

        return InteractionResult.SUCCESS;
    }
}