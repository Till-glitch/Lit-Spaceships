package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Netzwerkpaket zur Übertragung von Geschützturm-Zielwinkeln.
 * Nutzt 16-Bit Short Kompression zur Minimierung der Bandbreite bei hoher Update-Frequenz.
 */
public record TurretAimPayload(BlockPos weaponPos, short compressedYaw, short compressedPitch) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TurretAimPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "turret_aim"));

    public static final StreamCodec<ByteBuf, TurretAimPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TurretAimPayload::weaponPos,
            ByteBufCodecs.SHORT, TurretAimPayload::compressedYaw,
            ByteBufCodecs.SHORT, TurretAimPayload::compressedPitch,
            TurretAimPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
