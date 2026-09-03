package com.lit.spaceships.network;

import com.lit.spaceships.LitSpaceships;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Bandbreitenoptimiertes Netzwerk-Paket (< 32 Bytes) zur Synchronisation des
 * 64-Bit Aktivitäts-Zustands aller lokalen Schildzonen eines Schiffes.
 */
public record ShieldZoneStatePayload(
        UUID shipId,
        long activeMask,
        byte[] zoneEnergies
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShieldZoneStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "shield_zone_state"));

    public static final StreamCodec<FriendlyByteBuf, ShieldZoneStatePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            ShieldZoneStatePayload::shipId,
            ByteBufCodecs.VAR_LONG,
            ShieldZoneStatePayload::activeMask,
            ByteBufCodecs.byteArray(64),
            ShieldZoneStatePayload::zoneEnergies,
            ShieldZoneStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
