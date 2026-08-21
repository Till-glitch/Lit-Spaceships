package com.peaceman.alpha.block.entity;

import com.peaceman.alpha.block.ISpaceshipNode;
import com.peaceman.alpha.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Basis-Klasse für alle Schiffs-Knotenpunkte.
 * Nutzt moderne NeoForge 1.21 Data Attachments für die typsichere Speicherung der Schiffs-UUID.
 */
public abstract class AbstractSpaceshipNodeBlockEntity extends BlockEntity implements ISpaceshipNode {

    public AbstractSpaceshipNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // --- GETTER & SETTER via Data Attachment ---
    @Override
    public UUID getShipId() {
        return this.hasData(ModAttachments.SHIP_ID) ? this.getData(ModAttachments.SHIP_ID) : null;
    }

    @Override
    public void setShipId(UUID shipId) {
        if (shipId != null) {
            this.setData(ModAttachments.SHIP_ID, shipId);
        } else {
            this.removeData(ModAttachments.SHIP_ID);
        }
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    // --- NETZWERK SYNC ---
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}