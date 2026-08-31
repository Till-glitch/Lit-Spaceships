package com.peaceman.alpha.network;

import com.peaceman.alpha.block.entity.SpaceshipControlBlockEntity;
import com.peaceman.alpha.ship.SpaceshipNavigationManager;
import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ServerShipManager;
import com.peaceman.alpha.ship.service.ShipMovementService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.UUID;

public class ServerPayloadHandler {

    public static void handleAction(final ShipActionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;
            Level level = player.level();
            var pos = payload.pos();

            // 1. SONDERFALL: SCHIFF ERSTELLEN
            if (payload.actionType() == ShipActionPayload.ActionType.CREATE) {
                if (level.getBlockEntity(pos) instanceof SpaceshipControlBlockEntity blockEntity) {
                    ShipState newShip = ServerShipManager.createShip(level, pos);
                    if (newShip != null) {
                        blockEntity.setShipId(newShip.getId());
                    }
                }
                return;
            }

            // 2. FÜR ALLE ANDEREN AKTIONEN: UUID BENÖTIGT
            if (payload.shipId().isEmpty()) {
                return;
            }

            ShipState ship = ServerShipManager.getShip(payload.shipId().get());
            if (ship == null) {
                return;
            }

            int dist = payload.value();
            String targetName = payload.targetName();

            // 3. TYPISIERTE AKTION AUSFÜHREN
            switch (payload.actionType()) {
                case SAVE_HOME -> SpaceshipNavigationManager.saveHome(level, ship, targetName);
                case TP_HOME -> SpaceshipNavigationManager.teleportToHome(level, ship, targetName, player);
                case UPDATE_BLOCKS -> ServerShipManager.updateShipBlocks(level, ship);
                case DELETE_SHIP -> ServerShipManager.deleteShip(level, ship);
                case MOVE_UP -> ShipMovementService.moveShip(level, ship, 0, dist, 0, player);
                case MOVE_DOWN -> ShipMovementService.moveShip(level, ship, 0, -dist, 0, player);
                case MOVE_FORWARD, MOVE_BACKWARD, MOVE_LEFT, MOVE_RIGHT -> {
                    Direction forward = player.getDirection();
                    Direction right = forward.getClockWise();
                    int dx = 0;
                    int dz = 0;

                    switch (payload.actionType()) {
                        case MOVE_FORWARD -> {
                            dx = forward.getStepX() * dist;
                            dz = forward.getStepZ() * dist;
                        }
                        case MOVE_BACKWARD -> {
                            dx = -forward.getStepX() * dist;
                            dz = -forward.getStepZ() * dist;
                        }
                        case MOVE_RIGHT -> {
                            dx = right.getStepX() * dist;
                            dz = right.getStepZ() * dist;
                        }
                        case MOVE_LEFT -> {
                            dx = -right.getStepX() * dist;
                            dz = -right.getStepZ() * dist;
                        }
                        default -> {}
                    }
                    ShipMovementService.moveShip(level, ship, dx, 0, dz, player);
                }
                case TOGGLE_SHIELD -> com.peaceman.alpha.ship.SpaceshipShieldHandler.toggleShield(level, ship);
                case TOGGLE_SHIELD_ZONE -> com.peaceman.alpha.ship.SpaceshipShieldHandler.toggleShieldZone(level, ship, payload.pos());
                default -> {}
            }
        });
    }

    public static void handleMovementRequest(final ShipMovementRequestPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;
            Level level = player.level();
            if (payload.shipId() == null) return;

            ShipState ship = ServerShipManager.getShip(payload.shipId());
            if (ship == null) return;

            Direction forward = player.getDirection();
            Direction right = forward.getClockWise();

            int dx = Math.round(forward.getStepX() * payload.impulseForward() + right.getStepX() * payload.impulseLeft());
            int dz = Math.round(forward.getStepZ() * payload.impulseForward() + right.getStepZ() * payload.impulseLeft());
            int dy = Math.round(payload.impulseUp());

            if (dx != 0 || dy != 0 || dz != 0) {
                // Distanz pro Tick (z.B. 1 Block)
                ShipMovementService.moveShip(level, ship, dx, dy, dz, player);
            }
        });
    }

    public static void handleCombatAction(final ShipCombatActionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;
            Level level = player.level();

            UUID targetShipId = payload.shipId().orElse(null);
            if (targetShipId == null && player.getVehicle() instanceof com.peaceman.alpha.entity.TurretSeatEntity seat) {
                targetShipId = seat.getShipId();
            }
            if (targetShipId == null) return;

            ShipState ship = ServerShipManager.getShip(targetShipId);
            if (ship == null) return;

            List<BlockPos> weapons = ship.getWeapons();
            if (weapons.isEmpty()) return;
            
            if (payload.action() == ShipCombatActionPayload.CombatAction.FIRE_SPECIFIC && payload.weaponPos().isPresent()) {
                BlockPos targetPos = payload.weaponPos().get();
                if (weapons.contains(targetPos)) {
                    com.peaceman.alpha.ship.combat.LaserCombatService.fireWeapon(level, ship, targetPos);
                }
                return;
            }

            for (BlockPos weaponPos : weapons) {
                var be = level.getBlockEntity(weaponPos);
                if (be instanceof com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity laserBe) {
                    switch (payload.action()) {
                        case FIRE_PULSE -> {
                            if (laserBe instanceof com.peaceman.alpha.block.entity.PulseLaserBlockEntity) {
                                com.peaceman.alpha.ship.combat.LaserCombatService.fireWeapon(level, ship, weaponPos);
                            }
                        }
                        case TOGGLE_HEAVY_BEAM -> {
                            if (laserBe instanceof com.peaceman.alpha.block.entity.HeavyBeamBlockEntity) {
                                com.peaceman.alpha.ship.combat.LaserCombatService.fireWeapon(level, ship, weaponPos);
                            }
                        }
                        case TOGGLE_MINING_LASER -> {
                            if (laserBe instanceof com.peaceman.alpha.block.entity.MiningLaserBlockEntity) {
                                com.peaceman.alpha.ship.combat.LaserCombatService.fireWeapon(level, ship, weaponPos);
                            }
                        }
                        case FIRE_ALL -> {
                            com.peaceman.alpha.ship.combat.LaserCombatService.fireWeapon(level, ship, weaponPos);
                        }
                    }
                }
            }
        });
    }

    public static void handleTurretAim(final TurretAimPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null || !(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

            var pos = payload.weaponPos();
            if (pos == null) return;

            if (serverLevel.getBlockEntity(pos) instanceof com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity laserBE) {
                if (player.getVehicle() instanceof com.peaceman.alpha.entity.TurretSeatEntity seat && pos.equals(seat.getWeaponPos())) {
                    float yaw = com.peaceman.alpha.ship.combat.aim.AimTransformMath.decompressAngle(payload.compressedYaw());
                    float pitch = com.peaceman.alpha.ship.combat.aim.AimTransformMath.decompressAngle(payload.compressedPitch());

                    laserBE.setAimAngles(new com.peaceman.alpha.ship.combat.aim.AimAngles(yaw, pitch));

                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(
                            serverLevel, new net.minecraft.world.level.ChunkPos(pos), payload
                    );
                }
            }
        });
    }

    public static void handleTurretAimSync(final TurretAimSyncPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null || !(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

            var pos = payload.weaponPos();
            if (pos == null) return;

            if (serverLevel.getBlockEntity(pos) instanceof com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity laserBE) {
                if (player.getVehicle() instanceof com.peaceman.alpha.entity.TurretSeatEntity seat && pos.equals(seat.getWeaponPos())) {
                    com.peaceman.alpha.helper.TurretDebugLogger.logServerAimReceived(player.getName().getString(), pos, payload.yaw(), payload.pitch(), laserBE.isAimLocked());
                    if (!laserBE.isAimLocked()) {
                        laserBE.setAimAngles(new com.peaceman.alpha.ship.combat.aim.AimAngles(payload.yaw(), payload.pitch()));

                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(
                                serverLevel, new net.minecraft.world.level.ChunkPos(pos), payload
                        );
                    }
                }
            }
        });
    }

    public static void handleTurretLockToggle(final TurretLockTogglePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null || !(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

            var pos = payload.weaponPos();
            if (pos == null) return;

            if (serverLevel.getBlockEntity(pos) instanceof com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity laserBE) {
                if (player.getVehicle() instanceof com.peaceman.alpha.entity.TurretSeatEntity seat && pos.equals(seat.getWeaponPos())) {
                    boolean newLock = !laserBE.isAimLocked();
                    laserBE.setAimLocked(newLock);
                    com.peaceman.alpha.helper.TurretDebugLogger.logServerLockToggled(player.getName().getString(), pos, newLock);

                    // 1. Akustisches Feedback über Server an alle nahegelegenen Spieler
                    net.minecraft.sounds.SoundEvent sound = newLock ? net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE : net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN;
                    serverLevel.playSound(null, pos, sound, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.2f);

                    // 2. Action-Bar Benachrichtigung an den Spieler
                    if (newLock) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                com.peaceman.alpha.registry.ModI18n.Message.TURRET_AIM_LOCKED,
                                laserBE.getTargetYaw(), laserBE.getTargetPitch()), true);
                    } else {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                com.peaceman.alpha.registry.ModI18n.Message.TURRET_AIM_RELEASED), true);
                    }

                    // 3. Synchronisiere aktuellen BE-Zustand an alle Clients
                    serverLevel.sendBlockUpdated(pos, laserBE.getBlockState(), laserBE.getBlockState(), 3);
                }
            }
        });
    }

    public static void handleOpenHelmConfig(OpenHelmConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                java.util.UUID shipId = payload.shipId().orElse(null);
                if (shipId != null && com.peaceman.alpha.ship.service.ServerShipManager.ACTIVE_SHIPS.containsKey(shipId)) {
                    com.peaceman.alpha.ship.domain.ShipState ship = com.peaceman.alpha.ship.service.ServerShipManager.getShip(shipId);
                    if (ship != null) {
                        net.minecraft.core.BlockPos controllerPos = ship.getControllerPos();
                        int energy = com.peaceman.alpha.ship.SpaceshipEnergyManager.getTotalAvailableEnergy(serverPlayer.serverLevel(), ship);
                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                                new ShipStateSyncPayload(shipId, energy, ship.isShieldActive(),
                                        ship.getShieldCooldownRemaining(serverPlayer.serverLevel().getGameTime()),
                                        ship.getMovementCooldownRemaining(serverPlayer.serverLevel().getGameTime())));
                        serverPlayer.openMenu(new net.minecraft.world.MenuProvider() {
                            @Override
                            public net.minecraft.network.chat.Component getDisplayName() {
                                return net.minecraft.network.chat.Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_NAV_TITLE);
                            }

                            @org.jetbrains.annotations.Nullable
                            @Override
                            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player player) {
                                return new com.peaceman.alpha.menu.SpaceshipHelmMenu(id, inv, controllerPos);
                            }
                        }, buf -> buf.writeBlockPos(controllerPos));
                    }
                }
            }
        });
    }
}
