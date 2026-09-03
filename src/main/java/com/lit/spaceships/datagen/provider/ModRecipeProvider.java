package com.lit.spaceships.datagen.provider;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.registry.ModBlocks;
import com.lit.spaceships.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.concurrent.CompletableFuture;

/**
 * Automatisierter RecipeProvider für NeoForge 1.21.
 * Erzeugt JSON-Deklarationen und Recipe-Advancements für alle 4 Progression-Tiers.
 */
public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // =====================================================================
        // TIER 1: TERRESTRIAL NAVIGATION & STRUCTURAL FRAMEWORK (OVERWORLD)
        // =====================================================================

        // 1. Hull Plating / Chassis (Yield: 16)
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.EXAMPLE_BLOCK.get(), 16)
                .pattern("CIC")
                .pattern("ISI")
                .pattern("CIC")
                .define('C', Items.COPPER_INGOT)
                .define('I', Items.IRON_INGOT)
                .define('S', Items.SMOOTH_STONE)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(output, ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "example_block_crafting"));

        // 2. Spaceship Helm Console
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SPACESHIP_HELM.get())
                .pattern("IGI")
                .pattern("RCR")
                .pattern("SSS")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS_PANE)
                .define('R', Items.REDSTONE)
                .define('C', Items.COMPASS)
                .define('S', Items.SMOOTH_STONE)
                .unlockedBy("has_compass", has(Items.COMPASS))
                .save(output, ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "spaceship_helm_crafting"));

        // 3. Backflip Tool (Kinetic Entity Launcher)
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.BACKFLIP_TOOL.get())
                .pattern(" SB")
                .pattern(" PS")
                .pattern("R  ")
                .define('B', Items.IRON_SWORD)
                .define('S', Items.SLIME_BALL)
                .define('P', Items.PISTON)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_slime_ball", has(Items.SLIME_BALL))
                .save(output, ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "backflip_tool_crafting"));

        // =====================================================================
        // TIER 2: SUB-ORBITAL UTILITY & LOCAL DEFENSE (NETHER ENTRY)
        // =====================================================================

        // 4. Spaceship Reactor (1,000,000 FE Storage)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SPACESHIP_REACTOR.get())
                .pattern("IQI")
                .pattern("BRB")
                .pattern("DID")
                .define('I', Items.IRON_BLOCK)
                .define('Q', Items.QUARTZ_BLOCK)
                .define('B', Items.BLAZE_ROD)
                .define('R', Items.REDSTONE_BLOCK)
                .define('D', Items.DIAMOND)
                .unlockedBy("has_blaze_rod", has(Items.BLAZE_ROD))
                .save(output, ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "spaceship_reactor_crafting"));

        // 5. Shield Generator (3D Voronoi Barrier Projection)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SPACESHIP_SHIELD.get())
                .pattern("OAO")
                .pattern("AEA")
                .pattern("ODO")
                .define('O', Items.OBSIDIAN)
                .define('A', Items.AMETHYST_SHARD)
                .define('E', Items.ENDER_EYE)
                .define('D', Items.DIAMOND_BLOCK)
                .unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD))
                .unlockedBy("has_ender_eye", has(Items.ENDER_EYE))
                .save(output, ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "spaceship_shield_crafting"));

        // 6. Mining Laser Turret (25 FE/t Industrial Drill)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MINING_LASER.get())
                .pattern("CQC")
                .pattern("QDQ")
                .pattern("IBR")
                .define('C', Items.COPPER_BLOCK)
                .define('Q', Items.QUARTZ)
                .define('D', Items.DIAMOND)
                .define('I', Items.IRON_BLOCK)
                .define('B', Items.DISPENSER)
                .define('R', Items.REDSTONE_BLOCK)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(output, ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "mining_laser_crafting"));

        // =====================================================================
        // TIER 3: NAVAL-GRADE OFFENSIVE WEAPONRY (SMITHING UPGRADES)
        // =====================================================================

        // 7. Heavy Laser Beam (50 FE/t Sustained Thermal Beam)
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModBlocks.MINING_LASER.get()),
                Ingredient.of(Items.NETHERITE_INGOT),
                RecipeCategory.COMBAT,
                ModBlocks.HEAVY_BEAM.get().asItem()
        )
        .unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
        .save(output, ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "heavy_beam_smithing"));

        // 8. Pulse Laser Cannon (250 FE/shot Kinetic Burst Cannon)
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModBlocks.MINING_LASER.get()),
                Ingredient.of(Items.ECHO_SHARD),
                RecipeCategory.COMBAT,
                ModBlocks.PULSE_LASER.get().asItem()
        )
        .unlocks("has_echo_shard", has(Items.ECHO_SHARD))
        .save(output, ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "pulse_laser_smithing"));

        // =====================================================================
        // TIER 4: CAPITAL ENTITY KERNEL & DIMENSIONAL WARP DRIVE (ENDGAME)
        // =====================================================================

        // 9. Spaceship Controller (Kernel with Diamond Blocks, Lodestones, Nether Star, End Crystal)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SPACESHIP_CONTROL.get())
                .pattern("NEN")
                .pattern("YSY")
                .pattern("LNL")
                .define('N', Items.DIAMOND_BLOCK)
                .define('E', Items.END_CRYSTAL)
                .define('Y', Items.ENDER_EYE)
                .define('S', Items.NETHER_STAR)
                .define('L', Items.LODESTONE)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .unlockedBy("has_lodestone", has(Items.LODESTONE))
                .save(output, ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "spaceship_control_crafting"));
    }
}
