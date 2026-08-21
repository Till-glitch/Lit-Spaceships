package com.peaceman.alpha.tests;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.ISpaceshipNode;
import com.peaceman.alpha.registry.ModBlocks;
import com.peaceman.alpha.ship.service.ServerShipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder(Alpha.MODID)
public class SpaceshipGameTests {

    @GameTest()
    public static void testShipCreation(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(1, 2, 1);

        // 1. Block platzieren
        helper.setBlock(relativePos, ModBlocks.SPACESHIP_CONTROL.get());

        // 2. Aktion ausführen
        BlockPos absolutePos = helper.absolutePos(relativePos);
        ServerShipManager.createShip(helper.getLevel(), absolutePos);

        // 3. Überprüfung
        helper.succeedIf(() -> {
            if (helper.getLevel().getBlockEntity(absolutePos) instanceof ISpaceshipNode node) {
                if (node.getShipId() != null) {
                    return;
                }
            }
            helper.fail("Kontrollblock hat nach createShip keine UUID erhalten!");
        });
    }
}