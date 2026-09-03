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

    @GameTest(template = "empty")
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

    @GameTest(template = "empty")
    public static void testReactorLitState(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(1, 2, 1);
        helper.setBlock(relativePos, ModBlocks.SPACESHIP_REACTOR.get());
        BlockPos absolutePos = helper.absolutePos(relativePos);
        
        helper.runAfterDelay(1, () -> {
            net.minecraft.world.level.block.state.BlockState state = helper.getBlockState(relativePos);
            if (state.getValue(com.peaceman.alpha.block.SpaceshipReactorBlock.LIT)) {
                helper.fail("Reactor was lit initially despite having no energy!");
            }
            
            if (helper.getLevel().getBlockEntity(absolutePos) instanceof com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity reactor) {
                reactor.getEnergyStorage().receiveEnergy(500, false);
            }
            
            helper.runAfterDelay(1, () -> {
                net.minecraft.world.level.block.state.BlockState newState = helper.getBlockState(relativePos);
                if (!newState.getValue(com.peaceman.alpha.block.SpaceshipReactorBlock.LIT)) {
                    helper.fail("Reactor was not lit after receiving energy!");
                }
                helper.succeed();
            });
        });
    }
}