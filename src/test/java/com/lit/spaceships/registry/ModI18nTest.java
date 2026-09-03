package com.lit.spaceships.registry;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.datagen.provider.ModEnglishLanguageProvider;
import com.lit.spaceships.datagen.provider.ModGermanLanguageProvider;
import net.minecraft.SharedConstants;
import net.minecraft.data.PackOutput;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ModI18nTest {

    private static final Pattern TAXONOMY_PATTERN = Pattern.compile("^(itemGroup\\.[a-z0-9_.]+|[a-z0-9_.]+)$");
    private static final List<String> VALID_PREFIXES = List.of(
            "itemGroup." + LitSpaceships.MODID,
            "screen." + LitSpaceships.MODID,
            "message." + LitSpaceships.MODID,
            "key.categories." + LitSpaceships.MODID,
            "key." + LitSpaceships.MODID,
            "tooltip." + LitSpaceships.MODID,
            "structure." + LitSpaceships.MODID,
            "biome." + LitSpaceships.MODID
    );

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static List<String> getAllI18nKeys() throws IllegalAccessException {
        List<String> keys = new ArrayList<>();
        Class<?>[] innerClasses = ModI18n.class.getDeclaredClasses();
        for (Class<?> inner : innerClasses) {
            for (Field field : inner.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) && field.getType() == String.class) {
                    field.setAccessible(true);
                    String key = (String) field.get(null);
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    @Test
    @DisplayName("All ModI18n keys conform to strict lowercase taxonomy rules")
    void testTaxonomyFormat() throws IllegalAccessException {
        List<String> keys = getAllI18nKeys();
        assertFalse(keys.isEmpty(), "ModI18n must contain translation keys");

        for (String key : keys) {
            assertTrue(TAXONOMY_PATTERN.matcher(key).matches(),
                    "Key '" + key + "' violates taxonomy syntax (must be lowercase alphanumeric with dots and underscores only)");

            boolean hasValidPrefix = VALID_PREFIXES.stream().anyMatch(key::startsWith);
            assertTrue(hasValidPrefix, "Key '" + key + "' does not start with any allowed category prefix");
        }
    }

    @Test
    @DisplayName("All ModI18n keys are unique with zero duplicates")
    void testNoDuplicateKeys() throws IllegalAccessException {
        List<String> keys = getAllI18nKeys();
        Set<String> uniqueKeys = new HashSet<>();
        List<String> duplicates = new ArrayList<>();

        for (String key : keys) {
            if (!uniqueKeys.add(key)) {
                duplicates.add(key);
            }
        }

        assertTrue(duplicates.isEmpty(), "Duplicate translation keys found in ModI18n: " + duplicates);
    }

    @Test
    @DisplayName("English and German language providers provide complete, non-empty translations for all ModI18n keys")
    void testLanguageProvidersCompleteness() throws Exception {
        PackOutput packOutput = new PackOutput(Path.of("test_output"));

        ModEnglishLanguageProvider enProvider = new ModEnglishLanguageProvider(packOutput);
        ModGermanLanguageProvider deProvider = new ModGermanLanguageProvider(packOutput);

        // Invoke protected addTranslations via reflection
        var addTranslationsMethodEn = ModEnglishLanguageProvider.class.getDeclaredMethod("addTranslations");
        addTranslationsMethodEn.setAccessible(true);
        addTranslationsMethodEn.invoke(enProvider);

        var addTranslationsMethodDe = ModGermanLanguageProvider.class.getDeclaredMethod("addTranslations");
        addTranslationsMethodDe.setAccessible(true);
        addTranslationsMethodDe.invoke(deProvider);

        // Extract internal 'data' Map from LanguageProvider
        Field dataField = net.neoforged.neoforge.common.data.LanguageProvider.class.getDeclaredField("data");
        dataField.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> enData = (Map<String, String>) dataField.get(enProvider);
        @SuppressWarnings("unchecked")
        Map<String, String> deData = (Map<String, String>) dataField.get(deProvider);

        assertNotNull(enData, "en_us translation data map must not be null");
        assertNotNull(deData, "de_de translation data map must not be null");

        List<String> keys = getAllI18nKeys();
        for (String key : keys) {
            assertTrue(enData.containsKey(key), "Missing English (en_us) translation for key: " + key);
            assertFalse(enData.get(key).isBlank(), "English translation must not be blank for key: " + key);

            assertTrue(deData.containsKey(key), "Missing German (de_de) translation for key: " + key);
            assertFalse(deData.get(key).isBlank(), "German translation must not be blank for key: " + key);
        }
    }
}
