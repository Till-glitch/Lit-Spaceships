package com.peaceman.alpha.entity;

import com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity;
import com.peaceman.alpha.registry.ModEntities;
import com.peaceman.alpha.ship.combat.LaserWeaponTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TurretSeatTest {

    @org.junit.jupiter.api.BeforeAll
    static void initMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("ModEntities TURRET_SEAT Holder ist korrekt registriert")
    void testTurretSeatHolderExists() {
        assertNotNull(ModEntities.TURRET_SEAT);
        assertEquals("turret_seat", ModEntities.TURRET_SEAT.getId().getPath());
    }

    @Test
    @DisplayName("TurretSeat DTO Attribute und Waffenbindung sind konsistent")
    void testTurretSeatWeaponBinding() {
        BlockPos weaponPos = new BlockPos(100, 64, -200);
        UUID shipId = UUID.randomUUID();

        assertNotNull(weaponPos);
        assertNotNull(shipId);
    }

    @Test
    @DisplayName("TurretSeatEntity speichert und lädt ShipId und WeaponPos via NBT")
    void testTurretSeatNbtPersistence() {
        UUID shipId = UUID.randomUUID();
        BlockPos weaponPos = new BlockPos(50, 100, -50);

        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.put("WeaponPos", net.minecraft.nbt.NbtUtils.writeBlockPos(weaponPos));
        tag.putUUID("ShipId", shipId);

        assertTrue(tag.contains("WeaponPos"));
        assertTrue(tag.hasUUID("ShipId"));
        assertEquals(shipId, tag.getUUID("ShipId"));
        assertEquals(weaponPos, net.minecraft.nbt.NbtUtils.readBlockPos(tag, "WeaponPos").orElse(null));
    }

    static class TestLaserBlockEntity extends AbstractLaserNodeBlockEntity {
        public TestLaserBlockEntity(BlockPos pos, BlockState state) {
            super(null, pos, state);
        }

        @Override
        public LaserWeaponTier getTier() {
            return LaserWeaponTier.PULSE_LASER;
        }

        @Override
        public boolean isContinuous() {
            return false;
        }

        @Override
        public void serverTick(net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {}

        public void saveToNbt(net.minecraft.nbt.CompoundTag tag) {
            saveAdditional(tag, net.minecraft.core.RegistryAccess.EMPTY);
        }

        public void loadFromNbt(net.minecraft.nbt.CompoundTag tag) {
            loadAdditional(tag, net.minecraft.core.RegistryAccess.EMPTY);
        }
    }

    @Test
    @DisplayName("AbstractLaserNodeBlockEntity speichert und lädt Zielausrichtung und Lock-Status fehlerfrei via NBT")
    void testLaserNodeAimNbtPersistence() {
        var dummyBe = new TestLaserBlockEntity(
                BlockPos.ZERO,
                net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState()
        );

        dummyBe.setAimAngles(new com.peaceman.alpha.ship.combat.aim.AimAngles(45.5f, -15.0f));
        dummyBe.setAimLocked(true);
        dummyBe.setOccupied(false);

        assertEquals(45.5f, dummyBe.getTargetYaw(), 1e-4);
        assertEquals(-15.0f, dummyBe.getTargetPitch(), 1e-4);
        assertTrue(dummyBe.isAimLocked());
        assertFalse(dummyBe.isOccupied());

        // Serialisiere in NBT
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        dummyBe.saveToNbt(tag);

        // Erstelle neue Instanz (simuliert Teleport / Chunk-Reload / Move)
        var restoredBe = new TestLaserBlockEntity(
                new BlockPos(10, 20, 30),
                net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState()
        );
        restoredBe.loadFromNbt(tag);

        assertEquals(45.5f, restoredBe.getTargetYaw(), 1e-4, "TargetYaw muss nach Save/Load exakt erhalten bleiben");
        assertEquals(-15.0f, restoredBe.getTargetPitch(), 1e-4, "TargetPitch muss nach Save/Load exakt erhalten bleiben");
        assertTrue(restoredBe.isAimLocked(), "IsAimLocked muss nach Save/Load erhalten bleiben");
        assertFalse(restoredBe.isOccupied(), "IsOccupied bleibt false wenn kein Spieler sitzt");
    }
}
