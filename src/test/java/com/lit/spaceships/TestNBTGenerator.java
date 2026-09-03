package com.lit.spaceships;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.jupiter.api.Test;

public class TestNBTGenerator {
    @org.junit.jupiter.api.Disabled("Utility generator for empty.nbt structure files")
    @Test
    public void generateEmptyNbt() throws Exception {
        SharedConstants.tryDetectVersion();

        CompoundTag tag = new CompoundTag();
        ListTag size = new ListTag();
        size.add(net.minecraft.nbt.IntTag.valueOf(5));
        size.add(net.minecraft.nbt.IntTag.valueOf(5));
        size.add(net.minecraft.nbt.IntTag.valueOf(5));
        tag.put("size", size);

        ListTag entities = new ListTag();
        tag.put("entities", entities);

        ListTag blocks = new ListTag();
        CompoundTag block = new CompoundTag();
        ListTag pos = new ListTag();
        pos.add(net.minecraft.nbt.IntTag.valueOf(0));
        pos.add(net.minecraft.nbt.IntTag.valueOf(0));
        pos.add(net.minecraft.nbt.IntTag.valueOf(0));
        block.put("pos", pos);
        block.putInt("state", 0);
        blocks.add(block);
        tag.put("blocks", blocks);

        ListTag palette = new ListTag();
        CompoundTag air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        palette.add(air);
        tag.put("palette", palette);

        tag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());

        File dir = new File("src/main/resources/data/lit_spaceships/structures");
        dir.mkdirs();
        File file = new File(dir, "empty.nbt");
        try (FileOutputStream out = new FileOutputStream(file)) {
            NbtIo.writeCompressed(tag, out);
        }
        System.out.println("Generated " + file.getAbsolutePath());
    }
}
