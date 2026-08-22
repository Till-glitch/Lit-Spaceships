package com.peaceman.alpha;

import com.mojang.logging.LogUtils;
import com.peaceman.alpha.network.ModPayloads;
import com.peaceman.alpha.registry.ModAttachments;
import com.peaceman.alpha.registry.ModBlockEntities;
import com.peaceman.alpha.registry.ModBlocks;
import com.peaceman.alpha.registry.ModCreativeTabs;
import com.peaceman.alpha.registry.ModItems;
import com.peaceman.alpha.registry.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Alpha.MODID)
public class Alpha {
    public static final String MODID = "peaceman_alpha";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Alpha(IEventBus modEventBus, ModContainer modContainer) {
        ModPayloads.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);

        // 2. Ruft unsere aufgeräumten Register-Klassen auf
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModAttachments.register(modEventBus);
        com.peaceman.alpha.registry.ModFeatures.register(modEventBus);
        modEventBus.addListener(this::registerGameTests);
    }

    private void registerGameTests(net.neoforged.neoforge.event.RegisterGameTestsEvent event) {
        event.register(com.peaceman.alpha.tests.SpaceshipGameTests.class);
        event.register(com.peaceman.alpha.tests.ShipScannerGameTests.class);
        event.register(com.peaceman.alpha.tests.ShipMovementGameTests.class);
        event.register(com.peaceman.alpha.tests.ShipAttachmentGameTests.class);
    }

    private void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.SPACESHIP_REACTOR_BE.get(),
                (be, side) -> be.getEnergyStorage()
        );
    }
}