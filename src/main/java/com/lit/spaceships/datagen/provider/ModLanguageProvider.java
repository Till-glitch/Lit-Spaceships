package com.lit.spaceships.datagen.provider;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.registry.ModBlocks;
import com.lit.spaceships.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    private final String locale;

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, LitSpaceships.MODID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        if ("de_de".equals(this.locale)) {
            addGermanTranslations();
        } else {
            addEnglishTranslations();
        }
    }

    private void addEnglishTranslations() {
        // Blocks
        addBlock(ModBlocks.EXAMPLE_BLOCK, "Example Block");
        addBlock(ModBlocks.SPACESHIP_CONTROL, "Spaceship Controller");
        addBlock(ModBlocks.SPACESHIP_HELM, "Spaceship Helm");
        addBlock(ModBlocks.SPACESHIP_REACTOR, "Spaceship Reactor");
        addBlock(ModBlocks.SPACESHIP_SHIELD, "Shield Generator");
        addBlock(ModBlocks.PULSE_LASER, "Pulse Laser Cannon");
        addBlock(ModBlocks.HEAVY_BEAM, "Heavy Laser Beam");
        addBlock(ModBlocks.MINING_LASER, "Mining Laser");

        // Items
        addItem(ModItems.BACKFLIP_TOOL, "Klasingscher Degen");

        // Creative Tabs & UI
        add(com.lit.spaceships.registry.ModI18n.Tab.MAIN, "Mod Alpha - Spaceships");
        add(com.lit.spaceships.registry.ModI18n.Screen.CONTROL_TITLE, "Spaceship Control");
        add(com.lit.spaceships.registry.ModI18n.Screen.HELM_NAV_TITLE, "Nav-Computer & Waypoints");
        add(com.lit.spaceships.registry.ModI18n.Screen.REACTOR_TITLE, "Reactor Core Diagnostics");
    }

    private void addGermanTranslations() {
        // Blocks
        addBlock(ModBlocks.EXAMPLE_BLOCK, "Beispielblock");
        addBlock(ModBlocks.SPACESHIP_CONTROL, "Raumschiff-Controller");
        addBlock(ModBlocks.SPACESHIP_HELM, "Raumschiff-Steuerkonsole");
        addBlock(ModBlocks.SPACESHIP_REACTOR, "Raumschiff-Reaktor");
        addBlock(ModBlocks.SPACESHIP_SHIELD, "Raumschiff-Schildgenerator");
        addBlock(ModBlocks.PULSE_LASER, "Pulslaser-Geschütz");
        addBlock(ModBlocks.HEAVY_BEAM, "Schwerer Strahl-Laser");
        addBlock(ModBlocks.MINING_LASER, "Bergbau-Laser");

        // Items
        addItem(ModItems.BACKFLIP_TOOL, "Klasingscher Degen");

        // Creative Tabs & UI
        add(com.lit.spaceships.registry.ModI18n.Tab.MAIN, "Mod Alpha - Raumschiffe");
        add(com.lit.spaceships.registry.ModI18n.Screen.CONTROL_TITLE, "Raumschiff Steuerung");
        add(com.lit.spaceships.registry.ModI18n.Screen.HELM_NAV_TITLE, "Nav-Computer & Wegpunkte");
        add(com.lit.spaceships.registry.ModI18n.Screen.REACTOR_TITLE, "Reaktorkern-Diagnose");
    }
}
