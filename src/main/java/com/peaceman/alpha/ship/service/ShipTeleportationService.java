package com.peaceman.alpha.ship.service;

import com.peaceman.alpha.block.ISpaceshipNode;
import com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity;
import com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity;
import com.peaceman.alpha.block.entity.SpaceshipShieldBlockEntity;
import com.peaceman.alpha.network.ShipDimensionSyncPayload;
import com.peaceman.alpha.network.ShipPositionSyncPayload;
import com.peaceman.alpha.network.ShipStateSyncPayload;
import com.peaceman.alpha.network.ShipStructureSyncPayload;
import com.peaceman.alpha.ship.SpaceshipEnergyManager;
import com.peaceman.alpha.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * Service für sichere, transaktionale und cross-dimensionale Schiffsteleportation
 * über alle 6 definierten Phasen (Suspendierung, Forceloading, Serialisierung,
 * Exzision, Materialisierung, Entitäts-Transfer).
 */
public class ShipTeleportationService {

    public record BlockData(BlockState state, CompoundTag nbt) {}

    public static boolean teleportShip(ServerLevel originLevel, ServerLevel targetLevel, ShipState ship, BlockPos targetControllerPos, Player initiator) {
        if (originLevel == null || targetLevel == null || ship == null || targetControllerPos == null) {
            return false;
        }
        if (ship.isJumping()) {
            return false;
        }

        // 1. Phase: Suspendierung
        ship.setJumping(true);

        BlockPos currentCtrl = ship.getControllerPos();
        int dx = targetControllerPos.getX() - currentCtrl.getX();
        int dy = targetControllerPos.getY() - currentCtrl.getY();
        int dz = targetControllerPos.getZ() - currentCtrl.getZ();

        // 2. Phase: Forceloading & Chunk-Tickets (Ursprung und Ziel)
        Set<ChunkPos> originChunks = new HashSet<>();
        for (BlockPos pos : ship.getBlocks()) {
            originChunks.add(new ChunkPos(pos));
        }

        Set<ChunkPos> targetChunks = new HashSet<>();
        for (BlockPos pos : ship.getBlocks()) {
            targetChunks.add(new ChunkPos(pos.offset(dx, dy, dz)));
        }

        for (ChunkPos cp : originChunks) {
            originLevel.getChunkSource().addRegionTicket(ShipMovementService.SHIP_TICKET, cp, 2, cp);
        }
        for (ChunkPos cp : targetChunks) {
            targetLevel.getChunkSource().addRegionTicket(ShipMovementService.SHIP_TICKET, cp, 2, cp);
        }

        try {
            // 3. Phase: Serialisierung (In-Memory Clipboard mit Data Components / NBT)
            Map<BlockPos, BlockData> clipboard = new HashMap<>(ship.getBlocks().size());
            for (BlockPos pos : ship.getBlocks()) {
                BlockState state = originLevel.getBlockState(pos);
                CompoundTag nbt = null;
                BlockEntity be = originLevel.getBlockEntity(pos);
                if (be != null) {
                    nbt = be.saveWithFullMetadata(originLevel.registryAccess());
                }
                clipboard.put(pos, new BlockData(state, nbt));
            }

            // 4. Phase: Exzision (Ursprungsort sauber entfernen ohne Drops)
            for (BlockPos pos : ship.getBlocks()) {
                originLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 50);
            }

            // 5. Phase: Materialisierung am Zielort
            Set<BlockPos> newBlocks = new HashSet<>(clipboard.size());
            List<BlockPos> newReactors = new ArrayList<>();
            List<BlockPos> newShields = new ArrayList<>();
            List<BlockPos> newWeapons = new ArrayList<>();

            for (Map.Entry<BlockPos, BlockData> entry : clipboard.entrySet()) {
                BlockPos oldPos = entry.getKey();
                BlockPos newPos = oldPos.offset(dx, dy, dz);
                newBlocks.add(newPos);

                BlockState state = entry.getValue().state();
                CompoundTag nbt = entry.getValue().nbt();

                targetLevel.setBlock(newPos, state, 50);

                if (nbt != null) {
                    nbt.putInt("x", newPos.getX());
                    nbt.putInt("y", newPos.getY());
                    nbt.putInt("z", newPos.getZ());
                    BlockEntity newBe = BlockEntity.loadStatic(newPos, state, nbt, targetLevel.registryAccess());
                    if (newBe != null) {
                        targetLevel.setBlockEntity(newBe);
                    }
                }

                BlockEntity placedBe = targetLevel.getBlockEntity(newPos);
                if (placedBe instanceof ISpaceshipNode node) {
                    node.setShipId(ship.getId());
                }
                if (placedBe instanceof SpaceshipReactorBlockEntity) {
                    newReactors.add(newPos);
                }
                if (placedBe instanceof SpaceshipShieldBlockEntity) {
                    newShields.add(newPos);
                }
                if (placedBe instanceof AbstractLaserNodeBlockEntity) {
                    newWeapons.add(newPos);
                }
            }

            // State & Dimension Update
            ship.setControllerPos(targetControllerPos);
            ship.setBlocks(newBlocks, targetLevel);
            ship.setReactors(newReactors);
            ship.setShields(newShields);
            ship.setWeapons(newWeapons);
            ship.recalculateHullBounds();

            ServerShipManager.changeShipDimension(targetLevel, ship, targetLevel.dimension());

            // 6. Phase: Entitäts- & Passagier-Transfer
            AABB originBox = ship.getTotalBoundingBox().move(-dx, -dy, -dz).inflate(1.0);
            List<Entity> entities = originLevel.getEntities((Entity) null, originBox, e -> true);

            for (Entity entity : entities) {
                Vec3 oldPos = entity.position();
                Vec3 newEntityPos = oldPos.add(dx, dy, dz);

                if (originLevel.equals(targetLevel)) {
                    entity.teleportTo(newEntityPos.x, newEntityPos.y, newEntityPos.z);
                } else {
                    DimensionTransition transition = new DimensionTransition(
                            targetLevel, newEntityPos, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING
                    );
                    entity.changeDimension(transition);
                }
            }

            // Sync an alle Clients
            Set<BlockPos> relativeBlocks = new HashSet<>(newBlocks.size());
            for (BlockPos b : newBlocks) {
                relativeBlocks.add(b.subtract(targetControllerPos));
            }

            PacketDistributor.sendToAllPlayers(new ShipPositionSyncPayload(ship.getId(), targetControllerPos));
            PacketDistributor.sendToAllPlayers(new ShipDimensionSyncPayload(ship.getId(), targetLevel.dimension()));
            PacketDistributor.sendToAllPlayers(new ShipStructureSyncPayload(ship.getId(), targetControllerPos, relativeBlocks));
            PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(
                    ship.getId(),
                    SpaceshipEnergyManager.getTotalAvailableEnergy(targetLevel, ship),
                    ship.isShieldActive(),
                    ship.getShieldCooldownRemaining(targetLevel.getGameTime()),
                    ship.getMovementCooldownRemaining(targetLevel.getGameTime())
            ));

            return true;

        } finally {
            // Freigabe der Chunk-Tickets
            for (ChunkPos cp : originChunks) {
                originLevel.getChunkSource().removeRegionTicket(ShipMovementService.SHIP_TICKET, cp, 2, cp);
            }
            for (ChunkPos cp : targetChunks) {
                targetLevel.getChunkSource().removeRegionTicket(ShipMovementService.SHIP_TICKET, cp, 2, cp);
            }

            ship.setJumping(false);
            ServerShipManager.saveData(targetLevel);
        }
    }
}
