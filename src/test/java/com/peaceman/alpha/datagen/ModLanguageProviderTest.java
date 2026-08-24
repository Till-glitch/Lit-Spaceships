package com.peaceman.alpha.datagen;

import com.peaceman.alpha.datagen.provider.ModLanguageProvider;
import net.minecraft.SharedConstants;
import net.minecraft.data.PackOutput;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ModLanguageProviderTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("ModLanguageProvider instantiates correctly for en_us and de_de")
    void testLanguageProviderInstantiation() {
        PackOutput packOutput = new PackOutput(Path.of("test_output"));
        ModLanguageProvider enProvider = new ModLanguageProvider(packOutput, "en_us");
        ModLanguageProvider deProvider = new ModLanguageProvider(packOutput, "de_de");

        assertNotNull(enProvider);
        assertNotNull(deProvider);

        assertEquals("Languages: en_us for mod: peaceman_alpha", enProvider.getName());
        assertEquals("Languages: de_de for mod: peaceman_alpha", deProvider.getName());
    }
}
