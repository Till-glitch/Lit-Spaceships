package com.peaceman.alpha.datagen.provider;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.registry.ModBlocks;
import com.peaceman.alpha.registry.ModI18n;
import com.peaceman.alpha.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Generiert die de_de.json Sprachdatei für die Mod Alpha.
 * Bildet alle Translation Keys symmetrisch zur englischen Primärdatei ab.
 */
public class ModGermanLanguageProvider extends LanguageProvider {

    public ModGermanLanguageProvider(PackOutput output) {
        super(output, Alpha.MODID, "de_de");
    }

    @Override
    protected void addTranslations() {
        // 1. Creative Tabs
        add(ModI18n.Tab.MAIN, "Mod Alpha - Raumschiffe");

        // 2. Blocks
        add(ModBlocks.EXAMPLE_BLOCK.getId().toLanguageKey("block"), "Beispielblock");
        add(ModBlocks.SPACESHIP_CONTROL.getId().toLanguageKey("block"), "Raumschiff-Controller");
        add(ModBlocks.SPACESHIP_HELM.getId().toLanguageKey("block"), "Raumschiff-Steuerkonsole");
        add(ModBlocks.SPACESHIP_REACTOR.getId().toLanguageKey("block"), "Raumschiff-Reaktor");
        add(ModBlocks.SPACESHIP_SHIELD.getId().toLanguageKey("block"), "Raumschiff-Schildgenerator");
        add(ModBlocks.PULSE_LASER.getId().toLanguageKey("block"), "Pulslaser-Geschütz");
        add(ModBlocks.HEAVY_BEAM.getId().toLanguageKey("block"), "Schwerer Strahl-Laser");
        add(ModBlocks.MINING_LASER.getId().toLanguageKey("block"), "Bergbau-Laser");

        // 3. Items
        add(ModItems.BACKFLIP_TOOL.getId().toLanguageKey("item"), "Klasingscher Degen");

        // 4. UI / Screens
        add(ModI18n.Screen.REACTOR_TITLE, "Reaktorkern-Diagnose");
        add(ModI18n.Screen.REACTOR_ENERGY, "Energie: %1$s FE");
        add(ModI18n.Screen.HELM_NAV_TITLE, "Nav-Computer & Wegpunkte");
        add(ModI18n.Screen.HELM_BTN_SAVE, "Speichern");
        add(ModI18n.Screen.HELM_BTN_FLYTO, "Anfliegen");
        add(ModI18n.Screen.HELM_BTN_FORWARD, "Vorwärts");
        add(ModI18n.Screen.HELM_BTN_UP, "Aufwärts");
        add(ModI18n.Screen.HELM_BTN_DOWN, "Abwärts");
        add(ModI18n.Screen.HELM_WAYPOINT_INPUT, "Wegpunkt");
        add(ModI18n.Screen.HELM_DISTANCE_INPUT, "Distanz");
        add(ModI18n.Screen.HELM_SHIP_SIZE, "Schiffsgröße: %1$d Blöcke");
        add(ModI18n.Screen.HELM_REACTOR_ENERGY, "Reaktor-Energie: %1$s FE");
        add(ModI18n.Screen.HELM_FLIGHT_COST, "Flugkosten: %1$d FE / Block");
        add(ModI18n.Screen.HELM_MAX_JUMP, "Max. Sprungreichweite: %1$s Blöcke");
        add(ModI18n.Screen.HELM_WAYPOINT_NAV, "Wegpunkt Navigation");
        add(ModI18n.Screen.HELM_MANUAL_JUMP, "Manueller Distanz-Sprung");
        add(ModI18n.Screen.HELM_COST_READY, "Kosten für %1$dm Sprung: %2$s FE (Bereit)");
        add(ModI18n.Screen.HELM_COST_MISSING, "Kosten: %1$s FE (Fehlen: %2$s FE)");
        add(ModI18n.Screen.HELM_ENTER_DISTANCE, "Distanz in Blöcken eingeben");
        add(ModI18n.Screen.HELM_WARP_COOLDOWN, "Warp-Cooldown aktiv (%1$.1fs)");
        add(ModI18n.Screen.HELM_SCREEN_TITLE, "Navigation & Sprung-Computer");

        add(ModI18n.Screen.SHIELD_TITLE, "Schildgenerator Interface");
        add(ModI18n.Screen.SHIELD_TOGGLE, "Schild An/Aus");
        add(ModI18n.Screen.SHIELD_STATUS, "Status: %1$s");
        add(ModI18n.Screen.SHIELD_ACTIVE, "Aktiv");
        add(ModI18n.Screen.SHIELD_INACTIVE, "Inaktiv");
        add(ModI18n.Screen.SHIELD_ENERGY, "Energie: %1$s FE (Ladebedarf: %2$s FE)");

        add(ModI18n.Screen.CONTROL_TITLE, "Raumschiff Steuerung");
        add(ModI18n.Screen.CONTROL_BTN_CREATE, "Schiff erstellen");
        add(ModI18n.Screen.CONTROL_BTN_UPDATE, "Struktur updaten");
        add(ModI18n.Screen.CONTROL_BTN_DISASSEMBLE, "Schiff auflösen");
        add(ModI18n.Screen.CONTROL_BTN_HIGHLIGHT, "Markierung An/Aus");
        add(ModI18n.Screen.CONTROL_BTN_SHIELD, "Schild An/Aus");
        add(ModI18n.Screen.CONTROL_SHIELD_COOLDOWN, "Schild (%1$.1fs)");
        add(ModI18n.Screen.CONTROL_SHIELD_ACTIVE, "Schild: Aktiv");
        add(ModI18n.Screen.CONTROL_SHIELD_INACTIVE, "Schild: Inaktiv");

        add(ModI18n.Screen.HUD_TACTICAL_HEADER, "--- Taktik ---");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_CRITICAL, "Schilde: Kritisch");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_ACTIVE, "Schilde: Aktiv");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_OFFLINE, "Schilde: Offline");
        add(ModI18n.Screen.HUD_TACTICAL_REBOOT, "Reboot: %1$.1fs");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_DISABLED, "Schilde: Deaktiviert");
        add(ModI18n.Screen.HUD_TACTICAL_ENERGY, "Energie: %1$d%%");
        add(ModI18n.Screen.HUD_HELM_HEADER, "--- Helm HUD ---");
        add(ModI18n.Screen.HUD_HELM_WARP_COOLDOWN, "Warp Cooldown: %1$.1fs");
        add(ModI18n.Screen.HUD_HELM_READY, "Antrieb Bereit");
        add(ModI18n.Screen.HUD_HELM_CONTROLS, "[H] Verlassen | [M] Konfig | [Rechtsklick] Feuern");

        // 5. Messages (Chat / Action-Bar)
        add(ModI18n.Message.TURRET_AIM_LOCKED, "[Geschützturm] Ausrichtung eingeloggt (Yaw: %1$.1f°, Pitch: %2$.1f°)");
        add(ModI18n.Message.TURRET_AIM_RELEASED, "[Geschützturm] Ausrichtung freigegeben (Freelook aktiv)");
        add(ModI18n.Message.TURRET_GIMBAL_LIMIT, "[Warnung] Gimbal-Limit erreicht! Ausrichtung blockiert.");
        add(ModI18n.Message.TURRET_OCCUPIED, "Dieser Geschützturm ist bereits belegt!");
        add(ModI18n.Message.COMBAT_TARGET_DESTROYED, "Ziel neutralisiert: %1$s");
        add(ModI18n.Message.SHIELD_COLLAPSE, "[Alarm] Schildgenerator zusammengebrochen! Energie erschöpft.");
        add(ModI18n.Message.SHIELD_PROTECTED_BLOCK, "Dieser Block wird von einem Schild geschützt!");
        add(ModI18n.Message.REACTOR_DEPLETED, "[System] Kritischer Energieausfall. Systeme schalten ab.");
        add(ModI18n.Message.ENERGY_INSUFFICIENT, "Nicht genug Energie! Benötigt: %1$s FE | Vorhanden: %2$s FE");
        add(ModI18n.Message.WARP_PHASE_STATUS, "Warp-Sequenz Phase %1$d: %2$s");
        add(ModI18n.Message.WAYPOINT_NOT_FOUND, "Fehler: Wegpunkt '%1$s' existiert nicht!");
        add(ModI18n.Message.MOVEMENT_COOLDOWN_ACTIVE, "[Antrieb] Abklingzeit aktiv! Noch %1$s Sekunden.");
        add(ModI18n.Message.COLLISION_WARNING, "[Kollisionswarnung] Kollision erkannt (%1$s)! Bewegung gestoppt.");
        add(ModI18n.Message.HELM_CONTROL_ENTER, "[Helm] Du steuerst nun das Schiff. WASD zum Fliegen, LSHIFT zum Sinken, LEERTASTE zum Steigen. H/ESC zum Verlassen.");
        add(ModI18n.Message.HELM_CONTROL_LEAVE, "[Helm] Du hast die Steuerung verlassen.");
        add(ModI18n.Message.DEV_CHEAT_ENERGY, "DEV-CHEAT: %1$s FE geladen!");
        add(ModI18n.Message.SHADER_TEST_HIT, "[Shader-Test] Treffer-Welle an (%1$d, %2$d, %3$d) ausgelöst!");
        add(ModI18n.Message.SHADER_TEST_NO_SHIP, "[Shader-Test] Kein aktives Schiff am Zielort gefunden.");

        // 6. Keybindings
        add(ModI18n.Keybind.CATEGORY, "Mod Alpha Steuerung");
        add(ModI18n.Keybind.FIRE_ALL, "Alle Schiffswaffen abfeuern");
        add(ModI18n.Keybind.NAV_MENU, "Navigationsmenü öffnen");
        add(ModI18n.Keybind.EXIT_HELM, "Steuerkonsole verlassen");
        add(ModI18n.Keybind.OPEN_HELM_CONFIG, "Helm-Navigationskonfiguration öffnen");
        add(ModI18n.Keybind.MOUNT_TURRET, "Geschützturm bemannen");

        // 7. Tooltips & Environments
        add(ModI18n.Tooltip.REACTOR_CAPACITY, "Max. Kapazität: %1$s FE");
        add(ModI18n.Tooltip.WEAPON_TIER, "Waffenstufe: %1$s");
        add(ModI18n.Tooltip.ENERGY_COST, "Energiekosten: %1$s FE/Schuss");
        add(ModI18n.Tooltip.SHIELD_STATUS, "Schild-Matrix: %1$s");
        add(ModI18n.Structure.SHIPWRECK, "Verlassenes Raumschiff-Wrack");
        add(ModI18n.Biome.DEEP_SPACE, "Tiefraum-Vakuum");
    }
}
