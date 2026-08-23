package com.peaceman.alpha.datagen;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.server.Bootstrap;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataGeneratorsTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Mock
    private GatherDataEvent event;

    @Mock
    private DataGenerator generator;

    @Mock
    private ExistingFileHelper existingFileHelper;

    @Mock
    private HolderLookup.Provider lookupProvider;

    @Test
    @DisplayName("gatherData runs without exception when client and server data gen are enabled")
    void testGatherDataHappyPath() {
        PackOutput packOutput = new PackOutput(Path.of("test_output"));
        when(event.getGenerator()).thenReturn(generator);
        when(generator.getPackOutput()).thenReturn(packOutput);
        when(event.getExistingFileHelper()).thenReturn(existingFileHelper);
        when(event.getLookupProvider()).thenReturn(CompletableFuture.completedFuture(lookupProvider));

        assertDoesNotThrow(() -> DataGenerators.gatherData(event));

        verify(event).getGenerator();
        verify(generator).getPackOutput();
        verify(event).getExistingFileHelper();
        verify(event).getLookupProvider();
    }

    @Test
    @DisplayName("gatherData handles edge case where lookup provider or existing file helper is called")
    void testGatherDataHandlesLookupProvider() {
        PackOutput packOutput = new PackOutput(Path.of("test_output"));
        when(event.getGenerator()).thenReturn(generator);
        when(generator.getPackOutput()).thenReturn(packOutput);
        when(event.getExistingFileHelper()).thenReturn(existingFileHelper);
        when(event.getLookupProvider()).thenReturn(CompletableFuture.completedFuture(lookupProvider));

        assertDoesNotThrow(() -> DataGenerators.gatherData(event));
    }
}
