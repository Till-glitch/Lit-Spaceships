package com.lit.spaceships.network;

import com.lit.spaceships.LitSpaceships;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Überträgt Dimensionswechsel eines Raumschiffs an alle verbundenen Clients,
 * um clientseitige VBOs bei Dimensionsgrenzen zu isolieren und VRAM-Leaks zu verhindern.
 */
public record ShipDimensionSyncPayload(UUID shipId, ResourceKey<Level> dimension) implements CustomPacketPayload {

    public static final Type<ShipDimensionSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "ship_dimension_sync"));

    public static final StreamCodec<FriendlyByteBuf, ShipDimensionSyncPayload> STREAM_CODEC =
            StreamCodec.ofMember(ShipDimensionSyncPayload::write, ShipDimensionSyncPayload::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(shipId);
        buf.writeResourceLocation(dimension.location());
    }

    public static ShipDimensionSyncPayload read(FriendlyByteBuf buf) {
        UUID shipId = buf.readUUID();
        ResourceLocation loc = buf.readResourceLocation();
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, loc);
        return new ShipDimensionSyncPayload(shipId, dim);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
