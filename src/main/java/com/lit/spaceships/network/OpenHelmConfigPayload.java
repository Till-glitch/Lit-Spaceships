package com.lit.spaceships.network;

import com.lit.spaceships.LitSpaceships;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

public record OpenHelmConfigPayload(Optional<UUID> shipId) implements CustomPacketPayload {

    public static final Type<OpenHelmConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "open_helm_config"));

    public static final StreamCodec<ByteBuf, OpenHelmConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), OpenHelmConfigPayload::shipId,
            OpenHelmConfigPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
