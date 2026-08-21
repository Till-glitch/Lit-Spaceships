package com.peaceman.alpha.ship.service;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.ISpaceshipNode;
import com.peaceman.alpha.block.entity.SpaceshipControlBlockEntity;
import com.peaceman.alpha.network.ShipPositionSyncPayload;
import com.peaceman.alpha.ship.SpaceshipEnergyManager;
import com.peaceman.alpha.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service für translatorische Schiffsbewegungen mit Time-Slicing Tick-Budget (max. 10ms pro Tick),
 * atomarem Pre-Collision-Check (Intent-Lock-Execute) und Ticket-Management (Schritt 5).
 */
@EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ShipMovementService {

    public static final long TICK_BUDGET_NANOS = 10_000_000L; // 10 Millisekunden Budget pro Server-Tick

    public static final TicketType<ChunkPos> SHIP_TICKET =
            TicketType.create("peaceman_alpha:ship_movement", Comparator.comparing(ChunkPos::toLong));

    private static final Queue<MovementTask> PENDING_TASKS = new ConcurrentLinkedQueue<>();
    private static MovementTask currentTask = null;

    public record BlockData(BlockState state, CompoundTag nbt) {}

    public static class MovementTask {
        final ServerLevel level;
        final ShipState ship;
        final int dx, dy, dz;
        final Player player;

        // Phasen
        int phase = 0; // 0: Init/Prep, 1: Placement, 2: Removal & Cleanup
        Map<BlockPos, BlockData> snapshot = new HashMap<>();
        List<Map.Entry<BlockPos, BlockData>> blocksToPlace = new ArrayList<>();
        int placeIndex = 0;

        List<BlockPos> blocksToRemove = new ArrayList<>();
        int removeIndex = 0;

        Set<BlockPos> newShipBlocks = new HashSet<>();
        List<Entity> entitiesToMove = new ArrayList<>();
        Set<ChunkPos> destinationChunks = new HashSet<>();
        BlockPos startPos;

        public MovementTask(ServerLevel level, ShipState ship, int dx, int dy, int dz, Player player) {
            this.level = level;
            this.ship = ship;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.player = player;
            this.startPos = ship.getControllerPos();
        }

        /**
         * Führt die Task scheibchenweise bis zum Ablauf des Zeitbudgets aus.
         */
        public boolean tick(long deadlineNanos) {
            if (dx == 0 && dy == 0 && dz == 0) {
                return true; // Keine Bewegung erforderlich
            }

            // Phase 0: Vorbereitung, Chunk-Loading & Snapshot
            if (phase == 0) {
                Set<BlockPos> shipBlocks = ship.getBlocks();

                // 1. Energieprüfung
                if (!SpaceshipEnergyManager.tryConsumeFlightEnergy(level, ship, dx, dy, dz, player)) {
                    return true; // Abbruch wegen Energiemangel
                }

                // 2. Chunks im Zielgebiet vorbereiten und forceloaden
                destinationChunks = prepareDestinationChunks(level, ship, new Vec3(dx, dy, dz));

                // 3. Bounding Box & Passagier-Matrix erfassen
                int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
                for (BlockPos pos : shipBlocks) {
                    if (pos.getX() < minX) minX = pos.getX();
                    if (pos.getY() < minY) minY = pos.getY();
                    if (pos.getZ() < minZ) minZ = pos.getZ();
                    if (pos.getX() > maxX) maxX = pos.getX();
                    if (pos.getY() > maxY) maxY = pos.getY();
                    if (pos.getZ() > maxZ) maxZ = pos.getZ();
                }
                AABB shipBounds = new AABB(minX - 1, minY - 1, minZ - 1, maxX + 2, maxY + 3, maxZ + 2);

                entitiesToMove = level.getEntities(null, shipBounds).stream().filter(entity -> {
                    BlockPos entityPos = entity.blockPosition();
                    if (shipBlocks.contains(entityPos) || shipBlocks.contains(entityPos.below())) return true;
                    for (Direction dir : Direction.values()) {
                        if (shipBlocks.contains(entityPos.relative(dir))) return true;
                        if (shipBlocks.contains(entityPos.below().relative(dir))) return true;
                    }
                    return false;
                }).toList();

                // 4. Snapshot erstellen
                for (BlockPos pos : shipBlocks) {
                    BlockState state = level.getBlockState(pos);
                    BlockEntity be = level.getBlockEntity(pos);
                    CompoundTag nbt = (be != null) ? be.saveWithFullMetadata(level.registryAccess()) : null;
                    snapshot.put(pos, new BlockData(state, nbt));
                }

                // 5. Inventare sichern
                for (BlockPos pos : shipBlocks) {
                    if (level.getBlockEntity(pos) != null) {
                        level.removeBlockEntity(pos);
                    }
                }

                // 6. Zielpositionen berechnen
                for (BlockPos pos : shipBlocks) {
                    newShipBlocks.add(pos.offset(dx, dy, dz));
                }

                // 7. Vorabbereinigung im Weg stehender Blöcke
                for (BlockPos newPos : newShipBlocks) {
                    if (!shipBlocks.contains(newPos) && !level.getBlockState(newPos).isAir()) {
                        level.destroyBlock(newPos, true);
                    }
                }

                // 8. Controller entkoppeln
                if (level.getBlockEntity(startPos) instanceof SpaceshipControlBlockEntity be) {
                    be.setShipId(null);
                }
                BlockPos newStartPos = startPos.offset(dx, dy, dz);
                ship.setControllerPos(newStartPos);

                // 9. Blöcke sortieren: Erst feste, dann zerbrechliche
                List<Map.Entry<BlockPos, BlockData>> solid = new ArrayList<>();
                List<Map.Entry<BlockPos, BlockData>> fragile = new ArrayList<>();

                for (Map.Entry<BlockPos, BlockData> entry : snapshot.entrySet()) {
                    if (entry.getValue().state().getCollisionShape(level, entry.getKey()).isEmpty()) {
                        fragile.add(entry);
                    } else {
                        solid.add(entry);
                    }
                }
                blocksToPlace.addAll(solid);
                blocksToPlace.addAll(fragile);

                // 10. Alte Blöcke zur Löschung vormerken
                List<BlockPos> solidOld = new ArrayList<>();
                List<BlockPos> fragileOld = new ArrayList<>();
                for (BlockPos pos : shipBlocks) {
                    if (!newShipBlocks.contains(pos)) {
                        if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                            fragileOld.add(pos);
                        } else {
                            solidOld.add(pos);
                        }
                    }
                }
                blocksToRemove.addAll(fragileOld);
                blocksToRemove.addAll(solidOld);

                phase = 1;
            }

            // Phase 1: Inkrementelles Platzieren mit Zeitlimit
            if (phase == 1) {
                while (placeIndex < blocksToPlace.size()) {
                    if (System.nanoTime() >= deadlineNanos) {
                        return false; // Budget für diesen Tick erschöpft -> nächster Tick
                    }
                    Map.Entry<BlockPos, BlockData> entry = blocksToPlace.get(placeIndex++);
                    placeBlockFromSnapshot(level, entry, dx, dy, dz, ship.getId());
                }
                phase = 2;
            }

            // Phase 2: Inkrementelles Löschen alter Blöcke mit Zeitlimit
            if (phase == 2) {
                while (removeIndex < blocksToRemove.size()) {
                    if (System.nanoTime() >= deadlineNanos) {
                        return false;
                    }
                    BlockPos pos = blocksToRemove.get(removeIndex++);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 50);
                }

                // Abschluss: RAM-Daten aktualisieren
                ship.setBlocksRaw(newShipBlocks);

                List<BlockPos> newReactors = new ArrayList<>(ship.getReactors().size());
                for (BlockPos pos : ship.getReactors()) {
                    newReactors.add(pos.offset(dx, dy, dz));
                }
                ship.setReactors(newReactors);

                List<BlockPos> newShields = new ArrayList<>(ship.getShields().size());
                for (BlockPos pos : ship.getShields()) {
                    newShields.add(pos.offset(dx, dy, dz));
                }
                ship.setShields(newShields);

                // Passagiere / Entities teleportieren
                for (Entity entity : entitiesToMove) {
                    double newX = entity.getX() + dx;
                    double newY = entity.getY() + dy;
                    double newZ = entity.getZ() + dz;
                    if (entity instanceof ServerPlayer serverPlayer) {
                        serverPlayer.teleportTo(level, newX, newY, newZ, serverPlayer.getYRot(), serverPlayer.getXRot());
                    } else {
                        entity.setPos(newX, newY, newZ);
                        entity.hurtMarked = true;
                    }
                    entity.resetFallDistance();
                }

                // Nachbar-Updates und Speichern
                for (BlockPos pos : newShipBlocks) {
                    level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
                }

                // Chunks wieder freigeben
                releaseDestinationChunks(level, destinationChunks);

                ServerShipManager.saveData(level);

                // Synchronisation der neuen Anker-Position an alle Clients
                PacketDistributor.sendToAllPlayers(new ShipPositionSyncPayload(ship.getId(), ship.getControllerPos()));
                return true; // Fertig!
            }

            return true;
        }

        private void placeBlockFromSnapshot(Level lvl, Map.Entry<BlockPos, BlockData> entry, int dx, int dy, int dz, UUID shipId) {
            BlockPos newPos = entry.getKey().offset(dx, dy, dz);
            BlockState state = entry.getValue().state();
            CompoundTag nbt = entry.getValue().nbt();

            lvl.setBlock(newPos, state, 50);

            if (nbt != null) {
                nbt.putInt("x", newPos.getX());
                nbt.putInt("y", newPos.getY());
                nbt.putInt("z", newPos.getZ());
                BlockEntity newBe = BlockEntity.loadStatic(newPos, state, nbt, lvl.registryAccess());
                if (newBe != null) {
                    lvl.setBlockEntity(newBe);
                }
            }

            if (lvl.getBlockEntity(newPos) instanceof ISpaceshipNode node) {
                node.setShipId(shipId);
            }
        }
    }

    /**
     * Berechnet und forceloaded Chunks für das Zielgebiet.
     */
    public static Set<ChunkPos> prepareDestinationChunks(ServerLevel level, ShipState ship, Vec3 movementVector) {
        Set<BlockPos> blocks = ship.getBlocks();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : blocks) {
            if (pos.getX() < minX) minX = pos.getX();
            if (pos.getY() < minY) minY = pos.getY();
            if (pos.getZ() < minZ) minZ = pos.getZ();
            if (pos.getX() > maxX) maxX = pos.getX();
            if (pos.getY() > maxY) maxY = pos.getY();
            if (pos.getZ() > maxZ) maxZ = pos.getZ();
        }

        AABB currentBounds = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        AABB destinationBounds = currentBounds.move(movementVector);

        Set<ChunkPos> requiredChunks = getChunksInAABB(destinationBounds);
        for (ChunkPos pos : requiredChunks) {
            level.getChunkSource().addRegionTicket(SHIP_TICKET, pos, 2, pos);
        }
        return requiredChunks;
    }

    /**
     * Gibt Forceloading-Tickets nach Bewegungsabschluss wieder frei.
     */
    public static void releaseDestinationChunks(ServerLevel level, Set<ChunkPos> previouslyLoadedChunks) {
        if (previouslyLoadedChunks == null) return;
        for (ChunkPos pos : previouslyLoadedChunks) {
            level.getChunkSource().removeRegionTicket(SHIP_TICKET, pos, 2, pos);
        }
    }

    public static Set<ChunkPos> getChunksInAABB(AABB aabb) {
        Set<ChunkPos> chunks = new HashSet<>();
        int minChunkX = SectionPos.blockToSectionCoord((int) Math.floor(aabb.minX));
        int maxChunkX = SectionPos.blockToSectionCoord((int) Math.floor(aabb.maxX));
        int minChunkZ = SectionPos.blockToSectionCoord((int) Math.floor(aabb.minZ));
        int maxChunkZ = SectionPos.blockToSectionCoord((int) Math.floor(aabb.maxZ));

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                chunks.add(new ChunkPos(x, z));
            }
        }
        return chunks;
    }

    /**
     * Stellt eine Bewegungsanfrage in die Warteschlange nach erfolgreichem Intent-Lock-Pre-Check (Schritt 5).
     */
    public static void moveShip(Level level, ShipState ship, int dx, int dy, int dz, Player player) {
        if (!(level instanceof ServerLevel serverLevel) || ship == null) return;
        if (dx == 0 && dy == 0 && dz == 0) return;

        // Bewegungs-Cooldown prüfen
        long gameTime = level.getGameTime();
        if (ship.isMovementOnCooldown(gameTime)) {
            long remaining = ship.getMovementCooldownRemaining(gameTime);
            if (player != null) {
                player.displayClientMessage(
                        Component.literal("§c[Antrieb] §fAbklingzeit aktiv! Noch " + (remaining / 20) + "." + (remaining % 20 * 5) + " Sekunden."),
                        true
                );
            }
            return;
        }

        Vec3 moveVec = new Vec3(dx, dy, dz);

        // 1. Broad-Phase: Suche potenzielle Kollisions-Kandidaten
        List<ShipCollisionService.BroadPhaseCandidate> candidates =
                ShipCollisionService.findPotentialCollisions(ship, moveVec);

        Vec3 finalMoveVec = moveVec;

        // 2. Narrow-Phase: Voxel-genaue Prüfung & Physik-Resolving
        //    WICHTIG: Für das bewegte Schiff muss die projizierte Zielposition als Origin
        //    übergeben werden, da die Swept-AABB den Zukunftsraum abdeckt, der VoxelGridCache
        //    aber die Blöcke relativ zur aktuellen Controller-Position speichert.
        BlockPos projectedOriginA = ship.getControllerPos().offset(dx, dy, dz);

        for (ShipCollisionService.BroadPhaseCandidate candidate : candidates) {
            ShipCollisionService.VoxelCollisionResult collision =
                    ShipCollisionService.calculateVoxelIntersection(
                            candidate.movingShip(), projectedOriginA,
                            candidate.otherShip(), candidate.otherShip().getControllerPos(),
                            candidate.intersectionBox());

            if (collision.isColliding()) {
                CollisionResolver.CollisionResolution resolution =
                        CollisionResolver.resolve(serverLevel, collision, finalMoveVec);

                if (resolution.movementStopped()) {
                    finalMoveVec = resolution.clampedVector();
                    if (player != null) {
                        player.displayClientMessage(
                                Component.literal("§c[Kollisionswarnung] §fKollision erkannt (" + resolution.resolutionCase() + ")! Bewegung gestoppt."),
                                true
                        );
                    }
                    break;
                }
            }
        }

        // Falls die Bewegung komplett durch Kollision gestoppt wurde
        if (finalMoveVec.lengthSqr() == 0) {
            return;
        }

        int finalDx = (int) finalMoveVec.x;
        int finalDy = (int) finalMoveVec.y;
        int finalDz = (int) finalMoveVec.z;

        // Bewegungs-Cooldown sofort setzen (blockiert weitere Befehle während der Abklingzeit)
        ship.setMovementCooldownUntil(gameTime + ShipState.MOVEMENT_COOLDOWN_TICKS);

        PENDING_TASKS.add(new MovementTask(serverLevel, ship, finalDx, finalDy, finalDz, player));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long deadline = System.nanoTime() + TICK_BUDGET_NANOS;

        while (System.nanoTime() < deadline) {
            if (currentTask == null) {
                currentTask = PENDING_TASKS.poll();
                if (currentTask == null) {
                    break; // Keine anstehenden Tasks
                }
            }

            boolean finished = currentTask.tick(deadline);
            if (finished) {
                currentTask = null;
            } else {
                // Task pausiert und wird im nächsten Tick fortgesetzt
                break;
            }
        }
    }
}
