package com.peaceman.alpha.menu;

import com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity;
import com.peaceman.alpha.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SpaceshipReactorMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    private final SpaceshipReactorBlockEntity blockEntity;
    private final ContainerData data;

    // Konstruktor für den Client
    public SpaceshipReactorMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(14));
    }

    // Konstruktor für den Server
    public SpaceshipReactorMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.REACTOR_MENU.get(), containerId);
        checkContainerDataCount(data, 14);
        this.access = ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos());
        this.blockEntity = (SpaceshipReactorBlockEntity) entity;
        this.data = data;

        // DataSlots hinzufügen
        this.addDataSlots(data);
    }

    public SpaceshipReactorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getCurrentEnergy() {
        return this.data.get(0);
    }

    public int getMaxEnergy() {
        return this.data.get(1);
    }

    public int getTotalShipEnergy() {
        return this.data.get(2);
    }

    public int getTotalShipMaxEnergy() {
        return this.data.get(3);
    }

    public int getGenerationRate() {
        return this.data.get(4);
    }

    public int getConsumptionRate() {
        return this.data.get(5);
    }

    public int getNetThroughput() {
        return this.data.get(6);
    }

    public com.peaceman.alpha.ship.domain.PowerPriority getPowerPriority() {
        return com.peaceman.alpha.ship.domain.PowerPriority.fromId(this.data.get(7));
    }

    public int getStabilityPercentage() {
        return this.data.get(8);
    }

    public int getOperationalStatus() {
        return this.data.get(9);
    }

    public int getReactorCount() {
        return this.data.get(10);
    }

    public int getShieldDrainRate() {
        return this.data.get(11);
    }

    public int getWeaponDrainRate() {
        return this.data.get(12);
    }

    public int getEngineDrainRate() {
        return this.data.get(13);
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
