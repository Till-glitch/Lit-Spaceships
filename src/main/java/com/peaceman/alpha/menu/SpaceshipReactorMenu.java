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
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    // Konstruktor für den Server
    public SpaceshipReactorMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.REACTOR_MENU.get(), containerId);
        // Wir prüfen das Inventar nicht mehr auf Größe, da wir es nicht nutzen
        checkContainerDataCount(data, 2);
        this.access = ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos());
        this.blockEntity = (SpaceshipReactorBlockEntity) entity;
        this.data = data;

        // KEIN Spieler-Inventar mehr hinzufügen!

        // DataSlots hinzufügen
        this.addDataSlots(data);
    }

    public int getCurrentEnergy() {
        return this.data.get(0);
    }

    public int getMaxEnergy() {
        return this.data.get(1);
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
