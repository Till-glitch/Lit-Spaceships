package com.lit.spaceships.block.entity;

import com.lit.spaceships.network.WarpStateSyncPayload;
import com.lit.spaceships.registry.ModBlockEntities;
import com.lit.spaceships.registry.ModI18n;
import com.lit.spaceships.ship.SpaceshipEnergyManager;
import com.lit.spaceships.ship.domain.ShipState;
import com.lit.spaceships.ship.service.ServerShipManager;
import com.lit.spaceships.ship.service.WarpService;
import com.lit.spaceships.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;

import java.util.Optional;
import java.util.UUID;

public class WarpEngineBlockEntity extends AbstractSpaceshipNodeBlockEntity implements IEnergyStorage {

    public static final int REQUIRED_ENERGY = 100000;
    public static final int ENERGY_CAPACITY = REQUIRED_ENERGY;
    public static final int COUNTDOWN_TOTAL_TICKS = 200; // 10 Sekunden (20 Ticks/s)
    public static final int COUNTDOWN_MAX_TICKS = COUNTDOWN_TOTAL_TICKS;
    public static final long COOLDOWN_TOTAL_TICKS = 1200L; // 60 Sekunden (1 Minute)
    public static final int COOLDOWN_TICKS = 1200;
    public static final int TRICKLE_CHARGE_RATE = 500; // FE pro Tick aus dem Schiffsnetz
    public static final int TRICKLE_DRAW_PER_TICK = TRICKLE_CHARGE_RATE;

    private final EnergyStorage energyStorage = new EnergyStorage(REQUIRED_ENERGY, 10000, REQUIRED_ENERGY) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
            }
            return extracted;
        }
    };

    private int countdownTicks = 0;
    private boolean isCountingDown = false;
    private long cooldownUntil = 0L;

    private BlockPos initialShipPos = null;
    private Quaternionf initialShipRot = null;
    private UUID initiatorId = null;

    public WarpEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WARP_ENGINE_BE.get(), pos, state);
    }

    // --- IEnergyStorage Implementierung ---
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return energyStorage.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return energyStorage.extractEnergy(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return energyStorage.getEnergyStored() < REQUIRED_ENERGY;
    }

    public IEnergyStorage getEnergyStorage() {
        return this;
    }

    // --- Status-Getter für Client & Tests ---
    public boolean isCountingDown() {
        return isCountingDown;
    }

    public int getCountdownTicks() {
        return countdownTicks;
    }

    public long getCooldownUntil() {
        return cooldownUntil;
    }

    public long getCooldownRemaining(long gameTime) {
        return Math.max(0L, cooldownUntil - gameTime);
    }

    public boolean isReady(long gameTime) {
        return getEnergyStored() >= REQUIRED_ENERGY && getCooldownRemaining(gameTime) <= 0L && !isCountingDown;
    }

    // --- Countdown Start & Abbruch ---
    public boolean startCountdown(Player initiator) {
        if (level == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (getShipId() == null) {
            if (initiator != null) {
                initiator.displayClientMessage(Component.translatable(ModI18n.Screen.WARP_STATUS_UNLINKED), true);
            }
            return false;
        }

        ShipState ship = ServerShipManager.getShip(getShipId());
        if (ship == null) {
            return false;
        }

        long gameTime = level.getGameTime();
        if (getCooldownRemaining(gameTime) > 0L) {
            if (initiator != null) {
                long sec = (getCooldownRemaining(gameTime) + 19L) / 20L;
                initiator.displayClientMessage(Component.translatable(ModI18n.Message.WARP_COOLDOWN_ACTIVE, sec), true);
            }
            return false;
        }

        if (getEnergyStored() < REQUIRED_ENERGY) {
            if (initiator != null) {
                initiator.displayClientMessage(Component.translatable(ModI18n.Message.WARP_ENERGY_INSUFFICIENT), true);
            }
            return false;
        }

        this.isCountingDown = true;
        this.countdownTicks = COUNTDOWN_TOTAL_TICKS;
        this.initialShipPos = ship.getControllerPos();
        this.initialShipRot = new Quaternionf(ship.getRotation());
        this.initiatorId = initiator != null ? initiator.getUUID() : null;

        setChanged();
        syncStateToClients();

        if (initiator != null) {
            initiator.displayClientMessage(Component.translatable(ModI18n.Message.WARP_COUNTDOWN_TICK, 10), true);
        }

        return true;
    }

    public boolean startCountdown() {
        return startCountdown((Player) null);
    }

    public void abortCountdown(String reason) {
        abortCountdown(reason != null ? Component.literal(reason) : null);
    }

    public void abortCountdown(Component reason) {
        if (this.isCountingDown) {
            this.isCountingDown = false;
            this.countdownTicks = 0;
            this.initialShipPos = null;
            this.initialShipRot = null;

            setChanged();
            syncStateToClients();

            if (level != null && !level.isClientSide() && reason != null && initiatorId != null) {
                Player player = level.getPlayerByUUID(initiatorId);
                if (player != null) {
                    player.displayClientMessage(reason, true);
                }
            }
            this.initiatorId = null;
        }
    }

    // --- Server Tick ---
    public static void serverTick(Level level, BlockPos pos, BlockState state, WarpEngineBlockEntity be) {
        if (level == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        long gameTime = level.getGameTime();

        // 1. Grid-Laden aus Schiffsreaktoren, wenn nicht voll und nicht im Countdown
        if (!be.isCountingDown && be.getEnergyStored() < REQUIRED_ENERGY && be.getShipId() != null) {
            ShipState ship = ServerShipManager.getShip(be.getShipId());
            if (ship != null && !ship.getReactors().isEmpty()) {
                int needed = REQUIRED_ENERGY - be.getEnergyStored();
                int toDraw = Math.min(TRICKLE_CHARGE_RATE, needed);
                if (SpaceshipEnergyManager.tryConsumeEnergyAmount(level, ship, toDraw)) {
                    be.energyStorage.receiveEnergy(toDraw, false);
                    be.setChanged();
                }
            }
        }

        // 2. Countdown-Verarbeitung
        if (be.isCountingDown) {
            ShipState ship = be.getShipId() != null ? ServerShipManager.getShip(be.getShipId()) : null;

            // Abbruchbedingung: Schiff gelöscht, bewegt oder rotiert
            if (ship == null) {
                be.abortCountdown(Component.translatable(ModI18n.Message.WARP_COUNTDOWN_ABORTED));
                return;
            }

            if (be.initialShipPos != null && !be.initialShipPos.equals(ship.getControllerPos())) {
                be.abortCountdown(Component.translatable(ModI18n.Message.WARP_COUNTDOWN_ABORTED_MOVEMENT));
                return;
            }

            if (be.initialShipRot != null && !be.initialShipRot.equals(ship.getRotation())) {
                be.abortCountdown(Component.translatable(ModI18n.Message.WARP_COUNTDOWN_ABORTED_MOVEMENT));
                return;
            }

            be.countdownTicks--;

            // Jede Sekunde Action-Bar Tick senden
            if (be.countdownTicks > 0 && be.countdownTicks % 20 == 0) {
                int sec = be.countdownTicks / 20;
                if (be.initiatorId != null) {
                    Player p = level.getPlayerByUUID(be.initiatorId);
                    if (p != null) {
                        p.displayClientMessage(Component.translatable(ModI18n.Message.WARP_COUNTDOWN_TICK, sec), true);
                    }
                }
            }

            // Sync an Clients alle 10 Ticks
            if (be.countdownTicks % 10 == 0) {
                be.syncStateToClients();
            }

            // Countdown abgeschlossen -> Sprung ausführen!
            if (be.countdownTicks <= 0) {
                be.isCountingDown = false;
                be.countdownTicks = 0;

                ServerLevel targetLevel = WarpService.getTargetLevel(serverLevel);
                if (targetLevel == null) {
                    be.abortCountdown(Component.translatable(ModI18n.Message.WARP_COUNTDOWN_ABORTED));
                    return;
                }

                Optional<BlockPos> safePos = WarpService.findSafeTargetPos(serverLevel, targetLevel, ship);
                if (safePos.isEmpty()) {
                    be.abortCountdown(Component.translatable(ModI18n.Message.WARP_OBSTRUCTED));
                    return;
                }

                // Energie verbrauchen und Cooldown starten
                be.energyStorage.extractEnergy(REQUIRED_ENERGY, false);
                be.cooldownUntil = gameTime + COOLDOWN_TOTAL_TICKS;

                Player initiator = be.initiatorId != null ? level.getPlayerByUUID(be.initiatorId) : null;
                be.initiatorId = null;
                be.initialShipPos = null;
                be.initialShipRot = null;
                be.setChanged();

                WarpService.executeWarp(serverLevel, targetLevel, ship, safePos.get(), initiator);
            }
        }
    }

    public void syncStateToClients() {
        if (level != null && !level.isClientSide() && level instanceof ServerLevel serverLevel) {
            long gameTime = level.getGameTime();
            int cdRemaining = (int) Math.max(0L, cooldownUntil - gameTime);
            boolean targetIsSpace = !level.dimension().equals(ModDimensions.SPACE_LEVEL);

            WarpStateSyncPayload payload = new WarpStateSyncPayload(
                    worldPosition,
                    getEnergyStored(),
                    getMaxEnergyStored(),
                    countdownTicks,
                    cdRemaining,
                    isCountingDown,
                    getShipId() != null,
                    targetIsSpace
            );

            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(worldPosition), payload);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", energyStorage.serializeNBT(registries));
        tag.putInt("CountdownTicks", countdownTicks);
        tag.putBoolean("IsCountingDown", isCountingDown);
        tag.putLong("CooldownUntil", cooldownUntil);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(registries, tag.get("Energy"));
        }
        countdownTicks = tag.getInt("CountdownTicks");
        isCountingDown = tag.getBoolean("IsCountingDown");
        cooldownUntil = tag.getLong("CooldownUntil");
    }
}
