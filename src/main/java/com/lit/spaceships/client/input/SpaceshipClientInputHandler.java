package com.peaceman.alpha.client.input;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.SpaceshipControlBlock;
import com.peaceman.alpha.block.SpaceshipHelmBlock;
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

    public static UUID activeHelmShipId = null;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof SpaceshipHelmBlock) {
            BlockEntity be = event.getLevel().getBlockEntity(pos);
            if (be instanceof com.peaceman.alpha.block.ISpaceshipNode node) {
                // If not sneaking, set as active pilot (sneaking is handled by server opening the menu)
                if (!event.getEntity().isShiftKeyDown()) {
                    activeHelmShipId = node.getShipId();
                    event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable(com.peaceman.alpha.registry.ModI18n.Message.HELM_CONTROL_ENTER).withStyle(net.minecraft.ChatFormatting.GREEN), true);
                }
            }
        } else if (block instanceof SpaceshipControlBlock) {
            BlockEntity be = event.getLevel().getBlockEntity(pos);
            if (be instanceof com.peaceman.alpha.block.ISpaceshipNode node) {
                UUID shipId = node.getShipId();
                ClientHooks.openControlScreen(shipId, pos);
            }
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        if (activeHelmShipId != null && event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.peaceman.alpha.network.ShipCombatActionPayload(
                            java.util.Optional.ofNullable(activeHelmShipId),
                            com.peaceman.alpha.network.ShipCombatActionPayload.CombatAction.FIRE_ALL,
                            java.util.Optional.empty()
                    )
            );
        }
    }
}
