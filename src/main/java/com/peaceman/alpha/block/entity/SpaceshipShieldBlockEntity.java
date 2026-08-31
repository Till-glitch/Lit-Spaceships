package com.peaceman.alpha.block.entity;

import com.peaceman.alpha.menu.SpaceshipShieldMenu;
import com.peaceman.alpha.registry.ModBlockEntities;
import com.peaceman.alpha.ship.domain.ShieldZone;
import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ServerShipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * BlockEntity für den Raumschiff-Schildgenerator.
 * Erbt von AbstractSpaceshipNodeBlockEntity für natives Data Attachment Tracking.
 */
public class SpaceshipShieldBlockEntity extends AbstractSpaceshipNodeBlockEntity implements MenuProvider {

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            UUID shipId = getShipId();
            if (shipId == null) return 0;
            ShipState ship = ServerShipManager.getShip(shipId);
            if (ship == null) return 0;

            ShieldZone myZone = null;
            for (ShieldZone zone : ship.getShieldZones().values()) {
                if (zone.generatorPos().equals(worldPosition)) {
                    myZone = zone;
                    break;
                }
            }

            return switch (index) {
                case 0 -> myZone != null ? myZone.currentEnergy() : 0;
                case 1 -> myZone != null ? myZone.maxEnergy() : 0;
                case 2 -> (myZone != null && myZone.isEnabled()) ? 1 : 0;
                case 3 -> myZone != null ? (myZone.maxEnergy() - myZone.currentEnergy()) : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Client-seitig setzen wir hier nichts.
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public SpaceshipShieldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPACESHIP_SHIELD_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.peaceman_alpha.spaceship_shield");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SpaceshipShieldMenu(containerId, playerInventory, this, this.data);
    }
}