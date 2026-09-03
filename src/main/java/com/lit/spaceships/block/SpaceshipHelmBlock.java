package com.lit.spaceships.block;

import com.lit.spaceships.block.entity.SpaceshipHelmBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SpaceshipHelmBlock extends Block implements EntityBlock {

    public SpaceshipHelmBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpaceshipHelmBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player.isShiftKeyDown()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof com.lit.spaceships.block.ISpaceshipNode node && node.getShipId() != null) {
                    com.lit.spaceships.ship.domain.ShipState ship = com.lit.spaceships.ship.service.ServerShipManager.getShip(node.getShipId());
                    if (ship != null) {
                        int energy = com.lit.spaceships.ship.SpaceshipEnergyManager.getTotalAvailableEnergy(level, ship);
                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                                new com.lit.spaceships.network.ShipStateSyncPayload(ship.getId(), energy, ship.isShieldActive(),
                                        ship.getShieldCooldownRemaining(level.getGameTime()),
                                        ship.getMovementCooldownRemaining(level.getGameTime())));
                    }
                }
                serverPlayer.openMenu(new net.minecraft.world.MenuProvider() {
                    @Override
                    public net.minecraft.network.chat.Component getDisplayName() {
                        return net.minecraft.network.chat.Component.translatable(com.lit.spaceships.registry.ModI18n.Screen.HELM_NAV_TITLE);
                    }

                    @Nullable
                    @Override
                    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player player) {
                        return new com.lit.spaceships.menu.SpaceshipHelmMenu(id, inv, pos);
                    }
                }, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.SUCCESS;
    }
}