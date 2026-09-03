package com.lit.spaceships.network;

import com.lit.spaceships.LitSpaceships;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload zur kontinuierlichen Synchronisation der Turm-Ausrichtung (Yaw & Pitch)
 * vom Client an den Server und Broadcast an Tracking-Clients.
 */
public record TurretAimSyncPayload(BlockPos weaponPos, float yaw, float pitch) implements CustomPacketPayload {

    public static final Type<TurretAimSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "turret_aim_sync"));

    public static final StreamCodec<ByteBuf, TurretAimSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TurretAimSyncPayload::weaponPos,
            ByteBufCodecs.FLOAT,
            TurretAimSyncPayload::yaw,
            ByteBufCodecs.FLOAT,
            TurretAimSyncPayload::pitch,
            TurretAimSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
