package com.lit.spaceships.block.entity;

import com.lit.spaceships.menu.SpaceshipReactorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.jetbrains.annotations.Nullable;

// 1. NEU: Erbt von AbstractSpaceshipNodeBlockEntity (welches bereits ISpaceshipNode implementiert!)
public class SpaceshipReactorBlockEntity extends AbstractSpaceshipNodeBlockEntity implements MenuProvider {

    // Kapazität: 1.000.000 FE, maxReceive: 10.000 FE, maxExtract: 10.000 FE
    private final EnergyStorage energyStorage = new EnergyStorage(1000000, 100000, 10000) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
                updateLitState();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
                updateLitState();
            }
            return extracted;
        }
    };

    private void updateLitState() {
        if (level != null && !level.isClientSide()) {
            boolean shouldBeLit = energyStorage.getEnergyStored() > 0;
            BlockState currentState = getBlockState();
            if (currentState.hasProperty(com.lit.spaceships.block.SpaceshipReactorBlock.LIT)) {
                boolean isLit = currentState.getValue(com.lit.spaceships.block.SpaceshipReactorBlock.LIT);
                if (isLit != shouldBeLit) {
                    level.setBlock(getBlockPos(), currentState.setValue(com.lit.spaceships.block.SpaceshipReactorBlock.LIT, shouldBeLit), 3);
                }
            }
        }
    }

    // Daten-Synchronisation für das Menü (Server -> Client für die GUI)
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            com.lit.spaceships.ship.domain.ShipState ship = (getShipId() != null) ? com.lit.spaceships.ship.service.ServerShipManager.getShip(getShipId()) : null;
            int localEnergy = energyStorage.getEnergyStored();
            int localMax = energyStorage.getMaxEnergyStored();

            switch (index) {
                case 0:
                    return localEnergy;
                case 1:
                    return localMax;
                case 2:
                    return (ship != null && level != null) ? com.lit.spaceships.ship.SpaceshipEnergyManager.getTotalAvailableEnergy(level, ship) : localEnergy;
                case 3:
                    return (ship != null) ? Math.max(localMax, ship.getReactors().size() * 1000000) : localMax;
                case 4:
                    return (ship != null) ? ship.getLastGenerationRate() : 0;
                case 5:
                    return (ship != null) ? ship.getLastConsumptionRate() : 0;
                case 6:
                    return (ship != null) ? ship.getNetEnergyThroughput() : 0;
                case 7:
                    return (ship != null) ? ship.getPowerPriority().getId() : com.lit.spaceships.ship.domain.PowerPriority.BALANCED.getId();
                case 8: {
                    // Core Stability: 100% normal, sinkt leicht bei anhaltender Überlast
                    if (localEnergy <= 0) return 0;
                    int drain = (ship != null) ? ship.getLastConsumptionRate() : 0;
                    return Math.clamp(100 - (drain / 200), 75, 100);
                }
                case 9: {
                    // Operational Status: 0=Optimal, 1=High Load, 2=Critical Drain, 3=Standby, 4=Unlinked
                    if (ship == null) return 4; // UNLINKED
                    if (localEnergy <= 0) return 2; // CRITICAL_DRAIN
                    int drain = ship.getLastConsumptionRate();
                    if (drain > 150) return 1; // HIGH_LOAD
                    if (drain == 0 && localEnergy >= localMax) return 3; // STANDBY
                    return 0; // OPTIMAL
                }
                case 10:
                    return (ship != null) ? Math.max(1, ship.getReactors().size()) : 1;
                case 11:
                    return (ship != null) ? ship.getLastShieldDrain() : 0;
                case 12:
                    return (ship != null) ? ship.getLastWeaponDrain() : 0;
                case 13:
                    return (ship != null) ? ship.getLastEngineDrain() : 0;
                default:
                    return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            // Client-seitig setzen wir hier nichts, da Energie nur Server -> Client fließt
        }

        @Override
        public int getCount() {
            return 14;
        }
    };

    public SpaceshipReactorBlockEntity(BlockPos pos, BlockState state) {
        // 2. NEU: Ruft den Konstruktor der abstrakten Elternklasse auf
        super(com.lit.spaceships.registry.ModBlockEntities.SPACESHIP_REACTOR_BE.get(), pos, state);
    }

    // --- MenuProvider Methoden ---
    @Override
    public Component getDisplayName() {
        return Component.translatable(com.lit.spaceships.registry.ModI18n.Screen.REACTOR_TITLE);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SpaceshipReactorMenu(containerId, playerInventory, this, this.data);
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    // --- NBT Daten Speichern & Laden ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // 3. WICHTIG: super.saveAdditional speichert automatisch die UUID für uns!
        super.saveAdditional(tag, registries);

        // Energie SICHER speichern (Offizieller NeoForge-Weg)
        tag.put("Energy", energyStorage.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // 4. WICHTIG: super.loadAdditional lädt automatisch die UUID!
        super.loadAdditional(tag, registries);

        // Energie SICHER laden (Offizieller NeoForge-Weg)
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(registries, tag.get("Energy"));
        }
    }
}