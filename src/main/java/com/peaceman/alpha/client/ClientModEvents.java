package com.peaceman.alpha.client;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.client.screen.SpaceshipReactorScreen;
import com.peaceman.alpha.client.screen.SpaceshipShieldScreen;
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
        event.register(ModMenuTypes.HELM_MENU.get(), com.peaceman.alpha.client.screen.SpaceshipHelmConfigScreen::new);
        event.register(ModMenuTypes.SHIELD_MENU.get(), SpaceshipShieldScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.peaceman.alpha.registry.ModBlockEntities.PULSE_LASER_BE.get(), com.peaceman.alpha.client.render.TurretBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(com.peaceman.alpha.registry.ModBlockEntities.HEAVY_BEAM_BE.get(), com.peaceman.alpha.client.render.TurretBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(com.peaceman.alpha.registry.ModBlockEntities.MINING_LASER_BE.get(), com.peaceman.alpha.client.render.TurretBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(com.peaceman.alpha.registry.ModBlockEntities.SPACESHIP_REACTOR_BE.get(), com.peaceman.alpha.client.render.ReactorBlockEntityRenderer::new);
        event.registerEntityRenderer(com.peaceman.alpha.registry.ModEntities.TURRET_SEAT.get(), com.peaceman.alpha.client.render.TurretSeatRenderer::new);
    }
    @SubscribeEvent
    public static void registerModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "block/laser_turret_heavy")));
        event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "block/laser_turret_pulse")));
        event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "block/laser_turret_mining")));
    }

    public static final net.minecraft.client.KeyMapping KEY_EXIT_HELM = new net.minecraft.client.KeyMapping(
            com.peaceman.alpha.registry.ModI18n.Keybind.EXIT_HELM,
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_H,
            com.peaceman.alpha.registry.ModI18n.Keybind.CATEGORY
    );

    public static final net.minecraft.client.KeyMapping KEY_OPEN_HELM_CONFIG = new net.minecraft.client.KeyMapping(
            com.peaceman.alpha.registry.ModI18n.Keybind.OPEN_HELM_CONFIG,
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_M, // M for Map/Menu
            com.peaceman.alpha.registry.ModI18n.Keybind.CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(KEY_EXIT_HELM);
        event.register(KEY_OPEN_HELM_CONFIG);
    }

    @SubscribeEvent
    public static void registerGuiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "helm_hud"),
                new com.peaceman.alpha.client.screen.hud.SpaceshipHelmHudOverlay()
        );
        event.registerAboveAll(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "tactical_hud"),
                new com.peaceman.alpha.client.screen.hud.TacticalConsoleHudLayer()
        );
    }
}
