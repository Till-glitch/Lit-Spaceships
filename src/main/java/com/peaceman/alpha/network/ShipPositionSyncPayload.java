package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Leichtgewichtige Positions-Synchronisation bei translatorischen Schiffsbewegungen.
 * Aktualisiert ausschließlich die anchorPos auf dem Client, ohne das VBO-Mesh neu zu bauen (Bug 2 Fix).
 */
public record ShipPositionSyncPayload(
        UUID shipId,
        BlockPos newAnchorPos
) implements CustomPacketPayload {

    public static final Type<ShipPositionSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "ship_position_sync"));

    public static final StreamCodec<ByteBuf, ShipPositionSyncPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ShipPositionSyncPayload::shipId,
            BlockPos.STREAM_CODEC, ShipPositionSyncPayload::newAnchorPos,
            ShipPositionSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
