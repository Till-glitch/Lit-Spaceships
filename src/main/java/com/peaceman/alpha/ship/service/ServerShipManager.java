package com.peaceman.alpha.ship.service;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.ISpaceshipNode;
import com.peaceman.alpha.block.entity.SpaceshipControlBlockEntity;
import com.peaceman.alpha.helper.ShieldLifecycleLogger;
import com.peaceman.alpha.network.ShieldBubbleSyncPacket;
import com.peaceman.alpha.network.ShipDimensionSyncPayload;
import com.peaceman.alpha.network.ShipStateSyncPayload;
import com.peaceman.alpha.network.ShipStructureSyncPayload;
import com.peaceman.alpha.ship.ShieldMorphology;
import com.peaceman.alpha.ship.ShipSavedData;
import com.peaceman.alpha.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Koordiniert den mehrdimensionalen Lebenszyklus aller Schiffe auf dem Server (CRUD)
 * und handhabt gezieltes dimensionenspezifisches Spatial Hashing via ChunkWatchEvent.Sent.
 */
@EventBusSubscriber(modid = Alpha.MODID)
public class ServerShipManager {

    public static final Map<UUID, ShipState> ACTIVE_SHIPS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Map<UUID, ShipState>> SHIPS_BY_DIMENSION = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ShipSavedData.get(event.getServer().overworld());
        Alpha.LOGGER.info("Spaceships loaded: {} across {} dimensions", ACTIVE_SHIPS.size(), SHIPS_BY_DIMENSION.size());
    }

    public static ShipState getShip(UUID shipId) {
        if (shipId == null)
            return null;
        return ACTIVE_SHIPS.get(shipId);
    }

    public static boolean hasShip(UUID shipId) {
        return shipId != null && ACTIVE_SHIPS.containsKey(shipId);
    }

    public static Map<UUID, ShipState> getShipsInDimension(ResourceKey<Level> dimension) {
        if (dimension == null) return ConcurrentHashMap.newKeySet().stream().collect(ConcurrentHashMap::new, (m, v) -> {}, (m1, m2) -> {});
        return SHIPS_BY_DIMENSION.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>());
    }

    public static void registerShip(ShipState ship) {
        if (ship == null) return;
        ACTIVE_SHIPS.put(ship.getId(), ship);
        getShipsInDimension(ship.getDimension()).put(ship.getId(), ship);
    }

    public static void unregisterShip(ShipState ship) {
        if (ship == null) return;
        ACTIVE_SHIPS.remove(ship.getId());
        getShipsInDimension(ship.getDimension()).remove(ship.getId());
    }

    public static void changeShipDimension(Level level, ShipState ship, ResourceKey<Level> newDimension) {
        if (ship == null || newDimension == null) return;
        ResourceKey<Level> oldDim = ship.getDimension();
        getShipsInDimension(oldDim).remove(ship.getId());
        ship.setDimension(newDimension);
        getShipsInDimension(newDimension).put(ship.getId(), ship);
        saveData(level);
        PacketDistributor.sendToAllPlayers(new ShipDimensionSyncPayload(ship.getId(), newDimension));
    }

    public static ShipState createShip(Level level, BlockPos startPos) {
        if (level.getBlockEntity(startPos) instanceof SpaceshipControlBlockEntity be) {
            if (be.getShipId() != null && ACTIVE_SHIPS.containsKey(be.getShipId())) {
                return null;
            }

            Set<BlockPos> shipBlocks = ShipScannerService.scan(level, startPos);
            ShipState newShip = new ShipState(startPos, shipBlocks, level.dimension());
            newShip.setBlocks(shipBlocks, level);

            registerShip(newShip);

            for (BlockPos pos : shipBlocks) {
                BlockEntity entityAtPos = level.getBlockEntity(pos);
                if (entityAtPos instanceof ISpaceshipNode node) {
                    node.setShipId(newShip.getId());
                    entityAtPos.setChanged();
                    level.sendBlockUpdated(pos, entityAtPos.getBlockState(), entityAtPos.getBlockState(), 3);
                }
            }

            saveData(level);
            PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(newShip.getId(), 0, newShip.isShieldActive(), 0L, 0L));
            PacketDistributor.sendToAllPlayers(new ShipDimensionSyncPayload(newShip.getId(), level.dimension()));
            return newShip;
        }
        return null;
    }

    public static void updateShipBlocks(Level level, ShipState ship) {
        if (ship != null) {
            Set<BlockPos> newBlocks = ShipScannerService.scan(level, ship.getControllerPos());
            ship.setBlocks(newBlocks, level);

            for (BlockPos pos : newBlocks) {
                if (level.getBlockEntity(pos) instanceof ISpaceshipNode node) {
                    node.setShipId(ship.getId());
                }
            }
            saveData(level);
            PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(ship.getId(), 0, ship.isShieldActive(),
                    ship.getShieldCooldownRemaining(level.getGameTime()),
                    ship.getMovementCooldownRemaining(level.getGameTime())));
        }
    }

    public static void deleteShip(Level level, ShipState ship) {
        if (ship != null) {
            for (BlockPos pos : ship.getBlocks()) {
                if (level.getBlockEntity(pos) instanceof ISpaceshipNode node) {
                    node.setShipId(null);
                }
            }
            unregisterShip(ship);
            saveData(level);
        }
    }

    public static void saveData(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            ShipSavedData.get(serverLevel).setDirty();
        }
    }

    /**
     * Spatial Hashing: Synchronisiert Schiffsdaten zielgerichtet an Spieler,
     * wenn diese einen Chunk betreten bzw. geladen bekommen – strikt dimensionenspezifisch.
     */
    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        ServerPlayer player = event.getPlayer();
        ChunkPos chunkPos = event.getPos();
        ResourceKey<Level> chunkDimension = event.getLevel().dimension();

        for (ShipState ship : getShipsInDimension(chunkDimension).values()) {
            boolean hasBlockInChunk = false;
            for (BlockPos pos : ship.getBlocks()) {
                if (SectionPos.blockToSectionCoord(pos.getX()) == chunkPos.x
                        && SectionPos.blockToSectionCoord(pos.getZ()) == chunkPos.z) {
                    hasBlockInChunk = true;
                    break;
                }
            }

            if (hasBlockInChunk) {
                BlockPos ctrl = ship.getControllerPos();
                Set<BlockPos> relative = new HashSet<>(ship.getBlocks().size());
                for (BlockPos b : ship.getBlocks()) {
                    relative.add(b.subtract(ctrl));
                }

                ShieldLifecycleLogger.logServerChunkSent(ship.getId(), ctrl, chunkPos, player.getName().getString());
                PacketDistributor.sendToPlayer(player, new ShipDimensionSyncPayload(ship.getId(), ship.getDimension()));
                PacketDistributor.sendToPlayer(player, new ShipStructureSyncPayload(ship.getId(), ctrl, relative));
                PacketDistributor.sendToPlayer(player,
                        new ShipStateSyncPayload(ship.getId(), 0, ship.isShieldActive(),
                                ship.getShieldCooldownRemaining(player.serverLevel().getGameTime()),
                                ship.getMovementCooldownRemaining(player.serverLevel().getGameTime())));
                if (!ship.getShields().isEmpty()) {
                    Set<BlockPos> bubble = ShieldMorphology.calculateShieldBubble(ship.getBlocks(), 5);
                    Set<BlockPos> relBubble = new HashSet<>(bubble.size());
                    for (BlockPos bp : bubble) {
                        relBubble.add(bp.subtract(ctrl));
                    }
                    PacketDistributor.sendToPlayer(player, new ShieldBubbleSyncPacket(ship.getId(), ctrl, relBubble));
                }
            }
        }
    }
}
