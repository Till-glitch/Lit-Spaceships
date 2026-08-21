package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.ship.combat.LaserWeaponTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Synchronisiert den kontinuierlichen Feuer-Zustand (Heavy Beam / Mining Laser) an Clients.
 */
public record LaserStateSyncPayload(
        UUID shooterShipId,
        BlockPos weaponPos,
        boolean isFiring,
        LaserWeaponTier tier
) implements CustomPacketPayload {

    public static final Type<LaserStateSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "laser_state_sync"));

    public static final StreamCodec<FriendlyByteBuf, LaserStateSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.shooterShipId());
                buf.writeBlockPos(payload.weaponPos());
                buf.writeBoolean(payload.isFiring());
                buf.writeEnum(payload.tier());
            },
            buf -> {
                UUID id = buf.readUUID();
                BlockPos pos = buf.readBlockPos();
                boolean firing = buf.readBoolean();
                LaserWeaponTier tier = buf.readEnum(LaserWeaponTier.class);
                return new LaserStateSyncPayload(id, pos, firing, tier);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
