package com.lit.spaceships.network;

import com.lit.spaceships.LitSpaceships;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

// Die Record-Definition bekommt ein Feld mehr: anchorPos
public record ShieldBubbleSyncPacket(UUID shipId, BlockPos anchorPos, java.util.Map<BlockPos, Byte> relativeBubbleBlocks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShieldBubbleSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "shield_bubble_sync"));

    @Override
    public CustomPacketPayload.Type<ShieldBubbleSyncPacket> type() { return TYPE; }

    public static final StreamCodec<FriendlyByteBuf, ShieldBubbleSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.shipId());
                buf.writeBlockPos(packet.anchorPos()); // NEU: Anker mitsenden
                buf.writeInt(packet.relativeBubbleBlocks().size());
                for (java.util.Map.Entry<BlockPos, Byte> entry : packet.relativeBubbleBlocks().entrySet()) {
                    buf.writeLong(entry.getKey().asLong());
                    buf.writeByte(entry.getValue());
                }
            },
            buf -> {
                UUID id = buf.readUUID();
                BlockPos anchor = buf.readBlockPos(); // NEU: Anker auslesen
                int size = buf.readInt();
                java.util.Map<BlockPos, Byte> blocks = new java.util.HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    blocks.put(BlockPos.of(buf.readLong()), buf.readByte());
                }
                return new ShieldBubbleSyncPacket(id, anchor, blocks);
            }
    );
}