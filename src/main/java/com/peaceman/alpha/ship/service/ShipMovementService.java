package com.peaceman.alpha.ship.service;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.ISpaceshipNode;
import com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity;
import com.peaceman.alpha.block.entity.SpaceshipControlBlockEntity;
import com.peaceman.alpha.network.ShipPositionSyncPayload;
import com.peaceman.alpha.registry.ModI18n;
import com.peaceman.alpha.ship.SpaceshipEnergyManager;
import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.relocation.api.RelocationContext;
import com.peaceman.alpha.ship.relocation.graph.BlockDependencyGraph;
import com.peaceman.alpha.ship.relocation.graph.RelocationNode;
import com.peaceman.alpha.ship.relocation.registry.BlockRelocationRegistry;
import net.minecraft.ChatFormatting;
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

        // 1. Obere Multiblock-Hälften & abhängige Multiblock-Teile -> Pass 3
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) &&
                state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
            return PlacementPass.PASS_3_ATTACHABLES_AND_TOPS;
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART) &&
                state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD) {
            return PlacementPass.PASS_3_ATTACHABLES_AND_TOPS;
        }
        if (state.getBlock() instanceof net.minecraft.world.level.block.piston.PistonHeadBlock) {
            return PlacementPass.PASS_3_ATTACHABLES_AND_TOPS;
        }

        // 2. Untere Multiblock-Wurzeln -> Pass 2
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) &&
                state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
            return PlacementPass.PASS_2_ROOTS_AND_NORMALS;
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART) &&
                state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART) == net.minecraft.world.level.block.state.properties.BedPart.FOOT) {
            return PlacementPass.PASS_2_ROOTS_AND_NORMALS;
        }

        // 3. Universelle Heuristik für anhängende & zerbrechliche Blöcke (Pass 3):
        // Face-Attached Blöcke (Hebel, Knöpfe), oder Blöcke, die ohne Nachbarunterstützung nicht überleben können
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE)) {
            return PlacementPass.PASS_3_ATTACHABLES_AND_TOPS;
        }
        try {
            BlockPos targetPos = pos != null ? pos : BlockPos.ZERO;
            com.peaceman.alpha.ship.relocation.graph.VirtualSupportTestView virtualView =
                    new com.peaceman.alpha.ship.relocation.graph.VirtualSupportTestView(level, null, targetPos.below());
            if (!state.canSurvive(virtualView, targetPos)) {
                return PlacementPass.PASS_3_ATTACHABLES_AND_TOPS;
            }
        } catch (Exception ignored) {
        }

        // 4. Reine feste Vollblöcke (Fundamente) -> Pass 1
        if (level != null && state.isSolidRender(level, pos != null ? pos : BlockPos.ZERO)) {
            return PlacementPass.PASS_1_SOLIDS;
        }
        if (state.isCollisionShapeFullBlock(level != null ? level : net.minecraft.world.level.EmptyBlockGetter.INSTANCE, pos != null ? pos : BlockPos.ZERO)) {
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

        List<List<RelocationNode>> topologicalBatches = new ArrayList<>();
        int currentBatchIndex = 0;
        int currentBatchNodeIndex = 0;

        List<BlockPos> blocksToRemove = new ArrayList<>();
        int removeIndex = 0;

        Set<BlockPos> newShipBlocks = new HashSet<>();
        List<Entity> entitiesToMove = new ArrayList<>();
        Set<ChunkPos> destinationChunks = new HashSet<>();
        BlockPos startPos;
        RelocationContext relocationContext;

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

            // Phase 0: Vorbereitung, Immunitätsprüfung, Chunk-Loading & Graph-Konstruktion
            if (phase == 0) {
                Set<BlockPos> shipBlocks = ship.getBlocks();

                // 1. Immunitätsprüfung (#c:relocation_immune / unverschiebbare Blöcke)
                for (BlockPos pos : shipBlocks) {
                    BlockState state = level.getBlockState(pos);
                    if (BlockRelocationRegistry.isImmune(state)) {
                        if (player != null) {
                            player.sendSystemMessage(Component.translatable(ModI18n.Message.MOVEMENT_BLOCKED_IMMUNE).withStyle(ChatFormatting.RED));
                        }
                        return true; // Abbruch: Schiff enthält unverschiebbaren Block
                    }
                }

                // 2. Energieprüfung
                if (!SpaceshipEnergyManager.tryConsumeFlightEnergy(level, ship, dx, dy, dz, player)) {
                    return true; // Abbruch wegen Energiemangel
                }

                // 3. Chunks im Zielgebiet vorbereiten und forceloaden
                destinationChunks = prepareDestinationChunks(level, ship, new Vec3(dx, dy, dz));

                // 4. Bounding Box & Passagier-Matrix erfassen
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

                // 5. Zielpositionen berechnen
                for (BlockPos pos : shipBlocks) {
                    newShipBlocks.add(pos.offset(dx, dy, dz));
                }

                relocationContext = new RelocationContext(
                        level, ship, dx, dy, dz, net.minecraft.world.level.block.Rotation.NONE,
                        shipBlocks, newShipBlocks, player
                );

                // 6. Snapshot erstellen & onPreRelocation dispatchen
                for (BlockPos pos : shipBlocks) {
                    BlockState state = level.getBlockState(pos);
                    BlockEntity be = level.getBlockEntity(pos);
                    CompoundTag nbt = (be != null) ? be.saveWithFullMetadata(level.registryAccess()) : null;
                    if (nbt != null) {
                        com.peaceman.alpha.ship.relocation.util.NbtCoordinateRemapper.remapCoordinates(
                                nbt, shipBlocks, p -> p.offset(dx, dy, dz));
                    }
                    BlockRelocationRegistry.dispatchPreRelocation(pos, state, be, nbt, relocationContext);
                    snapshot.put(pos, new BlockData(state, nbt));
                }

                // 7. Inventare sichern & BlockEntities vorab entfernen (verhindert Containers.dropContents)
                for (BlockPos pos : shipBlocks) {
                    if (level.getBlockEntity(pos) != null) {
                        level.removeBlockEntity(pos);
                    }
                }

                // 7b. Ausgefahrene Pistons in der Welt vorab entwaffnen (EXTENDED = false mit Flag 48),
                // um zu verhindern, dass PistonHeadBlock.onRemove beim Abbau alter Positionen level.destroyBlock() triggert!
                for (BlockPos pos : shipBlocks) {
                    BlockState s = level.getBlockState(pos);
                    if (s.getBlock() instanceof net.minecraft.world.level.block.piston.PistonBaseBlock &&
                            s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.EXTENDED) &&
                            s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EXTENDED)) {
                        level.setBlock(pos, s.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EXTENDED, false), 48);
                    }
                }

                // 8. Vorabbereinigung im Weg stehender Weltblöcke
                for (BlockPos newPos : newShipBlocks) {
                    if (!shipBlocks.contains(newPos) && !level.getBlockState(newPos).isAir()) {
                        level.destroyBlock(newPos, true);
                    }
                }

                // 9. Controller entkoppeln
                if (level.getBlockEntity(startPos) instanceof SpaceshipControlBlockEntity be) {
                    be.setShipId(null);
                }
                BlockPos newStartPos = startPos.offset(dx, dy, dz);
                ship.setControllerPos(newStartPos);

                // 10. Freigewordene alte Positionen (P_alt \ P_neu) zur Löschung vormerken
                for (BlockPos pos : shipBlocks) {
                    if (!newShipBlocks.contains(pos)) {
                        blocksToRemove.add(pos);
                    }
                }
                // Von oben nach unten löschen (Attachables vor Fundamenten)
                blocksToRemove.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

                // 11. Topologischen Abhängigkeitsgraphen konstruieren & Schichten lösen
                BlockDependencyGraph graph = new BlockDependencyGraph();
                for (Map.Entry<BlockPos, BlockData> entry : snapshot.entrySet()) {
                    graph.addNode(entry.getKey(), entry.getKey().offset(dx, dy, dz), entry.getValue().state(), entry.getValue().nbt());
                }
                graph.buildDependencies(level);
                topologicalBatches = graph.resolveTopologicalBatches();

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

            // Phase 2: Topologische Batches sequenziell mit Flag 52 platzieren
            if (phase == 2) {
                while (currentBatchIndex < topologicalBatches.size()) {
                    List<RelocationNode> batch = topologicalBatches.get(currentBatchIndex);
                    while (currentBatchNodeIndex < batch.size()) {
                        if (System.nanoTime() >= deadlineNanos) {
                            return false;
                        }
                        RelocationNode node = batch.get(currentBatchNodeIndex++);
                        placeNode(level, node, ship.getId(), 52, relocationContext);
                    }
                    currentBatchIndex++;
                    currentBatchNodeIndex = 0;
                }
                phase = 3;
            }

            // Phase 3: Abschluss & Synchronisation
            if (phase == 3) {
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

        private void placeNode(Level lvl, RelocationNode node, UUID shipId, int flags, RelocationContext context) {
            BlockPos newPos = node.getNewPos();
            BlockState state = node.getState();
            CompoundTag nbt = node.getNbt();

            lvl.setBlock(newPos, state, flags);

            BlockEntity newBe = null;
            if (nbt != null) {
                nbt.putInt("x", newPos.getX());
                nbt.putInt("y", newPos.getY());
                nbt.putInt("z", newPos.getZ());
                newBe = BlockEntity.loadStatic(newPos, state, nbt, lvl.registryAccess());
                if (newBe != null) {
                    newBe.clearRemoved();
                    lvl.setBlockEntity(newBe);
                }
            }

            if (newBe == null) {
                newBe = lvl.getBlockEntity(newPos);
            }

            if (newBe instanceof ISpaceshipNode spaceshipNode) {
                spaceshipNode.setShipId(shipId);
            }

            if (context != null) {
                BlockRelocationRegistry.dispatchPostRelocation(node.getOldPos(), newPos, state, newBe, context);
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

        List<List<RelocationNode>> topologicalBatches = new ArrayList<>();
        int currentBatchIndex = 0;
        int currentBatchNodeIndex = 0;

        List<BlockPos> blocksToRemove = new ArrayList<>();
        int removeIndex = 0;

        Set<BlockPos> newShipBlocks = new HashSet<>();
        List<Entity> entitiesToMove = new ArrayList<>();
        Set<ChunkPos> destinationChunks = new HashSet<>();
        BlockPos startPos;
        RelocationContext relocationContext;

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

            // Phase 0: Vorbereitung, Immunitätsprüfung, Chunk-Loading & Graph-Konstruktion
            if (phase == 0) {
                Set<BlockPos> shipBlocks = ship.getBlocks();

                // 1. Immunitätsprüfung (#c:relocation_immune / unverschiebbare Blöcke)
                for (BlockPos pos : shipBlocks) {
                    BlockState state = level.getBlockState(pos);
                    if (BlockRelocationRegistry.isImmune(state)) {
                        if (player != null) {
                            player.sendSystemMessage(Component.translatable(ModI18n.Message.MOVEMENT_BLOCKED_IMMUNE).withStyle(ChatFormatting.RED));
                        }
                        return true; // Abbruch: Schiff enthält unverschiebbaren Block
                    }
                }

                // 2. Energieprüfung
                if (!SpaceshipEnergyManager.tryConsumeRotationEnergy(level, ship, rotation, player)) {
                    return true;
                }

                // 3. Zielpositionen berechnen und Chunks im Zielgebiet vorbereiten
                for (BlockPos pos : shipBlocks) {
                    newShipBlocks.add(ShipRotationMath.rotateAbsoluteBlockPos(pos, startPos, rotation));
                }

                destinationChunks = prepareDestinationChunksForBlocks(level, newShipBlocks, shipBlocks);

                // 4. Bounding Box & Passagier-Matrix erfassen
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

                relocationContext = new RelocationContext(
                        level, ship, 0, 0, 0, rotation,
                        shipBlocks, newShipBlocks, player
                );

                // 5. Snapshot erstellen & onPreRelocation dispatchen
                for (BlockPos pos : shipBlocks) {
                    BlockState state = level.getBlockState(pos);
                    BlockEntity be = level.getBlockEntity(pos);
                    CompoundTag nbt = (be != null) ? be.saveWithFullMetadata(level.registryAccess()) : null;
                    if (nbt != null) {
                        com.peaceman.alpha.ship.relocation.util.NbtCoordinateRemapper.remapCoordinates(
                                nbt, shipBlocks, p -> ShipRotationMath.rotateAbsoluteBlockPos(p, startPos, rotation));
                    }
                    BlockRelocationRegistry.dispatchPreRelocation(pos, state, be, nbt, relocationContext);
                    snapshot.put(pos, new BlockData(state, nbt));
                }

                // 6. Inventare sichern & BlockEntities vorab entfernen
                for (BlockPos pos : shipBlocks) {
                    if (level.getBlockEntity(pos) != null) {
                        level.removeBlockEntity(pos);
                    }
                }

                // 6b. Ausgefahrene Pistons in der Welt vorab entwaffnen (EXTENDED = false mit Flag 48),
                // um zu verhindern, dass PistonHeadBlock.onRemove beim Abbau alter Positionen level.destroyBlock() triggert!
                for (BlockPos pos : shipBlocks) {
                    BlockState s = level.getBlockState(pos);
                    if (s.getBlock() instanceof net.minecraft.world.level.block.piston.PistonBaseBlock &&
                            s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.EXTENDED) &&
                            s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EXTENDED)) {
                        level.setBlock(pos, s.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EXTENDED, false), 48);
                    }
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

                // 9. Freigewordene alte Positionen (P_alt \ P_neu) zur Löschung vormerken
                for (BlockPos pos : shipBlocks) {
                    if (!newShipBlocks.contains(pos)) {
                        blocksToRemove.add(pos);
                    }
                }
                blocksToRemove.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

                // 10. Topologischen Abhängigkeitsgraphen für rotierte Blöcke konstruieren
                BlockDependencyGraph graph = new BlockDependencyGraph();
                for (Map.Entry<BlockPos, BlockData> entry : snapshot.entrySet()) {
                    BlockPos newPos = ShipRotationMath.rotateAbsoluteBlockPos(entry.getKey(), startPos, rotation);
                    BlockState rotState = entry.getValue().state().rotate(rotation);
                    graph.addNode(entry.getKey(), newPos, rotState, entry.getValue().nbt());
                }
                graph.buildDependencies(level);
                topologicalBatches = graph.resolveTopologicalBatches();

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

            // Phase 2: Topologische Batches sequenziell mit Flag 52 platzieren
            if (phase == 2) {
                while (currentBatchIndex < topologicalBatches.size()) {
                    List<RelocationNode> batch = topologicalBatches.get(currentBatchIndex);
                    while (currentBatchNodeIndex < batch.size()) {
                        if (System.nanoTime() >= deadlineNanos) {
                            return false;
                        }
                        RelocationNode node = batch.get(currentBatchNodeIndex++);
                        placeRotatedNode(level, node, rotation, ship.getId(), 52, relocationContext);
                    }
                    currentBatchIndex++;
                    currentBatchNodeIndex = 0;
                }
                phase = 3;
            }

            // Phase 3: Abschluss & Synchronisation
            if (phase == 3) {
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

        private void placeRotatedNode(Level lvl, RelocationNode node, net.minecraft.world.level.block.Rotation rot, UUID shipId, int flags, RelocationContext context) {
            BlockPos newPos = node.getNewPos();
            BlockState state = node.getState();
            CompoundTag nbt = node.getNbt();

            lvl.setBlock(newPos, state, flags);

            BlockEntity newBe = null;
            if (nbt != null) {
                nbt.putInt("x", newPos.getX());
                nbt.putInt("y", newPos.getY());
                nbt.putInt("z", newPos.getZ());
                newBe = BlockEntity.loadStatic(newPos, state, nbt, lvl.registryAccess());
                if (newBe != null) {
                    newBe.clearRemoved();
                    lvl.setBlockEntity(newBe);
                }
            }

            if (newBe == null) {
                newBe = lvl.getBlockEntity(newPos);
            }

            if (newBe instanceof ISpaceshipNode spaceshipNode) {
                spaceshipNode.setShipId(shipId);
            }

            if (newBe instanceof AbstractLaserNodeBlockEntity laserBe) {
                laserBe.rotateTurret(rot);
            }

            if (context != null) {
                BlockRelocationRegistry.dispatchPostRelocation(node.getOldPos(), newPos, state, newBe, context);
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
