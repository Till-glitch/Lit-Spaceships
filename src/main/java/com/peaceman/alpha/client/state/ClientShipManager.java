package com.peaceman.alpha.client.state;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.helper.ShieldLifecycleLogger;
import com.peaceman.alpha.network.ShieldBubbleSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Verwaltet den clientseitigen Lebenszyklus aller sichtbaren Schiffe.
 * Dient als View-Cache für Rendering-Systeme und verhindert VRAM-Leaks.
 */
@EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientShipManager {

    private static final Map<UUID, ClientShipState> ACTIVE_CLIENT_SHIPS = new ConcurrentHashMap<>();
    private static final Map<ChunkPos, List<ShieldBubbleSyncPacket>> PENDING_SYNCS = new ConcurrentHashMap<>();

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

    public static void updateShipState(UUID shipId, int currentEnergy, boolean isShieldActive,
                                        long shieldCooldownTicks, long movementCooldownTicks) {
        ClientShipState shipState = getOrCreateShip(shipId);
        shipState.setShieldActive(isShieldActive);
        if (currentEnergy > 0) {
            shipState.setShieldEnergyPercentage(Math.min(1.0f, (float) currentEnergy / 1000000.0f));
        }
        long clientTick = net.minecraft.client.Minecraft.getInstance().level != null
                ? net.minecraft.client.Minecraft.getInstance().level.getGameTime() : 0L;
        shipState.updateCooldowns(shieldCooldownTicks, movementCooldownTicks, clientTick);
    }

    public static void updateShipPosition(UUID shipId, BlockPos newAnchorPos) {
        ClientShipState shipState = getShip(shipId);
        if (shipState != null) {
            shipState.setAnchorPos(newAnchorPos);
        }
    }

    public static void updateShipDimension(UUID shipId, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
        net.minecraft.world.level.Level clientLevel = net.minecraft.client.Minecraft.getInstance().level;
        if (clientLevel != null && dimension != null && !dimension.equals(clientLevel.dimension())) {
            // Schiff hat Dimension verlassen -> VBOs sofort freigeben
            removeShip(shipId);
        } else {
            ClientShipState shipState = getShip(shipId);
            if (shipState != null) {
                shipState.setDimension(dimension);
            }
        }
    }

    public static void clearAllVBOs() {
        clear();
    }

    public static void addImpact(UUID shipId, Vec3 localPos) {
        ClientShipState shipState = getShip(shipId);
        if (shipState != null) {
            long clientTick = net.minecraft.client.Minecraft.getInstance().level != null
                    ? net.minecraft.client.Minecraft.getInstance().level.getGameTime() : 0L;
            shipState.addImpact(localPos, clientTick);
        }
    }

    public static void removeStructureBlocks(UUID shipId, List<BlockPos> removedBlocks) {
        ClientShipState shipState = getShip(shipId);
        if (shipState != null && removedBlocks != null) {
            shipState.removeStructureBlocks(removedBlocks);
        }
    }

    public static void addPendingSync(ShieldBubbleSyncPacket packet) {
        if (packet == null || packet.anchorPos() == null) return;
        ChunkPos chunkPos = new ChunkPos(packet.anchorPos());
        ShieldLifecycleLogger.logClientPendingSyncQueued(packet.shipId(), chunkPos);
        PENDING_SYNCS.computeIfAbsent(chunkPos, k -> new CopyOnWriteArrayList<>()).add(packet);
    }

    public static void removeShip(UUID shipId) {
        ClientShipState removed = ACTIVE_CLIENT_SHIPS.remove(shipId);
        if (removed != null) {
            ShieldLifecycleLogger.logClientVramDisposed(shipId, "Manuelles removeShip()");
            removed.dispose();
        }
        ClientLaserState.removeBeamsForShip(shipId);
    }

    public static void clear() {
        ShieldLifecycleLogger.logClientReset("Client-Logout / Dimension-Wechsel");
        for (ClientShipState state : ACTIVE_CLIENT_SHIPS.values()) {
            state.dispose();
        }
        ACTIVE_CLIENT_SHIPS.clear();
        PENDING_SYNCS.clear();
        ClientLaserState.clearAll();
    }

    /**
     * Blueprint 1: Wendet zwischengespeicherte Schild-Pakete an, sobald der Vanilla-Chunk geladen wird.
     */
    @SubscribeEvent
    public static void onClientChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() == null || !event.getLevel().isClientSide()) return;
        ChunkPos loadedChunk = event.getChunk().getPos();
        List<ShieldBubbleSyncPacket> pendingList = PENDING_SYNCS.remove(loadedChunk);
        if (pendingList != null) {
            for (ShieldBubbleSyncPacket packet : pendingList) {
                ShieldLifecycleLogger.logClientPendingSyncApplied(packet.shipId(), loadedChunk);
                updateShieldBubble(packet.shipId(), packet.anchorPos(), packet.relativeBubbleBlocks());
            }
        }
    }

    /**
     * Blueprint 2: Gibt native OpenGL-Ressourcen (VBOs) frei, wenn Chunks entladen werden.
     */
    @SubscribeEvent
    public static void onClientChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() == null || !event.getLevel().isClientSide()) return;
        ChunkPos unloadedChunk = event.getChunk().getPos();

        for (ClientShipState ship : ACTIVE_CLIENT_SHIPS.values()) {
            if (ship.getAnchorPos() != null && new ChunkPos(ship.getAnchorPos()).equals(unloadedChunk)) {
                ShieldLifecycleLogger.logClientVramDisposed(ship.getShipId(), "ChunkEvent.Unload fuer Chunk " + unloadedChunk);
                ship.dispose();
                ACTIVE_CLIENT_SHIPS.remove(ship.getShipId());
            }
        }
        PENDING_SYNCS.remove(unloadedChunk);
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        clear();
    }
}
