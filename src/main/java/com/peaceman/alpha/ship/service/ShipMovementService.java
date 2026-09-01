package com.peaceman.alpha.ship.service;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.ISpaceshipNode;
import com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity;
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

    public interface IShipTask {
        boolean tick(long deadlineNanos);
        ShipState getShip();
    }

    private static final Queue<IShipTask> PENDING_TASKS = new ConcurrentLinkedQueue<>();
    private static IShipTask currentTask = null;

    public static boolean isShipMoving(UUID shipId) {
        return currentTask != null && currentTask.getShip() != null && currentTask.getShip().getId().equals(shipId);
    }

    public record BlockData(BlockState state, CompoundTag nbt) {}
    public enum PlacementPass {
        PASS_1_SOLIDS,
        PASS_2_ROOTS_AND_NORMALS,
        PASS_3_ATTACHABLES_AND_TOPS
    }

    public static PlacementPass getPlacementPass(BlockState state, Level level, BlockPos pos) {
        if (state == null || state.isAir()) {
            return PlacementPass.PASS_2_ROOTS_AND_NORMALS;
        }

        // 1. Obere Multiblock-Hälften -> Pass 3
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) &&
                state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
            return PlacementPass.PASS_3_ATTACHABLES_AND_TOPS;
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART) &&
                state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD) {
            return PlacementPass.PASS_3_ATTACHABLES_AND_TOPS;
        }

        // 2. Anhängende & zerbrechliche Blöcke -> Pass 3
        net.minecraft.world.level.block.Block block = state.getBlock();
        if (block instanceof net.minecraft.world.level.block.TorchBlock ||
                block instanceof net.minecraft.world.level.block.RedStoneWireBlock ||
                block instanceof net.minecraft.world.level.block.DiodeBlock ||
                block instanceof net.minecraft.world.level.block.LeverBlock ||
                block instanceof net.minecraft.world.level.block.ButtonBlock ||
                block instanceof net.minecraft.world.level.block.LadderBlock ||
                block instanceof net.minecraft.world.level.block.SignBlock ||
                block instanceof net.minecraft.world.level.block.BannerBlock ||
                block instanceof net.minecraft.world.level.block.CarpetBlock ||
                block instanceof net.minecraft.world.level.block.BasePressurePlateBlock ||
                block instanceof net.minecraft.world.level.block.TripWireHookBlock ||
                block instanceof net.minecraft.world.level.block.TripWireBlock ||
                block instanceof net.minecraft.world.level.block.FlowerPotBlock) {
            return PlacementPass.PASS_3_ATTACHABLES_AND_TOPS;
        }

        // 3. Untere Multiblock-Wurzeln -> Pass 2
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) &&
                state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
            return PlacementPass.PASS_2_ROOTS_AND_NORMALS;
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART) &&
                state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART) == net.minecraft.world.level.block.state.properties.BedPart.FOOT) {
            return PlacementPass.PASS_2_ROOTS_AND_NORMALS;
        }

        // 4. Reine feste Vollblöcke (Fundamente) -> Pass 1
        if (level != null && state.isSolidRender(level, pos)) {
            return PlacementPass.PASS_1_SOLIDS;
        }
        if (state.isCollisionShapeFullBlock(level != null ? level : net.minecraft.world.level.EmptyBlockGetter.INSTANCE, pos)) {
            return PlacementPass.PASS_1_SOLIDS;
        }

        // 5. Alle übrigen (Maschinen, Reaktoren, Treppen, Stufen, Zäune, etc.) -> Pass 2
        return PlacementPass.PASS_2_ROOTS_AND_NORMALS;
    }

    public static boolean isFragileBlock(BlockState state, Level level, BlockPos pos) {
        return getPlacementPass(state, level, pos) == PlacementPass.PASS_3_ATTACHABLES_AND_TOPS;
    }

    public static class MovementTask implements IShipTask {
        final ServerLevel level;
        final ShipState ship;
        final int dx, dy, dz;
        final Player player;

        int phase = 0;
        Map<BlockPos, BlockData> snapshot = new HashMap<>();

        List<Map.Entry<BlockPos, BlockData>> pass1Blocks = new ArrayList<>();
        int pass1Index = 0;

        List<Map.Entry<BlockPos, BlockData>> pass2Blocks = new ArrayList<>();
        int pass2Index = 0;

        List<Map.Entry<BlockPos, BlockData>> pass3Blocks = new ArrayList<>();
        int pass3Index = 0;

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

        @Override
        public ShipState getShip() {
            return ship;
        }

        /**
         * Führt die Task scheibchenweise bis zum Ablauf des Zeitbudgets aus.
         */
        @Override
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

                // 5. Inventare sichern & BlockEntities vorab entfernen (verhindert Containers.dropContents)
                for (BlockPos pos : shipBlocks) {
                    if (level.getBlockEntity(pos) != null) {
                        level.removeBlockEntity(pos);
                    }
                }

                // 6. Zielpositionen berechnen
                for (BlockPos pos : shipBlocks) {
                    newShipBlocks.add(pos.offset(dx, dy, dz));
                }

                // 7. Vorabbereinigung im Weg stehender Weltblöcke
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

                // 9. Freigewordene alte Positionen (P_alt \ P_neu) zur Löschung vormerken
                for (BlockPos pos : shipBlocks) {
                    if (!newShipBlocks.contains(pos)) {
                        blocksToRemove.add(pos);
                    }
                }
                // Von oben nach unten löschen
                blocksToRemove.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

                // 10. Blöcke in 3 Pässe sortieren (jeweils aufsteigend nach Ziel-Y)
                List<Map.Entry<BlockPos, BlockData>> p1 = new ArrayList<>();
                List<Map.Entry<BlockPos, BlockData>> p2 = new ArrayList<>();
                List<Map.Entry<BlockPos, BlockData>> p3 = new ArrayList<>();

                for (Map.Entry<BlockPos, BlockData> entry : snapshot.entrySet()) {
                    PlacementPass pass = getPlacementPass(entry.getValue().state(), level, entry.getKey());
                    switch (pass) {
                        case PASS_1_SOLIDS -> p1.add(entry);
                        case PASS_2_ROOTS_AND_NORMALS -> p2.add(entry);
                        case PASS_3_ATTACHABLES_AND_TOPS -> p3.add(entry);
                    }
                }

                p1.sort(Comparator.comparingInt(e -> e.getKey().getY() + dy));
                p2.sort(Comparator.comparingInt(e -> e.getKey().getY() + dy));
                p3.sort(Comparator.comparingInt(e -> e.getKey().getY() + dy));

                pass1Blocks.addAll(p1);
                pass2Blocks.addAll(p2);
                pass3Blocks.addAll(p3);

                phase = 1;
            }

            // Phase 1: Freigewordene alte Blöcke (P_alt \ P_neu) mit Flag 48 zu AIR leeren
            if (phase == 1) {
                while (removeIndex < blocksToRemove.size()) {
                    if (System.nanoTime() >= deadlineNanos) {
                        return false; // Budget für diesen Tick erschöpft -> nächster Tick
                    }
                    BlockPos pos = blocksToRemove.get(removeIndex++);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 48);
                }
                phase = 2;
            }

            // Phase 2: Pass 1 (Solids) mit Flag 52 platzieren
            if (phase == 2) {
                while (pass1Index < pass1Blocks.size()) {
                    if (System.nanoTime() >= deadlineNanos) {
                        return false;
                    }
                    Map.Entry<BlockPos, BlockData> entry = pass1Blocks.get(pass1Index++);
                    placeBlockFromSnapshot(level, entry, dx, dy, dz, ship.getId(), 52);
                }
                phase = 3;
            }

            // Phase 3: Pass 2 (Roots & Normals) mit Flag 52 platzieren
            if (phase == 3) {
                while (pass2Index < pass2Blocks.size()) {
                    if (System.nanoTime() >= deadlineNanos) {
                        return false;
                    }
                    Map.Entry<BlockPos, BlockData> entry = pass2Blocks.get(pass2Index++);
                    placeBlockFromSnapshot(level, entry, dx, dy, dz, ship.getId(), 52);
                }
                phase = 4;
            }

            // Phase 4: Pass 3 (Attachables & Tops) mit Flag 52 platzieren
            if (phase == 4) {
                while (pass3Index < pass3Blocks.size()) {
                    if (System.nanoTime() >= deadlineNanos) {
                        return false;
                    }
                    Map.Entry<BlockPos, BlockData> entry = pass3Blocks.get(pass3Index++);
                    placeBlockFromSnapshot(level, entry, dx, dy, dz, ship.getId(), 52);
                }
                phase = 5;
            }

            // Phase 5: Abschluss & Synchronisation
            if (phase == 5) {
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

                List<BlockPos> newWeapons = new ArrayList<>(ship.getWeapons().size());
                for (BlockPos pos : ship.getWeapons()) {
                    newWeapons.add(pos.offset(dx, dy, dz));
                }
                ship.setWeapons(newWeapons);

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

                // Synchronisiertes Block-Update (Flag 50) und Nachbar-Updates
                for (BlockPos pos : newShipBlocks) {
                    BlockState state = level.getBlockState(pos);
                    level.sendBlockUpdated(pos, state, state, 50);
                    level.updateNeighborsAt(pos, state.getBlock());
                }
                for (BlockPos pos : blocksToRemove) {
                    level.sendBlockUpdated(pos, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), 50);
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

        private void placeBlockFromSnapshot(Level lvl, Map.Entry<BlockPos, BlockData> entry, int dx, int dy, int dz, UUID shipId, int flags) {
            BlockPos newPos = entry.getKey().offset(dx, dy, dz);
            BlockState state = entry.getValue().state();
            CompoundTag nbt = entry.getValue().nbt();

            lvl.setBlock(newPos, state, flags);

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

    public static class RotationTask implements IShipTask {
        final ServerLevel level;
        final ShipState ship;
        final net.minecraft.world.level.block.Rotation rotation;
        final Player player;

        int phase = 0;
        Map<BlockPos, BlockData> snapshot = new HashMap<>();

        List<Map.Entry<BlockPos, BlockData>> pass1Blocks = new ArrayList<>();
        int pass1Index = 0;

        List<Map.Entry<BlockPos, BlockData>> pass2Blocks = new ArrayList<>();
        int pass2Index = 0;

        List<Map.Entry<BlockPos, BlockData>> pass3Blocks = new ArrayList<>();
        int pass3Index = 0;

        List<BlockPos> blocksToRemove = new ArrayList<>();
        int removeIndex = 0;

        Set<BlockPos> newShipBlocks = new HashSet<>();
        List<Entity> entitiesToMove = new ArrayList<>();
        Set<ChunkPos> destinationChunks = new HashSet<>();
        BlockPos startPos;

        public RotationTask(ServerLevel level, ShipState ship, net.minecraft.world.level.block.Rotation rotation, Player player) {
            this.level = level;
            this.ship = ship;
            this.rotation = rotation;
            this.player = player;
            this.startPos = ship.getControllerPos();
        }

        @Override
        public ShipState getShip() {
            return ship;
        }

        @Override
        public boolean tick(long deadlineNanos) {
            if (rotation == null || rotation == net.minecraft.world.level.block.Rotation.NONE) {
                return true;
            }

            // Phase 0: Vorbereitung, Chunk-Loading & Snapshot
            if (phase == 0) {
                Set<BlockPos> shipBlocks = ship.getBlocks();

                // 1. Energieprüfung
                if (!SpaceshipEnergyManager.tryConsumeRotationEnergy(level, ship, rotation, player)) {
                    return true;
                }

                // 2. Zielpositionen berechnen und Chunks im Zielgebiet vorbereiten
                for (BlockPos pos : shipBlocks) {
                    newShipBlocks.add(ShipRotationMath.rotateAbsoluteBlockPos(pos, startPos, rotation));
                }

                destinationChunks = prepareDestinationChunksForBlocks(level, newShipBlocks, shipBlocks);

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

                // 5. Inventare sichern & BlockEntities vorab entfernen
                for (BlockPos pos : shipBlocks) {
                    if (level.getBlockEntity(pos) != null) {
                        level.removeBlockEntity(pos);
                    }
                }

                // 6. Vorabbereinigung im Weg stehender Weltblöcke
                for (BlockPos newPos : newShipBlocks) {
                    if (!shipBlocks.contains(newPos) && !level.getBlockState(newPos).isAir()) {
                        level.destroyBlock(newPos, true);
                    }
                }

                // 7. Controller entkoppeln
                if (level.getBlockEntity(startPos) instanceof SpaceshipControlBlockEntity be) {
                    be.setShipId(null);
                }

                // 8. Freigewordene alte Positionen (P_alt \ P_neu) zur Löschung vormerken
                for (BlockPos pos : shipBlocks) {
                    if (!newShipBlocks.contains(pos)) {
                        blocksToRemove.add(pos);
                    }
                }
                blocksToRemove.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

                // 9. Blöcke mit Rotation in 3 Pässe sortieren (jeweils aufsteigend nach Ziel-Y)
                List<Map.Entry<BlockPos, BlockData>> p1 = new ArrayList<>();
                List<Map.Entry<BlockPos, BlockData>> p2 = new ArrayList<>();
                List<Map.Entry<BlockPos, BlockData>> p3 = new ArrayList<>();

                for (Map.Entry<BlockPos, BlockData> entry : snapshot.entrySet()) {
                    BlockPos newPos = ShipRotationMath.rotateAbsoluteBlockPos(entry.getKey(), startPos, rotation);
                    BlockState rotState = entry.getValue().state().rotate(rotation);
                    Map.Entry<BlockPos, BlockData> rotEntry = Map.entry(newPos, new BlockData(rotState, entry.getValue().nbt()));

                    PlacementPass pass = getPlacementPass(rotState, level, newPos);
                    switch (pass) {
                        case PASS_1_SOLIDS -> p1.add(rotEntry);
                        case PASS_2_ROOTS_AND_NORMALS -> p2.add(rotEntry);
                        case PASS_3_ATTACHABLES_AND_TOPS -> p3.add(rotEntry);
                    }
                }

                p1.sort(Comparator.comparingInt(e -> e.getKey().getY()));
                p2.sort(Comparator.comparingInt(e -> e.getKey().getY()));
                p3.sort(Comparator.comparingInt(e -> e.getKey().getY()));

                pass1Blocks.addAll(p1);
                pass2Blocks.addAll(p2);
                pass3Blocks.addAll(p3);

                phase = 1;
            }

            // Phase 1: Freigewordene alte Blöcke (P_alt \ P_neu) mit Flag 48 zu AIR leeren
            if (phase == 1) {
                while (removeIndex < blocksToRemove.size()) {
                    if (System.nanoTime() >= deadlineNanos) {
                        return false;
                    }
                    BlockPos pos = blocksToRemove.get(removeIndex++);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 48);
                }
                phase = 2;
            }

            // Phase 2: Pass 1 (Solids) mit Flag 52 platzieren
            if (phase == 2) {
                while (pass1Index < pass1Blocks.size()) {
                    if (System.nanoTime() >= deadlineNanos) {
                        return false;
                    }
                    Map.Entry<BlockPos, BlockData> entry = pass1Blocks.get(pass1Index++);
                    placeRotatedBlock(level, entry, rotation, ship.getId(), 52);
                }
                phase = 3;
            }

            // Phase 3: Pass 2 (Roots & Normals) mit Flag 52 platzieren
            if (phase == 3) {
                while (pass2Index < pass2Blocks.size()) {
                    if (System.nanoTime() >= deadlineNanos) {
                        return false;
                    }
                    Map.Entry<BlockPos, BlockData> entry = pass2Blocks.get(pass2Index++);
                    placeRotatedBlock(level, entry, rotation, ship.getId(), 52);
                }
                phase = 4;
            }

            // Phase 4: Pass 3 (Attachables & Tops) mit Flag 52 platzieren
            if (phase == 4) {
                while (pass3Index < pass3Blocks.size()) {
                    if (System.nanoTime() >= deadlineNanos) {
                        return false;
                    }
                    Map.Entry<BlockPos, BlockData> entry = pass3Blocks.get(pass3Index++);
                    placeRotatedBlock(level, entry, rotation, ship.getId(), 52);
                }
                phase = 5;
            }

            // Phase 5: Abschluss & Synchronisation
            if (phase == 5) {
                // Abschluss: RAM-Daten aktualisieren
                ship.setBlocksRaw(newShipBlocks);

                List<BlockPos> newReactors = new ArrayList<>(ship.getReactors().size());
                for (BlockPos pos : ship.getReactors()) {
                    newReactors.add(ShipRotationMath.rotateAbsoluteBlockPos(pos, startPos, rotation));
                }
                ship.setReactors(newReactors);

                List<BlockPos> newShields = new ArrayList<>(ship.getShields().size());
                for (BlockPos pos : ship.getShields()) {
                    newShields.add(ShipRotationMath.rotateAbsoluteBlockPos(pos, startPos, rotation));
                }
                ship.setShields(newShields);

                List<BlockPos> newWeapons = new ArrayList<>(ship.getWeapons().size());
                for (BlockPos pos : ship.getWeapons()) {
                    newWeapons.add(ShipRotationMath.rotateAbsoluteBlockPos(pos, startPos, rotation));
                }
                ship.setWeapons(newWeapons);

                Map<String, BlockPos> newHomes = new HashMap<>();
                for (Map.Entry<String, BlockPos> home : ship.getHomes().entrySet()) {
                    newHomes.put(home.getKey(), ShipRotationMath.rotateAbsoluteBlockPos(home.getValue(), startPos, rotation));
                }
                ship.getHomes().clear();
                ship.getHomes().putAll(newHomes);

                // Schiffsorientierungs-Quaternion aktualisieren
                float rad = switch (rotation) {
                    case CLOCKWISE_90 -> (float) Math.toRadians(-90.0);
                    case COUNTERCLOCKWISE_90 -> (float) Math.toRadians(90.0);
                    case CLOCKWISE_180 -> (float) Math.toRadians(180.0);
                    default -> 0.0f;
                };
                if (rad != 0.0f) {
                    ship.getRotation().rotateY(rad);
                }

                // Passagiere / Entities teleportieren und POV rotieren
                for (Entity entity : entitiesToMove) {
                    Vec3 newPos = ShipRotationMath.rotateEntityPos(entity.position(), startPos, rotation);
                    float newYaw = ShipRotationMath.rotateYaw(entity.getYRot(), rotation);
                    if (entity instanceof ServerPlayer serverPlayer) {
                        serverPlayer.teleportTo(level, newPos.x, newPos.y, newPos.z, newYaw, serverPlayer.getXRot());
                    } else {
                        entity.setPos(newPos.x, newPos.y, newPos.z);
                        entity.setYRot(newYaw);
                        entity.hurtMarked = true;
                    }
                    entity.resetFallDistance();
                }

                // Synchronisiertes Block-Update (Flag 50) und Nachbar-Updates
                for (BlockPos pos : newShipBlocks) {
                    BlockState state = level.getBlockState(pos);
                    level.sendBlockUpdated(pos, state, state, 50);
                    level.updateNeighborsAt(pos, state.getBlock());
                }
                for (BlockPos pos : blocksToRemove) {
                    level.sendBlockUpdated(pos, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), 50);
                }

                // Chunks wieder freigeben
                releaseDestinationChunks(level, destinationChunks);

                ServerShipManager.saveData(level);

                // Client-Synchronisation
                Set<BlockPos> relativeBlocks = new HashSet<>(newShipBlocks.size());
                for (BlockPos p : newShipBlocks) {
                    relativeBlocks.add(p.subtract(startPos));
                }
                PacketDistributor.sendToAllPlayers(new com.peaceman.alpha.network.ShipStructureSyncPayload(ship.getId(), startPos, relativeBlocks));
                PacketDistributor.sendToAllPlayers(new ShipPositionSyncPayload(ship.getId(), startPos));

                if (ship.isShieldActive() && !ship.getShields().isEmpty()) {
                    ShipMorphologyService.calculateAndSyncShieldAsync(ship, level, com.peaceman.alpha.ship.SpaceshipShieldHandler.getShieldRadius(ship));
                }

                return true;
            }

            return true;
        }

        private void placeRotatedBlock(Level lvl, Map.Entry<BlockPos, BlockData> entry, net.minecraft.world.level.block.Rotation rot, UUID shipId, int flags) {
            BlockPos newPos = entry.getKey();
            BlockState state = entry.getValue().state();
            CompoundTag nbt = entry.getValue().nbt();

            lvl.setBlock(newPos, state, flags);

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

            if (lvl.getBlockEntity(newPos) instanceof AbstractLaserNodeBlockEntity laserBe) {
                laserBe.rotateTurret(rot);
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
        if (!(level instanceof ServerLevel serverLevel) || ship == null || ship.isJumping()) return;
        if (dx == 0 && dy == 0 && dz == 0) return;

        // Bewegungs-Cooldown prüfen
        long gameTime = level.getGameTime();
        if (ship.isMovementOnCooldown(gameTime)) {
            long remaining = ship.getMovementCooldownRemaining(gameTime);
            if (player != null) {
                String remainingSec = (remaining / 20) + "." + (remaining % 20 * 5);
                player.displayClientMessage(
                        Component.translatable(com.peaceman.alpha.registry.ModI18n.Message.MOVEMENT_COOLDOWN_ACTIVE, remainingSec),
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

        List<ShipCollisionService.VoxelCollisionResult> collisions = new ArrayList<>();
        for (ShipCollisionService.BroadPhaseCandidate candidate : candidates) {
            ShipCollisionService.VoxelCollisionResult collision =
                    ShipCollisionService.calculateVoxelIntersection(
                            candidate.movingShip(), projectedOriginA,
                            candidate.otherShip(), candidate.otherShip().getControllerPos(),
                            candidate.intersectionBox());

            if (collision.isColliding()) {
                collisions.add(collision);
            }
        }

        if (!collisions.isEmpty()) {
            CollisionResolver.CollisionResolution resolution =
                    CollisionResolver.resolveMultiple(serverLevel, collisions, finalMoveVec);

            if (resolution.movementStopped()) {
                finalMoveVec = resolution.clampedVector();
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(com.peaceman.alpha.registry.ModI18n.Message.COLLISION_WARNING, resolution.resolutionCase()),
                            true
                    );
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

    /**
     * Berechnet und forceloaded Chunks für beliebige Blockmengen (z.B. für Rotationen).
     */
    public static Set<ChunkPos> prepareDestinationChunksForBlocks(ServerLevel level, Set<BlockPos> blocksA, Set<BlockPos> blocksB) {
        Set<ChunkPos> requiredChunks = new HashSet<>();
        if (blocksA != null) {
            for (BlockPos pos : blocksA) {
                requiredChunks.add(new ChunkPos(pos));
            }
        }
        if (blocksB != null) {
            for (BlockPos pos : blocksB) {
                requiredChunks.add(new ChunkPos(pos));
            }
        }
        for (ChunkPos pos : requiredChunks) {
            level.getChunkSource().addRegionTicket(SHIP_TICKET, pos, 2, pos);
        }
        return requiredChunks;
    }

    /**
     * Führt eine orthogonale 90°-Schiffsrotation um den SpaceshipControlBlock durch (Pre-Collision Check & Time-Slicing).
     */
    public static void rotateShip(Level level, ShipState ship, net.minecraft.world.level.block.Rotation rotation, Player player) {
        if (!(level instanceof ServerLevel serverLevel) || ship == null || ship.isJumping()) return;
        if (rotation == null || rotation == net.minecraft.world.level.block.Rotation.NONE) return;

        // Bewegungs-Cooldown prüfen
        long gameTime = level.getGameTime();
        if (ship.isMovementOnCooldown(gameTime)) {
            long remaining = ship.getMovementCooldownRemaining(gameTime);
            if (player != null) {
                String remainingSec = (remaining / 20) + "." + (remaining % 20 * 5);
                player.displayClientMessage(
                        Component.translatable(com.peaceman.alpha.registry.ModI18n.Message.MOVEMENT_COOLDOWN_ACTIVE, remainingSec),
                        true
                );
            }
            return;
        }

        // 1. Pre-Rotation Collision-Check gegen Welt und andere Schiffe
        if (ShipCollisionService.checkRotationCollisions(serverLevel, ship, rotation)) {
            serverLevel.playSound(null, ship.getControllerPos(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 0.5f);
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable(com.peaceman.alpha.registry.ModI18n.Message.ROTATION_BLOCKED_COLLISION),
                        true
                );
            }
            return;
        }

        // Bewegungs-Cooldown sofort setzen
        ship.setMovementCooldownUntil(gameTime + ShipState.MOVEMENT_COOLDOWN_TICKS);

        PENDING_TASKS.add(new RotationTask(serverLevel, ship, rotation, player));
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
