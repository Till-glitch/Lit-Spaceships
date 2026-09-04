package com.lit.spaceships.tests;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.block.entity.WarpEngineBlockEntity;
import com.lit.spaceships.registry.ModBlocks;
import com.lit.spaceships.ship.domain.ShipState;
import com.lit.spaceships.ship.service.ServerShipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder(LitSpaceships.MODID)
public class WarpEngineGameTests {

    @GameTest(template = "empty")
    public static void testWarpEnginePlacementAndEnergyStorage(GameTestHelper helper) {
        BlockPos relPos = new BlockPos(1, 2, 1);
        helper.setBlock(relPos, ModBlocks.WARP_ENGINE.get());
        BlockPos absPos = helper.absolutePos(relPos);

        helper.runAfterDelay(1, () -> {
            if (!(helper.getLevel().getBlockEntity(absPos) instanceof WarpEngineBlockEntity engine)) {
                helper.fail("Warp Engine BlockEntity wurde nicht initialisiert!");
                return;
            }

            // Teste interne Energiespeicherung (100.000 FE Kapazität)
            int received = engine.getEnergyStorage().receiveEnergy(50_000, false);
            if (received != 50_000 || engine.getEnergyStorage().getEnergyStored() != 50_000) {
                helper.fail("Energiespeicherung hat 50.000 FE nicht korrekt akzeptiert!");
                return;
            }

            // Auffüllen über Limit -> muss bei 100.000 FE cappen
            engine.getEnergyStorage().receiveEnergy(80_000, false);
            if (engine.getEnergyStorage().getEnergyStored() != WarpEngineBlockEntity.ENERGY_CAPACITY) {
                helper.fail("Energiespeicher hat Kapazitätslimit von 100.000 FE überschritten oder nicht erreicht: "
                        + engine.getEnergyStorage().getEnergyStored());
                return;
            }

            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void testWarpEngineShipLinkage(GameTestHelper helper) {
        BlockPos ctrlRel = new BlockPos(1, 2, 1);
        BlockPos engineRel = new BlockPos(1, 2, 2);

        helper.setBlock(ctrlRel, ModBlocks.SPACESHIP_CONTROL.get());
        helper.setBlock(engineRel, ModBlocks.WARP_ENGINE.get());

        BlockPos ctrlAbs = helper.absolutePos(ctrlRel);
        BlockPos engineAbs = helper.absolutePos(engineRel);

        ServerShipManager.createShip(helper.getLevel(), ctrlAbs);

        helper.runAfterDelay(2, () -> {
            if (!(helper.getLevel().getBlockEntity(engineAbs) instanceof WarpEngineBlockEntity engine)) {
                helper.fail("WarpEngine BlockEntity fehlt!");
                return;
            }

            if (engine.getShipId() == null) {
                helper.fail("WarpEngine wurde beim createShip keine ShipId zugewiesen!");
                return;
            }

            ShipState ship = ServerShipManager.getShip(engine.getShipId());
            if (ship == null) {
                helper.fail("ShipState konnte nach Registrierung nicht gefunden werden!");
                return;
            }

            if (!ship.getWarpEngines().contains(engineAbs)) {
                helper.fail("ShipState enthält die Position der WarpEngine nicht im Subsystem-Set!");
                return;
            }

            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void testWarpEngineCountdownAndAbort(GameTestHelper helper) {
        BlockPos relPos = new BlockPos(1, 2, 1);
        helper.setBlock(relPos, ModBlocks.WARP_ENGINE.get());
        BlockPos absPos = helper.absolutePos(relPos);

        helper.runAfterDelay(1, () -> {
            if (!(helper.getLevel().getBlockEntity(absPos) instanceof WarpEngineBlockEntity engine)) {
                helper.fail("WarpEngine BlockEntity fehlt!");
                return;
            }

            // 1. Versuch ohne Energie -> muss fehlschlagen
            boolean startedWithoutEnergy = engine.startCountdown();
            if (startedWithoutEnergy || engine.isCountingDown()) {
                helper.fail("Warp-Countdown durfte ohne 100.000 FE Energie nicht starten!");
                return;
            }

            // 2. Voll aufladen
            engine.getEnergyStorage().receiveEnergy(WarpEngineBlockEntity.ENERGY_CAPACITY, false);
            boolean startedWithEnergy = engine.startCountdown();
            if (!startedWithEnergy || !engine.isCountingDown() || engine.getCountdownTicks() != WarpEngineBlockEntity.COUNTDOWN_MAX_TICKS) {
                helper.fail("Warp-Countdown startete trotz 100.000 FE nicht mit 200 Ticks!");
                return;
            }

            // 3. Manuellen Abbruch auslösen
            engine.abortCountdown("Test-Abbruch");
            if (engine.isCountingDown() || engine.getCountdownTicks() != 0) {
                helper.fail("Warp-Countdown wurde nach abortCountdown nicht zurückgesetzt!");
                return;
            }

            helper.succeed();
        });
    }
}
