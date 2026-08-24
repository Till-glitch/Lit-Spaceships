package com.peaceman.alpha.network;

import com.peaceman.alpha.client.network.ClientPayloadHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPayloads {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModPayloads::registerPayloads);
    }

    private static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0");

        registrar.playToServer(
                ShipActionPayload.TYPE,
                ShipActionPayload.STREAM_CODEC,
                ServerPayloadHandler::handleAction
        );

        registrar.playToServer(
                ShipCombatActionPayload.TYPE,
                ShipCombatActionPayload.STREAM_CODEC,
                ServerPayloadHandler::handleCombatAction
        );

        registrar.playToClient(
                ShieldBubbleSyncPacket.TYPE,
                ShieldBubbleSyncPacket.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleShieldBubbleSync(payload, context)
        );

        registrar.playToClient(
                ShipStructureSyncPayload.TYPE,
                ShipStructureSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleStructureSync(payload, context)
        );

        registrar.playToClient(
                ShipStateSyncPayload.TYPE,
                ShipStateSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleStateSync(payload, context)
        );

        registrar.playToClient(
                ShipPositionSyncPayload.TYPE,
                ShipPositionSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handlePositionSync(payload, context)
        );

        registrar.playToClient(
                ShipImpactEventPayload.TYPE,
                ShipImpactEventPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleShipImpact(payload, context)
        );

        registrar.playToClient(
                ShipStructureDeltaPayload.TYPE,
                ShipStructureDeltaPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleStructureDelta(payload, context)
        );

        registrar.playToClient(
                LaserFirePayload.TYPE,
                LaserFirePayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleLaserFire(payload, context)
        );

        registrar.playToClient(
                LaserStateSyncPayload.TYPE,
                LaserStateSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleLaserStateSync(payload, context)
        );

        registrar.playToClient(
                ShipDimensionSyncPayload.TYPE,
                ShipDimensionSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleDimensionSync(payload, context)
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

        registrar.playToServer(
                TurretLockTogglePayload.TYPE,
                TurretLockTogglePayload.STREAM_CODEC,
                ServerPayloadHandler::handleTurretLockToggle
        );
    }
}
