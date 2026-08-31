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

    public static void handleDimensionSync(final com.peaceman.alpha.network.ShipDimensionSyncPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                ClientShipManager.updateShipDimension(packet.shipId(), packet.dimension());
            });
        }
    }

    public static void handleShieldZoneStateSync(final com.peaceman.alpha.network.ShieldZoneStatePayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                ClientShipManager.updateShieldZoneState(packet.shipId(), packet.activeMask());
            });
        }
    }

    public static void handleTurretAim(final com.peaceman.alpha.network.TurretAimPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level != null && mc.level.getBlockEntity(packet.weaponPos()) instanceof com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity laserBE) {
                    float yaw = com.peaceman.alpha.ship.combat.aim.AimTransformMath.decompressAngle(packet.compressedYaw());
                    float pitch = com.peaceman.alpha.ship.combat.aim.AimTransformMath.decompressAngle(packet.compressedPitch());
                    laserBE.setAimAngles(new com.peaceman.alpha.ship.combat.aim.AimAngles(yaw, pitch));
                }
            });
        }
    }

    public static void handleTurretAimSync(final com.peaceman.alpha.network.TurretAimSyncPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level != null && mc.level.getBlockEntity(packet.weaponPos()) instanceof com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity laserBE) {
                    // Aktualisiere nur, wenn der lokale Spieler nicht gerade selbst in diesem Turm sitzt und ihn steuert
                    if (mc.player == null || !(mc.player.getVehicle() instanceof com.peaceman.alpha.entity.TurretSeatEntity seat && packet.weaponPos().equals(seat.getWeaponPos()))) {
                        laserBE.setAimAngles(new com.peaceman.alpha.ship.combat.aim.AimAngles(packet.yaw(), packet.pitch()));
                    }
                }
            });
        }
    }
}
