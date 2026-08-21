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

        registrar.playToClient(
                ShipPositionSyncPayload.TYPE,
                ShipPositionSyncPayload.STREAM_CODEC,
                ClientPayloadHandler::handlePositionSync
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
    }
}
