package com.peaceman.alpha.ship.service;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.ISpaceshipNode;
import com.peaceman.alpha.block.entity.SpaceshipControlBlockEntity;
import com.peaceman.alpha.network.ShieldBubbleSyncPacket;
import com.peaceman.alpha.network.ShipStateSyncPayload;
import com.peaceman.alpha.network.ShipStructureSyncPayload;
import com.peaceman.alpha.ship.ShieldMorphology;
import com.peaceman.alpha.ship.ShipSavedData;
import com.peaceman.alpha.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
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
 * Koordiniert den Lebenszyklus aller Schiffe auf dem Server (CRUD)
 * und handhabt gezieltes Spatial Hashing via ChunkWatchEvent.Sent.
 */
@EventBusSubscriber(modid = Alpha.MODID)
public class ServerShipManager {

    public static final Map<UUID, ShipState> ACTIVE_SHIPS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ShipSavedData.get(event.getServer().overworld());
        Alpha.LOGGER.info("Spaceships loaded: {}", ACTIVE_SHIPS.size());
    }

    public static ShipState getShip(UUID shipId) {
        if (shipId == null)
            return null;
        return ACTIVE_SHIPS.get(shipId);
    }

    public static ShipState createShip(Level level, BlockPos startPos) {
        if (level.getBlockEntity(startPos) instanceof SpaceshipControlBlockEntity be) {
            if (be.getShipId() != null && ACTIVE_SHIPS.containsKey(be.getShipId())) {
                return null;
            }

            Set<BlockPos> shipBlocks = ShipScannerService.scan(level, startPos);
            ShipState newShip = new ShipState(startPos, shipBlocks);
            newShip.setBlocks(shipBlocks, level);

            ACTIVE_SHIPS.put(newShip.getId(), newShip);

            for (BlockPos pos : shipBlocks) {
                BlockEntity entityAtPos = level.getBlockEntity(pos);
                if (entityAtPos instanceof ISpaceshipNode node) {
                    node.setShipId(newShip.getId());
                    entityAtPos.setChanged();
                    level.sendBlockUpdated(pos, entityAtPos.getBlockState(), entityAtPos.getBlockState(), 3);
                }
            }

            saveData(level);
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
        }
    }

    public static void deleteShip(Level level, ShipState ship) {
        if (ship != null) {
            for (BlockPos pos : ship.getBlocks()) {
                if (level.getBlockEntity(pos) instanceof ISpaceshipNode node) {
                    node.setShipId(null);
                }
            }
            ACTIVE_SHIPS.remove(ship.getId());
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
     * wenn diese einen Chunk betreten bzw. geladen bekommen.
     */
    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        ServerPlayer player = event.getPlayer();
        ChunkPos chunkPos = event.getPos();

        for (ShipState ship : ACTIVE_SHIPS.values()) {
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

                // Gezielte Synchronisation nur an diesen einen Spieler
                PacketDistributor.sendToPlayer(player, new ShipStructureSyncPayload(ship.getId(), ctrl, relative));
                PacketDistributor.sendToPlayer(player,
                        new ShipStateSyncPayload(ship.getId(), 0, ship.isShieldActive()));
                Alpha.LOGGER.info("Sending ship structure sync to player {}", player.getName());
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
