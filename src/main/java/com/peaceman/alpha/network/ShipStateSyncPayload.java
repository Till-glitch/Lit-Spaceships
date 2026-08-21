package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Hochfrequentes Delta-Update für dynamische Schiffsdaten (Energie, Schild-Status, Cooldowns).
 */
public record ShipStateSyncPayload(
        UUID shipId,
        int currentEnergy,
        boolean isShieldActive,
        long shieldCooldownRemainingTicks,
        long movementCooldownRemainingTicks
) implements CustomPacketPayload {

    public static final Type<ShipStateSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "ship_state_sync"));

    public static final StreamCodec<ByteBuf, ShipStateSyncPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ShipStateSyncPayload::shipId,
            ByteBufCodecs.VAR_INT, ShipStateSyncPayload::currentEnergy,
            ByteBufCodecs.BOOL, ShipStateSyncPayload::isShieldActive,
            ByteBufCodecs.VAR_LONG, ShipStateSyncPayload::shieldCooldownRemainingTicks,
            ByteBufCodecs.VAR_LONG, ShipStateSyncPayload::movementCooldownRemainingTicks,
            ShipStateSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
