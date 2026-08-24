package com.peaceman.alpha.client.input;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.SpaceshipControlBlock;
import com.peaceman.alpha.block.SpaceshipHelmBlock;
import com.peaceman.alpha.block.entity.SpaceshipControlBlockEntity;
import com.peaceman.alpha.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.UUID;

@EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SpaceshipClientInputHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof SpaceshipHelmBlock) {
            ClientHooks.openHelmScreen(pos);
            // Wir canceln nicht zwingend, damit Server-seitiges useWithoutItem noch durchläuft und Success zurückgibt.
        } else if (block instanceof SpaceshipControlBlock) {
            BlockEntity be = event.getLevel().getBlockEntity(pos);
            if (be instanceof com.peaceman.alpha.block.ISpaceshipNode node) {
                UUID shipId = node.getShipId();
                ClientHooks.openControlScreen(shipId, pos);
            }
        }
    }
}
