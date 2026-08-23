package com.peaceman.alpha.client;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.client.screen.SpaceshipReactorScreen;
import com.peaceman.alpha.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.REACTOR_MENU.get(), SpaceshipReactorScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.peaceman.alpha.registry.ModBlockEntities.PULSE_LASER_BE.get(), com.peaceman.alpha.client.render.TurretBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(com.peaceman.alpha.registry.ModBlockEntities.MINING_LASER_BE.get(), com.peaceman.alpha.client.render.TurretBlockEntityRenderer::new);
        event.registerEntityRenderer(com.peaceman.alpha.registry.ModEntities.TURRET_SEAT.get(), com.peaceman.alpha.client.render.TurretSeatRenderer::new);
    }
}
