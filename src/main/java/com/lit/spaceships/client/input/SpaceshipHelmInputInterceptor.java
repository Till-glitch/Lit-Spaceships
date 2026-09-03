package com.lit.spaceships.client.input;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.network.ShipMovementRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = LitSpaceships.MODID, value = Dist.CLIENT)
public class SpaceshipHelmInputInterceptor {

    private static long lastPayloadTime = 0;
    private static final long PAYLOAD_INTERVAL_MS = 50; // 20 updates per second

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (SpaceshipClientInputHandler.activeHelmShipId == null) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.getEntity() != player) return;

        float forwardImpulse = event.getInput().forwardImpulse;
        float leftImpulse = event.getInput().leftImpulse;
        boolean jump = event.getInput().jumping;
        boolean shift = event.getInput().shiftKeyDown;
        
        float upImpulse = 0;
        if (jump) upImpulse += 1.0f;
        if (shift) upImpulse -= 1.0f;

        // Nullify player movement
        event.getInput().forwardImpulse = 0;
        event.getInput().leftImpulse = 0;
        event.getInput().jumping = false;
        event.getInput().shiftKeyDown = false;
        
        if (com.lit.spaceships.client.ClientModEvents.KEY_EXIT_HELM.consumeClick()) {
            SpaceshipClientInputHandler.activeHelmShipId = null;
            if (player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(com.lit.spaceships.registry.ModI18n.Message.HELM_CONTROL_LEAVE).withStyle(net.minecraft.ChatFormatting.YELLOW), true);
            }
            return;
        }

        if (com.lit.spaceships.client.ClientModEvents.KEY_OPEN_HELM_CONFIG.consumeClick()) {
            PacketDistributor.sendToServer(new com.lit.spaceships.network.OpenHelmConfigPayload(java.util.Optional.ofNullable(SpaceshipClientInputHandler.activeHelmShipId)));
        }

        if (com.lit.spaceships.client.ClientModEvents.KEY_ROTATE_LEFT.consumeClick()) {
            PacketDistributor.sendToServer(new com.lit.spaceships.network.ShipActionPayload(
                    java.util.Optional.ofNullable(SpaceshipClientInputHandler.activeHelmShipId),
                    net.minecraft.core.BlockPos.ZERO,
                    com.lit.spaceships.network.ShipActionPayload.ActionType.ROTATE_CCW,
                    90,
                    ""
            ));
        }

        if (com.lit.spaceships.client.ClientModEvents.KEY_ROTATE_RIGHT.consumeClick()) {
            PacketDistributor.sendToServer(new com.lit.spaceships.network.ShipActionPayload(
                    java.util.Optional.ofNullable(SpaceshipClientInputHandler.activeHelmShipId),
                    net.minecraft.core.BlockPos.ZERO,
                    com.lit.spaceships.network.ShipActionPayload.ActionType.ROTATE_CW,
                    90,
                    ""
            ));
        }

        long now = System.currentTimeMillis();
        if (now - lastPayloadTime >= PAYLOAD_INTERVAL_MS) {
            if (forwardImpulse != 0 || leftImpulse != 0 || upImpulse != 0) {
                PacketDistributor.sendToServer(new ShipMovementRequestPayload(
                        SpaceshipClientInputHandler.activeHelmShipId,
                        forwardImpulse,
                        leftImpulse,
                        upImpulse
                ));
            }
            lastPayloadTime = now;
        }
    }
}
