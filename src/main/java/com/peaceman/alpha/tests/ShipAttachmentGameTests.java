package com.peaceman.alpha.tests;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.ISpaceshipNode;
import com.peaceman.alpha.registry.ModAttachments;
import com.peaceman.alpha.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.UUID;

/**
 * GameTests zur Validierung der NeoForge 1.21 Data Attachments (SHIP_ID) an BlockEntities.
 */
@GameTestHolder(Alpha.MODID)
public class ShipAttachmentGameTests {

    @GameTest
    public static void testDataAttachmentPersistence(GameTestHelper helper) {
        BlockPos relPos = new BlockPos(1, 2, 1);
        helper.setBlock(relPos, ModBlocks.SPACESHIP_REACTOR.get());

        BlockPos absPos = helper.absolutePos(relPos);
        BlockEntity be = helper.getLevel().getBlockEntity(absPos);

        UUID expectedShipId = UUID.randomUUID();

        helper.succeedIf(() -> {
            if (!(be instanceof ISpaceshipNode node)) {
                helper.fail("BlockEntity implementiert nicht ISpaceshipNode!");
                return;
            }

            // Setzen der ID
            node.setShipId(expectedShipId);

            // Prüfung über ISpaceshipNode Interface
            if (!expectedShipId.equals(node.getShipId())) {
                helper.fail("node.getShipId() liefert nicht die gesetzte UUID!");
                return;
            }

            // Prüfung über NeoForge Data Attachment direkt am BlockEntity
            if (!be.hasData(ModAttachments.SHIP_ID) || !expectedShipId.equals(be.getData(ModAttachments.SHIP_ID))) {
                helper.fail("Data Attachment ModAttachments.SHIP_ID wurde nicht korrekt im BlockEntity gespeichert!");
            }
        });
    }
}
