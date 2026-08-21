package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Hocheffizientes Delta-Payload zur Synchronisation entfernter/zerstörter Voxel eines Raumschiffs.
 * Reduziert die Netzwerklast bei Kollisionen von O(N) auf O(k).
 */
public record ShipStructureDeltaPayload(
        UUID shipId,
        List<BlockPos> removedBlocks
) implements CustomPacketPayload {

    public static final Type<ShipStructureDeltaPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "ship_structure_delta"));

    public static final StreamCodec<FriendlyByteBuf, ShipStructureDeltaPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.shipId());
                buf.writeVarInt(payload.removedBlocks().size());
                for (BlockPos pos : payload.removedBlocks()) {
                    buf.writeBlockPos(pos);
                }
            },
            buf -> {
                UUID id = buf.readUUID();
                int size = buf.readVarInt();
                List<BlockPos> removed = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    removed.add(buf.readBlockPos());
                }
                return new ShipStructureDeltaPayload(id, removed);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
