package com.lit.spaceships.client.network;

import com.lit.spaceships.network.*;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registriert alle clientseitigen Payload-Handler ausschließlich auf Dist.CLIENT.
 * Verhindert NoClassDefFoundError / Dist-Cleaner-Exceptions auf dem Dedicated Server.
 */
public class ClientPayloadRegistrar {

    public static void registerClientPayloads(PayloadRegistrar registrar) {
        registrar.playToClient(
                ShieldBubbleSyncPacket.TYPE,
                ShieldBubbleSyncPacket.STREAM_CODEC,
                ClientPayloadHandler::handleShieldBubbleSync
        );

        registrar.playToClient(
                ShipStructureSyncPayload.TYPE,
                ShipStructureSyncPayload.STREAM_CODEC,
                ClientPayloadHandler::handleStructureSync
        );

        registrar.playToClient(
                ShipStateSyncPayload.TYPE,
                ShipStateSyncPayload.STREAM_CODEC,
                ClientPayloadHandler::handleStateSync
        );

        registrar.playBidirectional(
                ShipPositionSyncPayload.TYPE,
                ShipPositionSyncPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler::handlePositionSync,
                        (payload, context) -> {}
                )
        );

        registrar.playToClient(
                ShipImpactEventPayload.TYPE,
                ShipImpactEventPayload.STREAM_CODEC,
                ClientPayloadHandler::handleShipImpact
        );

        registrar.playToClient(
                ShipStructureDeltaPayload.TYPE,
                ShipStructureDeltaPayload.STREAM_CODEC,
                ClientPayloadHandler::handleStructureDelta
        );

        registrar.playToClient(
                LaserFirePayload.TYPE,
                LaserFirePayload.STREAM_CODEC,
                ClientPayloadHandler::handleLaserFire
        );

        registrar.playToClient(
                LaserStateSyncPayload.TYPE,
                LaserStateSyncPayload.STREAM_CODEC,
                ClientPayloadHandler::handleLaserStateSync
        );

        registrar.playToClient(
                ShipDimensionSyncPayload.TYPE,
                ShipDimensionSyncPayload.STREAM_CODEC,
                ClientPayloadHandler::handleDimensionSync
        );

        registrar.playToClient(
                ShieldZoneStatePayload.TYPE,
                ShieldZoneStatePayload.STREAM_CODEC,
                ClientPayloadHandler::handleShieldZoneStateSync
        );

        registrar.playBidirectional(
                TurretAimPayload.TYPE,
                TurretAimPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.flow().isClientbound()) {
                        ClientPayloadHandler.handleTurretAim(payload, context);
                    } else {
                        ServerPayloadHandler.handleTurretAim(payload, context);
                    }
                }
        );

        registrar.playBidirectional(
                TurretAimSyncPayload.TYPE,
                TurretAimSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.flow().isClientbound()) {
                        ClientPayloadHandler.handleTurretAimSync(payload, context);
                    } else {
                        ServerPayloadHandler.handleTurretAimSync(payload, context);
                    }
                }
        );
    }
}
