package com.lit.spaceships.datagen;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.datagen.provider.ModBlockStateProvider;
import com.lit.spaceships.datagen.provider.ModItemModelProvider;
import com.lit.spaceships.datagen.provider.ModLootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LitSpaceships.MODID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Client / View-bezogene Provider
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new com.lit.spaceships.datagen.provider.ModEnglishLanguageProvider(packOutput));
        generator.addProvider(event.includeClient(), new com.lit.spaceships.datagen.provider.ModGermanLanguageProvider(packOutput));

        // Server / Domain-bezogene Provider
        generator.addProvider(event.includeServer(), ModLootTableProvider.create(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new com.lit.spaceships.datagen.provider.ModBlockTagsProvider(packOutput, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(), new com.lit.spaceships.datagen.provider.ModRecipeProvider(packOutput, lookupProvider));
    }
}
