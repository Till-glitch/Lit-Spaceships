package com.lit.spaceships.client.network;

import com.lit.spaceships.client.state.ClientShipManager;
import com.lit.spaceships.helper.ShieldLifecycleLogger;
import com.lit.spaceships.network.ShieldBubbleSyncPacket;
import com.lit.spaceships.network.ShipPositionSyncPayload;
import com.lit.spaceships.network.ShipStateSyncPayload;
import com.lit.spaceships.network.ShipStructureSyncPayload;
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
                if (packet.relativeBlocks() == null || packet.relativeBlocks().isEmpty()) {
                    ClientShipManager.removeShip(packet.shipId());
                } else {
                    ClientShipManager.updateShipStructure(packet.shipId(), packet.controllerPos(), packet.relativeBlocks());
                }
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
                com.lit.spaceships.client.state.ClientLaserState.removeBeamsForShip(packet.shipId());
            });
        }
    }

    public static void handleShipImpact(final com.lit.spaceships.network.ShipImpactEventPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                ShieldLifecycleLogger.logClientPayloadReceived("ShipImpactEventPayload", packet.shipId(), null, true);
                ClientShipManager.addImpact(packet.shipId(), packet.impactPos());
            });
        }
    }

    public static void handleStructureDelta(final com.lit.spaceships.network.ShipStructureDeltaPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                ShieldLifecycleLogger.logClientPayloadReceived("ShipStructureDeltaPayload", packet.shipId(), null, true);
                ClientShipManager.removeStructureBlocks(packet.shipId(), packet.removedBlocks());
            });
        }
    }

    public static void handleLaserFire(final com.lit.spaceships.network.LaserFirePayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                com.lit.spaceships.client.render.LaserBeamRenderer.addPulseBeam(
                        packet.shooterShipId(), packet.startPos(), packet.endPos(), packet.tier()
                );
            });
        }
    }

    public static void handleLaserStateSync(final com.lit.spaceships.network.LaserStateSyncPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                com.lit.spaceships.client.render.LaserBeamRenderer.setContinuousBeam(
                        packet.shooterShipId(), packet.weaponPos(), packet.isFiring(), packet.tier()
                );
            });
        }
    }

    public static void handleDimensionSync(final com.lit.spaceships.network.ShipDimensionSyncPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                ClientShipManager.updateShipDimension(packet.shipId(), packet.dimension());
                com.lit.spaceships.client.state.ClientLaserState.removeBeamsForShip(packet.shipId());
            });
        }
    }

    public static void handleShieldZoneStateSync(final com.lit.spaceships.network.ShieldZoneStatePayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                ClientShipManager.updateShieldZoneState(packet.shipId(), packet.activeMask(), packet.zoneEnergies());
            });
        }
    }

    public static void handleTurretAim(final com.lit.spaceships.network.TurretAimPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level != null && mc.level.getBlockEntity(packet.weaponPos()) instanceof com.lit.spaceships.block.entity.AbstractLaserNodeBlockEntity laserBE) {
                    float yaw = com.lit.spaceships.ship.combat.aim.AimTransformMath.decompressAngle(packet.compressedYaw());
                    float pitch = com.lit.spaceships.ship.combat.aim.AimTransformMath.decompressAngle(packet.compressedPitch());
                    laserBE.setAimAngles(new com.lit.spaceships.ship.combat.aim.AimAngles(yaw, pitch));
                }
            });
        }
    }

    public static void handleTurretAimSync(final com.lit.spaceships.network.TurretAimSyncPayload packet, final IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level != null && mc.level.getBlockEntity(packet.weaponPos()) instanceof com.lit.spaceships.block.entity.AbstractLaserNodeBlockEntity laserBE) {
                    // Aktualisiere nur, wenn der lokale Spieler nicht gerade selbst in diesem Turm sitzt und ihn steuert
                    if (mc.player == null || !(mc.player.getVehicle() instanceof com.lit.spaceships.entity.TurretSeatEntity seat && packet.weaponPos().equals(seat.getWeaponPos()))) {
                        laserBE.setAimAngles(new com.lit.spaceships.ship.combat.aim.AimAngles(packet.yaw(), packet.pitch()));
                    }
                }
            });
        }
    }
}
