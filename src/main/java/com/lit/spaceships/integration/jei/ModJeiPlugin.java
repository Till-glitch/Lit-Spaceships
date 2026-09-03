package com.peaceman.alpha.integration.jei;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.client.screen.SpaceshipReactorScreen;
import com.peaceman.alpha.client.screen.SpaceshipShieldScreen;
import com.peaceman.alpha.registry.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Dediziertes JEI-Plugin für Mod Alpha.
 * Registriert Recipe Catalysts, Screen Click Areas und GUI-Exclusion-Zonen
 * für vergrößerte Sci-Fi Terminal-Bildschirme.
 */
@JeiPlugin
public class ModJeiPlugin implements IModPlugin {

    public static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // Registriere Reaktor und Spaceship Control als Katalysatoren
        if (ModBlocks.SPACESHIP_REACTOR.isBound()) {
            registration.addRecipeCatalyst(
                    new net.minecraft.world.item.ItemStack(ModBlocks.SPACESHIP_REACTOR.get()),
                    RecipeTypes.CRAFTING,
                    RecipeTypes.SMITHING
            );
        }
        if (ModBlocks.SPACESHIP_CONTROL.isBound()) {
            registration.addRecipeCatalyst(
                    new net.minecraft.world.item.ItemStack(ModBlocks.SPACESHIP_CONTROL.get()),
                    RecipeTypes.CRAFTING
            );
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Klickbereich auf dem Reaktor-Screen (Prioritäts-/Schaltungsbereich)
        registration.addRecipeClickArea(
                SpaceshipReactorScreen.class,
                10, 162, 220, 20,
                RecipeTypes.CRAFTING,
                RecipeTypes.SMITHING
        );

        // GUI-Exclusion-Zonen für vergrößerte Terminal-Screens gegen Overlay-Überlappung
        registration.addGuiContainerHandler(SpaceshipReactorScreen.class, new IGuiContainerHandler<SpaceshipReactorScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(SpaceshipReactorScreen screen) {
                return List.of(new Rect2i(screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(), screen.getYSize()));
            }
        });

        registration.addGuiContainerHandler(SpaceshipShieldScreen.class, new IGuiContainerHandler<SpaceshipShieldScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(SpaceshipShieldScreen screen) {
                return List.of(new Rect2i(screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(), screen.getYSize()));
            }
        });
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Standard-Rezepte (Crafting, Smithing) werden von JEI automatisch aus dem RecipeManager indiziert
    }
}
