package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record ShipMovementRequestPayload(UUID shipId, float impulseForward, float impulseLeft, float impulseUp) implements CustomPacketPayload {

    public static final Type<ShipMovementRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "ship_movement_request"));

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
