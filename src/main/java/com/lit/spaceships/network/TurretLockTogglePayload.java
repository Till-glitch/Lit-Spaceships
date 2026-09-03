package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload zum Umschalten (Lock / Unlock) der Zielausrichtung des Geschützes per Linksklick.
 */
public record TurretLockTogglePayload(BlockPos weaponPos) implements CustomPacketPayload {

    public static final Type<TurretLockTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "turret_lock_toggle"));

    public static final StreamCodec<ByteBuf, TurretLockTogglePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TurretLockTogglePayload::weaponPos,
            TurretLockTogglePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
