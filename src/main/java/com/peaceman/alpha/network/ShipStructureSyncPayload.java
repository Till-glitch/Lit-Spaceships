package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Überträgt die strukturellen Basisdaten eines Schiffs (relative Block-Offsets).
 * Wird gezielt via Spatial Hashing gesendet, wenn ein Spieler den entsprechenden Chunk lädt.
 */
public record ShipStructureSyncPayload(
        UUID shipId,
        BlockPos controllerPos,
        Set<BlockPos> relativeBlocks
) implements CustomPacketPayload {

    public static final Type<ShipStructureSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "ship_structure_sync"));

    public static final StreamCodec<FriendlyByteBuf, ShipStructureSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.shipId());
                buf.writeBlockPos(packet.controllerPos());
                buf.writeVarInt(packet.relativeBlocks().size());
                for (BlockPos pos : packet.relativeBlocks()) {
                    buf.writeVarInt(pos.getX());
                    buf.writeVarInt(pos.getY());
                    buf.writeVarInt(pos.getZ());
                }
            },
            buf -> {
                UUID id = buf.readUUID();
                BlockPos anchor = buf.readBlockPos();
                int size = buf.readVarInt();
                Set<BlockPos> blocks = new HashSet<>(size);
                for (int i = 0; i < size; i++) {
                    blocks.add(new BlockPos(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
                }
                return new ShipStructureSyncPayload(id, anchor, blocks);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
