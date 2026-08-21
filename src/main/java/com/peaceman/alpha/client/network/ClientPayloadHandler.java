package com.peaceman.alpha.client.network;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.client.state.ClientShipManager;
import com.peaceman.alpha.network.ShieldBubbleSyncPacket;
import com.peaceman.alpha.network.ShipStateSyncPayload;
import com.peaceman.alpha.network.ShipStructureSyncPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {

    public static void handleShieldBubbleSync(final ShieldBubbleSyncPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientShipManager.updateShieldBubble(packet.shipId(), packet.anchorPos(), packet.relativeBubbleBlocks());
        });
    }

    public static void handleStructureSync(final ShipStructureSyncPayload packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientShipManager.updateShipStructure(packet.shipId(), packet.controllerPos(), packet.relativeBlocks());
        });
    }

    public static void handleStateSync(final ShipStateSyncPayload packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientShipManager.updateShipState(packet.shipId(), packet.currentEnergy(), packet.isShieldActive());
            Alpha.LOGGER.info("Received ship state sync for player {}", context.player().getName().getString());
        });
    }
}
