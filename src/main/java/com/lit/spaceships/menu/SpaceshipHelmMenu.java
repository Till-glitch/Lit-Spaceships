package com.lit.spaceships.menu;

import com.lit.spaceships.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class SpaceshipHelmMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;

    public SpaceshipHelmMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    public SpaceshipHelmMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.HELM_MENU.get(), containerId);
        this.blockPos = pos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
