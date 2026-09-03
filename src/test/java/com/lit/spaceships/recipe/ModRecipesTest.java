package com.peaceman.alpha.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.client.screen.SpaceshipReactorScreen;
import com.peaceman.alpha.client.screen.SpaceshipShieldScreen;
import com.peaceman.alpha.datagen.provider.ModRecipeProvider;
import com.peaceman.alpha.integration.jei.ModJeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ModRecipesTest {

    @org.junit.jupiter.api.BeforeAll
    static void initMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    private static final Path RECIPE_DIR = Path.of("src/generated/resources/data/peaceman_alpha/recipe");
    private static final Path ADVANCEMENT_DIR = Path.of("src/generated/resources/data/peaceman_alpha/advancement/recipes");

    @Test
    @DisplayName("ModRecipeProvider lässt sich instanziieren")
    void testRecipeProviderInstantiation() {
        PackOutput packOutput = new PackOutput(Path.of("build/test_output"));
        ModRecipeProvider provider = new ModRecipeProvider(packOutput, CompletableFuture.completedFuture(null));
        assertNotNull(provider);
        assertEquals("Recipes", provider.getName());
    }

    @Test
    @DisplayName("Alle 9 generierten Recipe-JSONs existieren und besitzen valide Typen und Ergebnisse")
    void testAllGeneratedRecipesExistAndAreValid() throws IOException {
        assertTrue(Files.exists(RECIPE_DIR), "Recipe-Verzeichnis muss existieren: " + RECIPE_DIR);

        Map<String, String> expectedRecipes = Map.of(
                "example_block_crafting.json", "peaceman_alpha:example_block",
                "spaceship_helm_crafting.json", "peaceman_alpha:spaceship_helm",
                "backflip_tool_crafting.json", "peaceman_alpha:backflip_tool",
                "spaceship_reactor_crafting.json", "peaceman_alpha:spaceship_reactor",
                "spaceship_shield_crafting.json", "peaceman_alpha:spaceship_shield",
                "mining_laser_crafting.json", "peaceman_alpha:mining_laser",
                "heavy_beam_smithing.json", "peaceman_alpha:heavy_beam",
                "pulse_laser_smithing.json", "peaceman_alpha:pulse_laser",
                "spaceship_control_crafting.json", "peaceman_alpha:spaceship_control"
        );

        for (Map.Entry<String, String> entry : expectedRecipes.entrySet()) {
            Path file = RECIPE_DIR.resolve(entry.getKey());
            assertTrue(Files.exists(file), "Rezeptdatei fehlt: " + entry.getKey());

            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                assertTrue(json.has("type"), "Rezept muss ein 'type' Feld haben: " + entry.getKey());
                assertTrue(json.has("result"), "Rezept muss ein 'result' Feld haben: " + entry.getKey());

                JsonObject resultObj = json.getAsJsonObject("result");
                assertEquals(entry.getValue(), resultObj.get("id").getAsString(), "Falsches Result-Item in: " + entry.getKey());
            }
        }
    }

    @Test
    @DisplayName("Smithing-Rezepte für Heavy Beam und Pulse Laser verwenden Mining Laser als Basis")
    void testSmithingRecipesUseMiningLaserBase() throws IOException {
        Path heavyBeamFile = RECIPE_DIR.resolve("heavy_beam_smithing.json");
        Path pulseLaserFile = RECIPE_DIR.resolve("pulse_laser_smithing.json");

        assertTrue(Files.exists(heavyBeamFile));
        assertTrue(Files.exists(pulseLaserFile));

        try (Reader reader = Files.newBufferedReader(heavyBeamFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals("minecraft:smithing_transform", json.get("type").getAsString());
            assertEquals("peaceman_alpha:mining_laser", json.getAsJsonObject("base").get("item").getAsString());
            assertEquals("minecraft:netherite_ingot", json.getAsJsonObject("addition").get("item").getAsString());
        }

        try (Reader reader = Files.newBufferedReader(pulseLaserFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals("minecraft:smithing_transform", json.get("type").getAsString());
            assertEquals("peaceman_alpha:mining_laser", json.getAsJsonObject("base").get("item").getAsString());
            assertEquals("minecraft:echo_shard", json.getAsJsonObject("addition").get("item").getAsString());
        }
    }

    @Test
    @DisplayName("Spaceship Controller Rezept verwendet survival-freundliche Diamantblöcke")
    void testSpaceshipControlRecipeUsesDiamondBlocks() throws IOException {
        Path ctrlFile = RECIPE_DIR.resolve("spaceship_control_crafting.json");
        assertTrue(Files.exists(ctrlFile));

        try (Reader reader = Files.newBufferedReader(ctrlFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals("minecraft:crafting_shaped", json.get("type").getAsString());
            JsonObject keyObj = json.getAsJsonObject("key");

            // N muss Diamantblock sein
            assertTrue(keyObj.has("N"));
            assertEquals("minecraft:diamond_block", keyObj.getAsJsonObject("N").get("item").getAsString());

            // Weitere Zutaten prüfen
            assertEquals("minecraft:end_crystal", keyObj.getAsJsonObject("E").get("item").getAsString());
            assertEquals("minecraft:ender_eye", keyObj.getAsJsonObject("Y").get("item").getAsString());
            assertEquals("minecraft:nether_star", keyObj.getAsJsonObject("S").get("item").getAsString());
            assertEquals("minecraft:lodestone", keyObj.getAsJsonObject("L").get("item").getAsString());
        }
    }

    @Test
    @DisplayName("Advancements für alle 9 Rezepte existieren")
    void testRecipeAdvancementsExist() {
        assertTrue(Files.exists(ADVANCEMENT_DIR));

        assertTrue(Files.exists(ADVANCEMENT_DIR.resolve("building_blocks/example_block_crafting.json")));
        assertTrue(Files.exists(ADVANCEMENT_DIR.resolve("combat/backflip_tool_crafting.json")));
        assertTrue(Files.exists(ADVANCEMENT_DIR.resolve("combat/heavy_beam_smithing.json")));
        assertTrue(Files.exists(ADVANCEMENT_DIR.resolve("combat/pulse_laser_smithing.json")));
        assertTrue(Files.exists(ADVANCEMENT_DIR.resolve("misc/mining_laser_crafting.json")));
        assertTrue(Files.exists(ADVANCEMENT_DIR.resolve("misc/spaceship_control_crafting.json")));
        assertTrue(Files.exists(ADVANCEMENT_DIR.resolve("misc/spaceship_helm_crafting.json")));
        assertTrue(Files.exists(ADVANCEMENT_DIR.resolve("misc/spaceship_reactor_crafting.json")));
        assertTrue(Files.exists(ADVANCEMENT_DIR.resolve("misc/spaceship_shield_crafting.json")));
    }

    @Test
    @DisplayName("ModJeiPlugin registriert korrekte Plugin-UID und GUI-Handlers")
    void testJeiPluginRegistration() {
        ModJeiPlugin plugin = new ModJeiPlugin();
        assertEquals(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "jei_plugin"), plugin.getPluginUid());

        IRecipeCatalystRegistration catalystRegistration = mock(IRecipeCatalystRegistration.class);
        assertDoesNotThrow(() -> plugin.registerRecipeCatalysts(catalystRegistration));

        IGuiHandlerRegistration guiHandlerRegistration = mock(IGuiHandlerRegistration.class);
        plugin.registerGuiHandlers(guiHandlerRegistration);

        // Verifiziere Click Area Registrierung für Reactor Screen
        verify(guiHandlerRegistration).addRecipeClickArea(
                eq(SpaceshipReactorScreen.class),
                eq(10), eq(162), eq(220), eq(20),
                eq(RecipeTypes.CRAFTING), eq(RecipeTypes.SMITHING)
        );

        // Verifiziere GUI Exclusion Handler für Reactor & Shield Screens
        verify(guiHandlerRegistration).addGuiContainerHandler(eq(SpaceshipReactorScreen.class), any());
        verify(guiHandlerRegistration).addGuiContainerHandler(eq(SpaceshipShieldScreen.class), any());
    }
}
