package com.lit.spaceships.ship.relocation;

import com.lit.spaceships.ship.relocation.util.NbtCoordinateRemapper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NbtCoordinateRemapper Unit Tests")
class NbtCoordinateRemapperTest {

    @Test
    @DisplayName("Remappt {x, y, z} und {X, Y, Z} im CompoundTag")
    void testRemapXYZCompounds() {
        BlockPos oldPos1 = new BlockPos(10, 64, 20);
        BlockPos oldPos2 = new BlockPos(15, 65, 25);
        BlockPos externalPos = new BlockPos(100, 100, 100);

        Set<BlockPos> shipBlocks = Set.of(oldPos1, oldPos2);

        CompoundTag tag = new CompoundTag();
        CompoundTag masterTag = new CompoundTag();
        masterTag.putInt("x", oldPos1.getX());
        masterTag.putInt("y", oldPos1.getY());
        masterTag.putInt("z", oldPos1.getZ());
        tag.put("master", masterTag);

        CompoundTag targetTag = new CompoundTag();
        targetTag.putInt("X", oldPos2.getX());
        targetTag.putInt("Y", oldPos2.getY());
        targetTag.putInt("Z", oldPos2.getZ());
        tag.put("target", targetTag);

        CompoundTag externalTag = new CompoundTag();
        externalTag.putInt("x", externalPos.getX());
        externalTag.putInt("y", externalPos.getY());
        externalTag.putInt("z", externalPos.getZ());
        tag.put("external", externalTag);

        // Verschiebung um dx = 5, dy = 1, dz = -3
        boolean modified = NbtCoordinateRemapper.remapCoordinates(tag, shipBlocks, p -> p.offset(5, 1, -3));
        assertTrue(modified, "Tag muss modifiziert worden sein");

        // master prüfen: (10, 64, 20) -> (15, 65, 17)
        CompoundTag newMaster = tag.getCompound("master");
        assertEquals(15, newMaster.getInt("x"));
        assertEquals(65, newMaster.getInt("y"));
        assertEquals(17, newMaster.getInt("z"));

        // target prüfen: (15, 65, 25) -> (20, 66, 22)
        CompoundTag newTarget = tag.getCompound("target");
        assertEquals(20, newTarget.getInt("X"));
        assertEquals(66, newTarget.getInt("Y"));
        assertEquals(22, newTarget.getInt("Z"));

        // external prüfen: darf NICHT verändert werden
        CompoundTag newExternal = tag.getCompound("external");
        assertEquals(100, newExternal.getInt("x"));
        assertEquals(100, newExternal.getInt("y"));
        assertEquals(100, newExternal.getInt("z"));
    }

    @Test
    @DisplayName("Remappt IntArrayTag [x, y, z] und LongTag masterPos")
    void testRemapIntArrayAndLongTag() {
        BlockPos oldPos = new BlockPos(2, 3, 4);
        Set<BlockPos> shipBlocks = Set.of(oldPos);

        CompoundTag tag = new CompoundTag();
        tag.putIntArray("controller_pos", new int[]{oldPos.getX(), oldPos.getY(), oldPos.getZ()});
        tag.putLong("masterPos", oldPos.asLong());

        boolean modified = NbtCoordinateRemapper.remapCoordinates(tag, shipBlocks, p -> p.offset(10, 20, 30));
        assertTrue(modified);

        int[] arr = tag.getIntArray("controller_pos");
        assertEquals(12, arr[0]);
        assertEquals(23, arr[1]);
        assertEquals(34, arr[2]);

        BlockPos newPosFromLong = BlockPos.of(tag.getLong("masterPos"));
        assertEquals(new BlockPos(12, 23, 34), newPosFromLong);
    }

    @Test
    @DisplayName("Remappt verschachtelte ListTags mit Multiblock-Referenzen")
    void testRemapListTag() {
        BlockPos slave1 = new BlockPos(5, 10, 15);
        BlockPos slave2 = new BlockPos(6, 10, 15);
        Set<BlockPos> shipBlocks = Set.of(slave1, slave2);

        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();

        CompoundTag elem1 = new CompoundTag();
        elem1.putInt("x", slave1.getX());
        elem1.putInt("y", slave1.getY());
        elem1.putInt("z", slave1.getZ());
        list.add(elem1);

        CompoundTag elem2 = new CompoundTag();
        elem2.putInt("x", slave2.getX());
        elem2.putInt("y", slave2.getY());
        elem2.putInt("z", slave2.getZ());
        list.add(elem2);

        root.put("parts", list);

        boolean modified = NbtCoordinateRemapper.remapCoordinates(root, shipBlocks, p -> p.offset(-1, -1, -1));
        assertTrue(modified);

        ListTag updatedList = root.getList("parts", 10);
        assertEquals(4, updatedList.getCompound(0).getInt("x"));
        assertEquals(9, updatedList.getCompound(0).getInt("y"));
        assertEquals(14, updatedList.getCompound(0).getInt("z"));

        assertEquals(5, updatedList.getCompound(1).getInt("x"));
        assertEquals(9, updatedList.getCompound(1).getInt("y"));
        assertEquals(14, updatedList.getCompound(1).getInt("z"));
    }
}
