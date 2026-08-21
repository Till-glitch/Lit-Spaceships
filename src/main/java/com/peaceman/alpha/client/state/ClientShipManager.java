package com.peaceman.alpha.client.state;

import com.peaceman.alpha.Alpha;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet den clientseitigen Lebenszyklus aller sichtbaren Schiffe.
 * Dient als View-Cache für Rendering-Systeme wie ShieldRenderer.
 */
@EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientShipManager {

    private static final Map<UUID, ClientShipState> ACTIVE_CLIENT_SHIPS = new ConcurrentHashMap<>();

    public static ClientShipState getOrCreateShip(UUID shipId) {
        return ACTIVE_CLIENT_SHIPS.computeIfAbsent(shipId, ClientShipState::new);
    }

    public static ClientShipState getShip(UUID shipId) {
        if (shipId == null) return null;
        return ACTIVE_CLIENT_SHIPS.get(shipId);
    }

    public static Collection<ClientShipState> getAllShips() {
        return ACTIVE_CLIENT_SHIPS.values();
    }

    public static void updateShieldBubble(UUID shipId, BlockPos anchorPos, Set<BlockPos> relativeBubbleBlocks) {
        ClientShipState shipState = getOrCreateShip(shipId);
        shipState.setAnchorPos(anchorPos);
        shipState.updateMesh(relativeBubbleBlocks);
    }

    public static void updateShipStructure(UUID shipId, BlockPos controllerPos, Set<BlockPos> relativeBlocks) {
        ClientShipState shipState = getOrCreateShip(shipId);
        shipState.setAnchorPos(controllerPos);
        shipState.setRelativeStructureBlocks(relativeBlocks);
    }

    public static void updateShipState(UUID shipId, int currentEnergy, boolean isShieldActive) {
        ClientShipState shipState = getShip(shipId);
        if (shipState != null) {
            shipState.setShieldActive(isShieldActive);
        }
    }

    public static void removeShip(UUID shipId) {
        ClientShipState removed = ACTIVE_CLIENT_SHIPS.remove(shipId);
        if (removed != null) {
            removed.close();
        }
    }

    public static void clear() {
        for (ClientShipState state : ACTIVE_CLIENT_SHIPS.values()) {
            state.close();
        }
        ACTIVE_CLIENT_SHIPS.clear();
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
