package com.lit.spaceships.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPayloads {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModPayloads::registerPayloads);
    }

    private static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0");

        // 1. Serverbound Payloads (Universal auf Client & Server verfügbar)
        registrar.playToServer(
                ShipActionPayload.TYPE,
                ShipActionPayload.STREAM_CODEC,
                ServerPayloadHandler::handleAction
        );

        registrar.playToServer(
                ShipMovementRequestPayload.TYPE,
                ShipMovementRequestPayload.STREAM_CODEC,
                ServerPayloadHandler::handleMovementRequest
        );

        registrar.playToServer(
                ShipCombatActionPayload.TYPE,
                ShipCombatActionPayload.STREAM_CODEC,
                ServerPayloadHandler::handleCombatAction
        );

        registrar.playToServer(
                OpenHelmConfigPayload.TYPE,
                OpenHelmConfigPayload.STREAM_CODEC,
                ServerPayloadHandler::handleOpenHelmConfig
        );

        registrar.playToServer(
                TurretLockTogglePayload.TYPE,
                TurretLockTogglePayload.STREAM_CODEC,
                ServerPayloadHandler::handleTurretLockToggle
        );

        // 2. Clientbound / Bidirektionale Payloads (Sided getrennt)
        if (FMLEnvironment.dist.isClient()) {
            com.lit.spaceships.client.network.ClientPayloadRegistrar.registerClientPayloads(registrar);
        } else {
            registerServerDummyPayloads(registrar);
        }
    }

    private static void registerServerDummyPayloads(final PayloadRegistrar registrar) {
        registrar.playToClient(ShieldBubbleSyncPacket.TYPE, ShieldBubbleSyncPacket.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(ShipStructureSyncPayload.TYPE, ShipStructureSyncPayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(ShipStateSyncPayload.TYPE, ShipStateSyncPayload.STREAM_CODEC, (p, c) -> {});
        registrar.playBidirectional(ShipPositionSyncPayload.TYPE, ShipPositionSyncPayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(ShipImpactEventPayload.TYPE, ShipImpactEventPayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(ShipStructureDeltaPayload.TYPE, ShipStructureDeltaPayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(LaserFirePayload.TYPE, LaserFirePayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(LaserStateSyncPayload.TYPE, LaserStateSyncPayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(ShipDimensionSyncPayload.TYPE, ShipDimensionSyncPayload.STREAM_CODEC, (p, c) -> {});
        registrar.playToClient(ShieldZoneStatePayload.TYPE, ShieldZoneStatePayload.STREAM_CODEC, (p, c) -> {});
        registrar.playBidirectional(TurretAimPayload.TYPE, TurretAimPayload.STREAM_CODEC, (p, c) -> {
            if (c.flow().isServerbound()) {
                ServerPayloadHandler.handleTurretAim(p, c);
            }
        });
        registrar.playBidirectional(TurretAimSyncPayload.TYPE, TurretAimSyncPayload.STREAM_CODEC, (p, c) -> {
            if (c.flow().isServerbound()) {
                ServerPayloadHandler.handleTurretAimSync(p, c);
            }
        });
    }
}
