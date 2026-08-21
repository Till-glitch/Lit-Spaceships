package com.peaceman.alpha.client.network;

import com.peaceman.alpha.client.state.ClientShipManager;
import com.peaceman.alpha.helper.ShieldLifecycleLogger;
import com.peaceman.alpha.network.ShieldBubbleSyncPacket;
import com.peaceman.alpha.network.ShipPositionSyncPayload;
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
                boolean isLoaded = (clientLevel != null && clientLevel.isLoaded(packet.anchorPos()));
                ShieldLifecycleLogger.logClientPayloadReceived("ShieldBubbleSyncPacket", packet.shipId(), packet.anchorPos(), isLoaded);

                if (isLoaded) {
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
                Level clientLevel = Minecraft.getInstance().level;
                boolean isLoaded = (clientLevel != null && clientLevel.isLoaded(packet.controllerPos()));
                ShieldLifecycleLogger.logClientPayloadReceived("ShipStructureSyncPayload", packet.shipId(), packet.controllerPos(), isLoaded);
                ClientShipManager.updateShipStructure(packet.shipId(), packet.controllerPos(), packet.relativeBlocks());
            });
        }
    }

    public static void handleStateSync(final ShipStateSyncPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                ShieldLifecycleLogger.logClientPayloadReceived("ShipStateSyncPayload", packet.shipId(), null, true);
                ClientShipManager.updateShipState(packet.shipId(), packet.currentEnergy(), packet.isShieldActive(),
                        packet.shieldCooldownRemainingTicks(), packet.movementCooldownRemainingTicks());
            });
        }
    }

    public static void handlePositionSync(final ShipPositionSyncPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                ShieldLifecycleLogger.logClientPayloadReceived("ShipPositionSyncPayload", packet.shipId(), packet.newAnchorPos(), true);
                ClientShipManager.updateShipPosition(packet.shipId(), packet.newAnchorPos());
            });
        }
    }

    public static void handleShipImpact(final com.peaceman.alpha.network.ShipImpactEventPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                ShieldLifecycleLogger.logClientPayloadReceived("ShipImpactEventPayload", packet.shipId(), null, true);
                ClientShipManager.addImpact(packet.shipId(), packet.impactPos());
            });
        }
    }

    public static void handleStructureDelta(final com.peaceman.alpha.network.ShipStructureDeltaPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                ShieldLifecycleLogger.logClientPayloadReceived("ShipStructureDeltaPayload", packet.shipId(), null, true);
                ClientShipManager.removeStructureBlocks(packet.shipId(), packet.removedBlocks());
            });
        }
    }

    public static void handleLaserFire(final com.peaceman.alpha.network.LaserFirePayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                com.peaceman.alpha.client.render.LaserBeamRenderer.addPulseBeam(
                        packet.shooterShipId(), packet.startPos(), packet.endPos(), packet.tier()
                );
            });
        }
    }

    public static void handleLaserStateSync(final com.peaceman.alpha.network.LaserStateSyncPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                com.peaceman.alpha.client.render.LaserBeamRenderer.setContinuousBeam(
                        packet.shooterShipId(), packet.weaponPos(), packet.isFiring(), packet.tier()
                );
            });
        }
    }
}
