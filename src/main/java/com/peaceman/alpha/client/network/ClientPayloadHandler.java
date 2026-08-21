package com.peaceman.alpha.client.network;

import com.peaceman.alpha.client.state.ClientShipManager;
import com.peaceman.alpha.network.ShieldBubbleSyncPacket;
import com.peaceman.alpha.network.ShipStateSyncPayload;
import com.peaceman.alpha.network.ShipStructureSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {

    public static void handleShieldBubbleSync(final ShieldBubbleSyncPacket packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                Level clientLevel = Minecraft.getInstance().level;
                if (clientLevel != null && clientLevel.isLoaded(packet.anchorPos())) {
                    ClientShipManager.updateShieldBubble(packet.shipId(), packet.anchorPos(), packet.relativeBubbleBlocks());
                } else {
                    ClientShipManager.addPendingSync(packet);
                }
            });
        }
    }

    public static void handleStructureSync(final ShipStructureSyncPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                ClientShipManager.updateShipStructure(packet.shipId(), packet.controllerPos(), packet.relativeBlocks());
            });
        }
    }

    public static void handleStateSync(final ShipStateSyncPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                ClientShipManager.updateShipState(packet.shipId(), packet.currentEnergy(), packet.isShieldActive());
            });
        }
    }
}
