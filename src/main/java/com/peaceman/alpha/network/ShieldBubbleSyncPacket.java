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

// Die Record-Definition bekommt ein Feld mehr: anchorPos
public record ShieldBubbleSyncPacket(UUID shipId, BlockPos anchorPos, Set<BlockPos> relativeBubbleBlocks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShieldBubbleSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "shield_bubble_sync"));

    @Override
    public CustomPacketPayload.Type<ShieldBubbleSyncPacket> type() { return TYPE; }

    public static final StreamCodec<FriendlyByteBuf, ShieldBubbleSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.shipId());
                buf.writeBlockPos(packet.anchorPos()); // NEU: Anker mitsenden
                buf.writeInt(packet.relativeBubbleBlocks().size());
                for (BlockPos pos : packet.relativeBubbleBlocks()) {
                    buf.writeLong(pos.asLong());
                }
            },
            buf -> {
                UUID id = buf.readUUID();
                BlockPos anchor = buf.readBlockPos(); // NEU: Anker auslesen
                int size = buf.readInt();
                Set<BlockPos> blocks = new HashSet<>(size);
                for (int i = 0; i < size; i++) {
                    blocks.add(BlockPos.of(buf.readLong()));
                }
                return new ShieldBubbleSyncPacket(id, anchor, blocks);
            }
    );
}