package com.lit.spaceships.menu;

import com.lit.spaceships.block.entity.SpaceshipShieldBlockEntity;
import com.lit.spaceships.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SpaceshipShieldMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    private final SpaceshipShieldBlockEntity blockEntity;
    private final ContainerData data;

    // Konstruktor für den Client
    public SpaceshipShieldMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(16));
    }

    // Konstruktor für den Server
    public SpaceshipShieldMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.SHIELD_MENU.get(), containerId);
        checkContainerDataCount(data, 16);
        this.access = ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos());
        this.blockEntity = (SpaceshipShieldBlockEntity) entity;
        this.data = data;

        // DataSlots hinzufügen
        this.addDataSlots(data);
    }

    public int getCurrentEnergy() {
        return this.data.get(0);
    }

    public int getMaxEnergy() {
        return this.data.get(1);
    }

    public boolean isShieldActive() {
        return this.data.get(2) == 1;
    }

    public int getEnergyDeficit() {
        return this.data.get(3);
    }

    public int getChargeRate() {
        return this.data.get(4);
    }

    public int getCooldownRemainingTicks() {
        return this.data.get(5);
    }

    public int getAssignedVoxelCount() {
        return this.data.get(6);
    }

    public int getTotalShipVoxelCount() {
        return this.data.get(7);
    }

    public int getSectorId() {
        return this.data.get(8);
    }

    public int getTotalZonesCount() {
        return this.data.get(9);
    }

    public int getMinRelX() {
        return this.data.get(10);
    }

    public int getMaxRelX() {
        return this.data.get(11);
    }

    public int getMinRelY() {
        return this.data.get(12);
    }

    public int getMaxRelY() {
        return this.data.get(13);
    }

    public int getMinRelZ() {
        return this.data.get(14);
    }

    public int getMaxRelZ() {
        return this.data.get(15);
    }

    public int getSpanX() {
        return getAssignedVoxelCount() > 0 ? (getMaxRelX() - getMinRelX() + 1) : 0;
    }

    public int getSpanY() {
        return getAssignedVoxelCount() > 0 ? (getMaxRelY() - getMinRelY() + 1) : 0;
    }

    public int getSpanZ() {
        return getAssignedVoxelCount() > 0 ? (getMaxRelZ() - getMinRelZ() + 1) : 0;
    }

    public float getCoverageRatio() {
        int total = getTotalShipVoxelCount();
        if (total <= 0) return 0.0f;
        return ((float) getAssignedVoxelCount() / (float) total) * 100.0f;
    }

    public SpaceshipShieldBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, blockEntity.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        return ItemStack.EMPTY;
    }
}
