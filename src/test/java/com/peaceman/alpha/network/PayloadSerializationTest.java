package com.peaceman.alpha.network;

import com.peaceman.alpha.ship.combat.LaserWeaponTier;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-Tests für die symmetrische Serialisierung und Deserialisierung aller Netzwerk-Payloads.
 */
public class PayloadSerializationTest {

    @Test
    @DisplayName("ShipStateSyncPayload serialisiert und deserialisiert fehlerfrei")
    void testShipStateSyncPayload_Codec() {
        UUID shipId = UUID.randomUUID();
        ShipStateSyncPayload original = new ShipStateSyncPayload(shipId, 50000, true, 120L, 15L);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ShipStateSyncPayload.STREAM_CODEC.encode(buf, original);
        ShipStateSyncPayload decoded = ShipStateSyncPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.shipId(), decoded.shipId());
        assertEquals(original.currentEnergy(), decoded.currentEnergy());
        assertEquals(original.isShieldActive(), decoded.isShieldActive());
        assertEquals(original.shieldCooldownRemainingTicks(), decoded.shieldCooldownRemainingTicks());
        assertEquals(original.movementCooldownRemainingTicks(), decoded.movementCooldownRemainingTicks());
    }

    @Test
    @DisplayName("ShipPositionSyncPayload serialisiert und deserialisiert fehlerfrei")
    void testShipPositionSyncPayload_Codec() {
        UUID shipId = UUID.randomUUID();
        BlockPos pos = new BlockPos(123, -45, 678);
        ShipPositionSyncPayload original = new ShipPositionSyncPayload(shipId, pos);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ShipPositionSyncPayload.STREAM_CODEC.encode(buf, original);
        ShipPositionSyncPayload decoded = ShipPositionSyncPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.shipId(), decoded.shipId());
        assertEquals(original.newAnchorPos(), decoded.newAnchorPos());
    }

    @Test
    @DisplayName("ShipImpactEventPayload serialisiert und deserialisiert fehlerfrei")
    void testShipImpactEventPayload_Codec() {
        UUID shipId = UUID.randomUUID();
        Vec3 impact = new Vec3(1.5, 2.5, 3.5);
        ShipImpactEventPayload original = new ShipImpactEventPayload(shipId, impact, 0.85f);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ShipImpactEventPayload.STREAM_CODEC.encode(buf, original);
        ShipImpactEventPayload decoded = ShipImpactEventPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.shipId(), decoded.shipId());
        assertEquals(original.impactPos().x, decoded.impactPos().x, 1e-5);
        assertEquals(original.impactPos().y, decoded.impactPos().y, 1e-5);
        assertEquals(original.impactPos().z, decoded.impactPos().z, 1e-5);
        assertEquals(original.force(), decoded.force(), 1e-5f);
    }

    @Test
    @DisplayName("ShipStructureDeltaPayload serialisiert und deserialisiert fehlerfrei")
    void testShipStructureDeltaPayload_Codec() {
        UUID shipId = UUID.randomUUID();
        List<BlockPos> removed = List.of(new BlockPos(1, 2, 3), new BlockPos(4, 5, 6));
        ShipStructureDeltaPayload original = new ShipStructureDeltaPayload(shipId, removed);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ShipStructureDeltaPayload.STREAM_CODEC.encode(buf, original);
        ShipStructureDeltaPayload decoded = ShipStructureDeltaPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.shipId(), decoded.shipId());
        assertEquals(original.removedBlocks().size(), decoded.removedBlocks().size());
        assertEquals(original.removedBlocks(), decoded.removedBlocks());
    }

    @Test
    @DisplayName("ShipStructureSyncPayload serialisiert und deserialisiert fehlerfrei")
    void testShipStructureSyncPayload_Codec() {
        UUID shipId = UUID.randomUUID();
        BlockPos anchor = new BlockPos(100, 64, 100);
        Set<BlockPos> blocks = Set.of(new BlockPos(100, 64, 100), new BlockPos(101, 64, 100));
        ShipStructureSyncPayload original = new ShipStructureSyncPayload(shipId, anchor, blocks);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ShipStructureSyncPayload.STREAM_CODEC.encode(buf, original);
        ShipStructureSyncPayload decoded = ShipStructureSyncPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.shipId(), decoded.shipId());
        assertEquals(original.controllerPos(), decoded.controllerPos());
        assertEquals(original.relativeBlocks().size(), decoded.relativeBlocks().size());
    }

    @Test
    @DisplayName("ShipStructureSyncPayload mit leerer Blockmenge (Schiffsaufloesung) serialisiert fehlerfrei")
    void testShipStructureSyncPayload_EmptyBlocks_Disassembly() {
        UUID shipId = UUID.randomUUID();
        BlockPos anchor = new BlockPos(10, 64, 10);
        ShipStructureSyncPayload original = new ShipStructureSyncPayload(shipId, anchor, Set.of());

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ShipStructureSyncPayload.STREAM_CODEC.encode(buf, original);
        ShipStructureSyncPayload decoded = ShipStructureSyncPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.shipId(), decoded.shipId());
        assertEquals(original.controllerPos(), decoded.controllerPos());
        assertTrue(decoded.relativeBlocks().isEmpty());
    }

    @Test
    @DisplayName("ShieldBubbleSyncPacket serialisiert und deserialisiert fehlerfrei")
    void testShieldBubbleSyncPacket_Codec() {
        UUID shipId = UUID.randomUUID();
        BlockPos anchor = new BlockPos(50, 70, 50);
        Map<BlockPos, Byte> shieldBlocks = Map.of(new BlockPos(51, 70, 50), (byte) 1, new BlockPos(50, 71, 50), (byte) 2);
        ShieldBubbleSyncPacket original = new ShieldBubbleSyncPacket(shipId, anchor, shieldBlocks);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ShieldBubbleSyncPacket.STREAM_CODEC.encode(buf, original);
        ShieldBubbleSyncPacket decoded = ShieldBubbleSyncPacket.STREAM_CODEC.decode(buf);

        assertEquals(original.shipId(), decoded.shipId());
        assertEquals(original.anchorPos(), decoded.anchorPos());
        assertEquals(original.relativeBubbleBlocks().size(), decoded.relativeBubbleBlocks().size());
    }

    @Test
    @DisplayName("LaserFirePayload serialisiert und deserialisiert fehlerfrei")
    void testLaserFirePayload_Codec() {
        UUID shipId = UUID.randomUUID();
        Vec3 start = new Vec3(10.0, 64.0, 10.0);
        Vec3 end = new Vec3(100.0, 64.0, 10.0);
        LaserFirePayload original = new LaserFirePayload(shipId, start, end, LaserWeaponTier.PULSE_LASER);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        LaserFirePayload.STREAM_CODEC.encode(buf, original);
        LaserFirePayload decoded = LaserFirePayload.STREAM_CODEC.decode(buf);

        assertEquals(original.shooterShipId(), decoded.shooterShipId());
        assertEquals(original.startPos().x, decoded.startPos().x, 1e-5);
        assertEquals(original.endPos().x, decoded.endPos().x, 1e-5);
        assertEquals(original.tier(), decoded.tier());
    }

    @Test
    @DisplayName("LaserStateSyncPayload serialisiert und deserialisiert fehlerfrei")
    void testLaserStateSyncPayload_Codec() {
        UUID shipId = UUID.randomUUID();
        BlockPos weaponPos = new BlockPos(12, 65, 14);
        LaserStateSyncPayload original = new LaserStateSyncPayload(shipId, weaponPos, true, LaserWeaponTier.HEAVY_BEAM);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        LaserStateSyncPayload.STREAM_CODEC.encode(buf, original);
        LaserStateSyncPayload decoded = LaserStateSyncPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.shooterShipId(), decoded.shooterShipId());
        assertEquals(original.weaponPos(), decoded.weaponPos());
        assertTrue(decoded.isFiring());
        assertEquals(original.tier(), decoded.tier());
    }

    @Test
    @DisplayName("ShipCombatActionPayload serialisiert und deserialisiert fehlerfrei")
    void testShipCombatActionPayload_Codec() {
        UUID shipId = UUID.randomUUID();
        ShipCombatActionPayload original = new ShipCombatActionPayload(Optional.of(shipId), ShipCombatActionPayload.CombatAction.FIRE_PULSE, Optional.empty());

        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        ShipCombatActionPayload.STREAM_CODEC.encode(buf, original);
        ShipCombatActionPayload decoded = ShipCombatActionPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.shipId(), decoded.shipId());
        assertEquals(original.action(), decoded.action());
        assertEquals(original.weaponPos(), decoded.weaponPos());
    }

    @Test
    @DisplayName("OpenHelmConfigPayload serialisiert und deserialisiert fehlerfrei")
    void testOpenHelmConfigPayload_Codec() {
        UUID shipId = UUID.randomUUID();
        OpenHelmConfigPayload original = new OpenHelmConfigPayload(Optional.of(shipId));

        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        OpenHelmConfigPayload.STREAM_CODEC.encode(buf, original);
        OpenHelmConfigPayload decoded = OpenHelmConfigPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.shipId(), decoded.shipId());
    }

    @Test
    @DisplayName("ShipActionPayload serialisiert und deserialisiert mit Optional UUID")
    void testShipActionPayload_Codec() {
        UUID shipId = UUID.randomUUID();
        BlockPos pos = new BlockPos(1, 2, 3);
        ShipActionPayload original = new ShipActionPayload(
                Optional.of(shipId), pos, ShipActionPayload.ActionType.MOVE_FORWARD, 10, "Base"
        );

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ShipActionPayload.STREAM_CODEC.encode(buf, original);
        ShipActionPayload decoded = ShipActionPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.shipId(), decoded.shipId());
        assertEquals(original.pos(), decoded.pos());
        assertEquals(original.actionType(), decoded.actionType());
        assertEquals(original.value(), decoded.value());
        assertEquals(original.targetName(), decoded.targetName());
    }

    @Test
    @DisplayName("ShipDimensionSyncPayload serialisiert und deserialisiert fehlerfrei")
    void testShipDimensionSyncPayload_Codec() {
        UUID shipId = UUID.randomUUID();
        ShipDimensionSyncPayload original = new ShipDimensionSyncPayload(shipId, net.minecraft.world.level.Level.NETHER);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ShipDimensionSyncPayload.STREAM_CODEC.encode(buf, original);
        ShipDimensionSyncPayload decoded = ShipDimensionSyncPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.shipId(), decoded.shipId());
        assertEquals(original.dimension(), decoded.dimension());
    }

    @Test
    @DisplayName("TurretAimPayload serialisiert und deserialisiert mit 16-Bit Kompression fehlerfrei")
    void testTurretAimPayload_Codec() {
        BlockPos pos = new BlockPos(45, 78, -120);
        short yaw = (short) 12345;
        short pitch = (short) -6789;
        TurretAimPayload original = new TurretAimPayload(pos, yaw, pitch);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        TurretAimPayload.STREAM_CODEC.encode(buf, original);
        TurretAimPayload decoded = TurretAimPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.weaponPos(), decoded.weaponPos());
        assertEquals(original.compressedYaw(), decoded.compressedYaw());
        assertEquals(original.compressedPitch(), decoded.compressedPitch());
    }

    @Test
    @DisplayName("TurretAimSyncPayload serialisiert und deserialisiert fehlerfrei")
    void testTurretAimSyncPayload_Codec() {
        BlockPos pos = new BlockPos(12, 64, -88);
        float yaw = 45.25f;
        float pitch = -12.75f;
        TurretAimSyncPayload original = new TurretAimSyncPayload(pos, yaw, pitch);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        TurretAimSyncPayload.STREAM_CODEC.encode(buf, original);
        TurretAimSyncPayload decoded = TurretAimSyncPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.weaponPos(), decoded.weaponPos());
        assertEquals(original.yaw(), decoded.yaw(), 1e-4);
        assertEquals(original.pitch(), decoded.pitch(), 1e-4);
    }

    @Test
    @DisplayName("TurretLockTogglePayload serialisiert und deserialisiert fehlerfrei")
    void testTurretLockTogglePayload_Codec() {
        BlockPos pos = new BlockPos(-55, 120, 300);
        TurretLockTogglePayload original = new TurretLockTogglePayload(pos);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        TurretLockTogglePayload.STREAM_CODEC.encode(buf, original);
        TurretLockTogglePayload decoded = TurretLockTogglePayload.STREAM_CODEC.decode(buf);

        assertEquals(original.weaponPos(), decoded.weaponPos());
    }
}
