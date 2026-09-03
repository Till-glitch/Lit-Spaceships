package com.lit.spaceships.network;

import com.lit.spaceships.LitSpaceships;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ShipMovementRequestPayload(UUID shipId, float impulseForward, float impulseLeft, float impulseUp) implements CustomPacketPayload {

    public static final Type<ShipMovementRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "ship_movement_request"));

    public static final StreamCodec<ByteBuf, ShipMovementRequestPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ShipMovementRequestPayload::shipId,
            ByteBufCodecs.FLOAT, ShipMovementRequestPayload::impulseForward,
            ByteBufCodecs.FLOAT, ShipMovementRequestPayload::impulseLeft,
            ByteBufCodecs.FLOAT, ShipMovementRequestPayload::impulseUp,
            ShipMovementRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
