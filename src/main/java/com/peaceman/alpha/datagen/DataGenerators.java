package com.peaceman.alpha.datagen;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.datagen.provider.ModBlockStateProvider;
import com.peaceman.alpha.datagen.provider.ModItemModelProvider;
import com.peaceman.alpha.datagen.provider.ModLootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.MOD)
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
        generator.addProvider(event.includeClient(), new com.peaceman.alpha.datagen.provider.ModEnglishLanguageProvider(packOutput));
        generator.addProvider(event.includeClient(), new com.peaceman.alpha.datagen.provider.ModGermanLanguageProvider(packOutput));

        // Server / Domain-bezogene Provider
        generator.addProvider(event.includeServer(), ModLootTableProvider.create(packOutput, lookupProvider));
    }
}
