package com.lit.spaceships.block.entity;

import com.lit.spaceships.menu.SpaceshipShieldMenu;
import com.lit.spaceships.registry.ModBlockEntities;
import com.lit.spaceships.ship.domain.ShieldZone;
import com.lit.spaceships.ship.domain.ShipState;
import com.lit.spaceships.ship.service.ServerShipManager;
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
                if (zone.generatorPos() != null && zone.generatorPos().equals(worldPosition)) {
                    myZone = zone;
                    break;
                }
            }

            com.lit.spaceships.ship.domain.SectorCoverage coverage = (myZone != null) ? ship.getSectorCoverage(myZone.id()) : null;
            long gameTime = (level != null) ? level.getGameTime() : 0L;
            long cdRemaining = (myZone != null && myZone.cooldownUntil() > gameTime) ? (myZone.cooldownUntil() - gameTime) : 0L;

            return switch (index) {
                case 0 -> myZone != null ? myZone.currentEnergy() : 0;
                case 1 -> myZone != null ? myZone.maxEnergy() : 0;
                case 2 -> (myZone != null && myZone.isEnabled()) ? 1 : 0;
                case 3 -> myZone != null ? (myZone.maxEnergy() - myZone.currentEnergy()) : 0;
                case 4 -> myZone != null ? myZone.lastChargeRate() : 0;
                case 5 -> (int) Math.min(Integer.MAX_VALUE, cdRemaining);
                case 6 -> coverage != null ? coverage.assignedVoxels() : 0;
                case 7 -> coverage != null ? coverage.totalShipVoxels() : (ship.getBlocks() != null ? ship.getBlocks().size() : 0);
                case 8 -> myZone != null ? (myZone.id() & 0xFF) : 0;
                case 9 -> ship.getShieldZones().size();
                case 10 -> coverage != null && coverage.minRelative() != null ? coverage.minRelative().getX() : 0;
                case 11 -> coverage != null && coverage.maxRelative() != null ? coverage.maxRelative().getX() : 0;
                case 12 -> coverage != null && coverage.minRelative() != null ? coverage.minRelative().getY() : 0;
                case 13 -> coverage != null && coverage.maxRelative() != null ? coverage.maxRelative().getY() : 0;
                case 14 -> coverage != null && coverage.minRelative() != null ? coverage.minRelative().getZ() : 0;
                case 15 -> coverage != null && coverage.maxRelative() != null ? coverage.maxRelative().getZ() : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Client-seitig setzen wir hier nichts.
        }

        @Override
        public int getCount() {
            return 16;
        }
    };

    public SpaceshipShieldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPACESHIP_SHIELD_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.lit_spaceships.spaceship_shield");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SpaceshipShieldMenu(containerId, playerInventory, this, this.data);
    }
}