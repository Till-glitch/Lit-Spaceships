package com.peaceman.alpha.menu;

import com.peaceman.alpha.block.entity.SpaceshipShieldBlockEntity;
import com.peaceman.alpha.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SpaceshipShieldMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    private final SpaceshipShieldBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // Konstruktor für den Client
    public SpaceshipShieldMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    // Konstruktor für den Server
    public SpaceshipShieldMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.SHIELD_MENU.get(), containerId);
        checkContainerDataCount(data, 4);
        this.access = ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos());
        this.level = inv.player.level();
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
