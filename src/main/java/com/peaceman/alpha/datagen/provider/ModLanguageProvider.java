package com.peaceman.alpha.datagen.provider;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.registry.ModBlocks;
import com.peaceman.alpha.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    private final String locale;

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, Alpha.MODID, locale);
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
        addBlock(ModBlocks.SPACESHIP_SHIELD, "Spaceship Shield Generator");
        addBlock(ModBlocks.PULSE_LASER, "Pulse Laser Turret");
        addBlock(ModBlocks.HEAVY_BEAM, "Heavy Beam Laser");
        addBlock(ModBlocks.MINING_LASER, "Mining Laser");

        // Items
        addItem(ModItems.BACKFLIP_TOOL, "Klasingscher Degen");

        // Creative Tabs & UI
        add("itemGroup.examplemod", "Mod Alpha - Spaceships");
        add("screen.peaceman_alpha.control", "Spaceship Control");
        add("screen.peaceman_alpha.helm", "Spaceship Navigation");
        add("screen.peaceman_alpha.reactor", "Spaceship Reactor");
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
        add("itemGroup.examplemod", "Mod Alpha - Raumschiffe");
        add("screen.peaceman_alpha.control", "Raumschiff Steuerung");
        add("screen.peaceman_alpha.helm", "Raumschiff Navigation");
        add("screen.peaceman_alpha.reactor", "Raumschiff-Reaktor");
    }
}
