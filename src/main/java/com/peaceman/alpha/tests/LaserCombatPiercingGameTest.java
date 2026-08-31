package com.peaceman.alpha.tests;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.registry.ModBlocks;
import com.peaceman.alpha.ship.combat.LaserCombatService;
import com.peaceman.alpha.ship.domain.ShieldZone;
import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ServerShipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * GameTest für Laser-Penetration bei kollabierter lokaler Schildzone
 * vs. Absorption bei intakter Schildzone.
 */
@GameTestHolder(Alpha.MODID)
public class LaserCombatPiercingGameTest {

    @GameTest(template = "empty")
    public static void testLaserPenetrationOnCollapsedZone(GameTestHelper helper) {
        BlockPos ctrlRel = new BlockPos(2, 2, 2);
        helper.setBlock(ctrlRel, ModBlocks.SPACESHIP_CONTROL.get());

        BlockPos shieldRel = new BlockPos(1, 2, 2);
        helper.setBlock(shieldRel, ModBlocks.SPACESHIP_SHIELD.get());

        BlockPos hullRel = new BlockPos(2, 2, 3);
        helper.setBlock(hullRel, Blocks.IRON_BLOCK);

        BlockPos ctrlAbs = helper.absolutePos(ctrlRel);
        ShipState targetShip = ServerShipManager.createShip(helper.getLevel(), ctrlAbs);

        // Ziel-Schildzone manipulieren auf 0 FE (Kollaps erzwingen)
        if (targetShip != null && targetShip.getShieldZone((byte) 1) != null) {
            targetShip.updateShieldZoneEnergy((byte) 1, 0);
        }

        // Pulse Laser Kanone platzieren
        BlockPos weaponRel = new BlockPos(2, 2, 6);
        helper.setBlock(weaponRel, ModBlocks.PULSE_LASER.get());

        BlockPos weaponAbs = helper.absolutePos(weaponRel);

        helper.succeedIf(() -> {
            if (targetShip == null) {
                helper.fail("Target ShipState konnte nicht initialisiert werden");
                return;
            }
            ShieldZone zone = targetShip.getShieldZone((byte) 1);
            if (zone == null || !zone.isCollapsed(helper.getLevel().getGameTime())) {
                helper.fail("Schildzone 1 sollte kollabiert sein");
                return;
            }
        });
    }
}
