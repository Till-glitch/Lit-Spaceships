package com.peaceman.alpha.block.entity;

import com.peaceman.alpha.menu.SpaceshipReactorMenu;
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
            if (currentState.hasProperty(com.peaceman.alpha.block.SpaceshipReactorBlock.LIT)) {
                boolean isLit = currentState.getValue(com.peaceman.alpha.block.SpaceshipReactorBlock.LIT);
                if (isLit != shouldBeLit) {
                    level.setBlock(getBlockPos(), currentState.setValue(com.peaceman.alpha.block.SpaceshipReactorBlock.LIT, shouldBeLit), 3);
                }
            }
        }
    }

    // Daten-Synchronisation für das Menü (Server -> Client für die GUI)
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0:
                    return energyStorage.getEnergyStored();
                case 1:
                    return energyStorage.getMaxEnergyStored();
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
            return 2;
        }
    };

    public SpaceshipReactorBlockEntity(BlockPos pos, BlockState state) {
        // 2. NEU: Ruft den Konstruktor der abstrakten Elternklasse auf
        super(com.peaceman.alpha.registry.ModBlockEntities.SPACESHIP_REACTOR_BE.get(), pos, state);
    }

    // --- MenuProvider Methoden ---
    @Override
    public Component getDisplayName() {
        return Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.REACTOR_TITLE);
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