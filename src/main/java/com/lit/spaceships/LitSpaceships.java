package com.lit.spaceships;

import com.mojang.logging.LogUtils;
import com.lit.spaceships.network.ModPayloads;
import com.lit.spaceships.registry.ModAttachments;
import com.lit.spaceships.registry.ModBlockEntities;
import com.lit.spaceships.registry.ModBlocks;
import com.lit.spaceships.registry.ModCreativeTabs;
import com.lit.spaceships.registry.ModItems;
import com.lit.spaceships.registry.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(LitSpaceships.MODID)
public class LitSpaceships {
    public static final String MODID = "lit_spaceships";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LitSpaceships(IEventBus modEventBus, ModContainer modContainer) {
        ModPayloads.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);

        // 2. Ruft unsere aufgeräumten Register-Klassen auf
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModAttachments.register(modEventBus);
        com.lit.spaceships.registry.ModEntities.register(modEventBus);
        com.lit.spaceships.registry.ModFeatures.register(modEventBus);
        modEventBus.addListener(this::registerGameTests);
    }

    private void registerGameTests(net.neoforged.neoforge.event.RegisterGameTestsEvent event) {
        event.register(com.lit.spaceships.tests.SpaceshipGameTests.class);
        event.register(com.lit.spaceships.tests.ShipScannerGameTests.class);
        event.register(com.lit.spaceships.tests.ShipMovementGameTests.class);
        event.register(com.lit.spaceships.tests.ShipAttachmentGameTests.class);
        event.register(com.lit.spaceships.tests.ShipCollisionGameTests.class);
    }

    private void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.SPACESHIP_REACTOR_BE.get(),
                (be, side) -> be.getEnergyStorage()
        );
    }
}