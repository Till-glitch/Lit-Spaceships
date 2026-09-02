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
        add(ModI18n.Screen.REACTOR_TITLE, "Raumschiff Reaktor-Terminal");
        add(ModI18n.Screen.REACTOR_ENERGY, "Energie: %1$s FE");
        add(ModI18n.Screen.REACTOR_STORAGE_LOCAL, "Kern-Speicher: %1$s / %2$s FE (%3$s%%)");
        add(ModI18n.Screen.REACTOR_STORAGE_GRID, "Schiffsnetz: %1$s / %2$s FE (%3$d Reaktoren)");
        add(ModI18n.Screen.REACTOR_GENERATION, "Generierung: +%1$s FE/t");
        add(ModI18n.Screen.REACTOR_CONSUMPTION, "Gesamtlast: -%1$s FE/t");
        add(ModI18n.Screen.REACTOR_NET_FLOW, "Netto-Fluss: %1$s FE/t");
        add(ModI18n.Screen.REACTOR_DRAIN_BREAKDOWN, "Antrieb: %1$s | Schilde: %2$s | Waffen: %3$s FE/t");
        add(ModI18n.Screen.REACTOR_PRIORITY_BTN, "Priorität: %1$s");
        add(ModI18n.Screen.REACTOR_PRIORITY_BALANCED, "AUSGEGLICHEN (1:1:1)");
        add(ModI18n.Screen.REACTOR_PRIORITY_SHIELDS, "SCHILDE ZUERST (70% Def)");
        add(ModI18n.Screen.REACTOR_PRIORITY_WEAPONS, "WAFFEN ZUERST (70% Atk)");
        add(ModI18n.Screen.REACTOR_PRIORITY_ENGINES, "ANTRIEB ZUERST (70% Spd)");
        add(ModI18n.Screen.REACTOR_ALLOCATION_RATIO, "Allokation: Antrieb: %1$d%% | Schilde: %2$d%% | Waffen: %3$d%%");
        add(ModI18n.Screen.REACTOR_FOCUS_BALANCED, "Fokus: Gleichmäßige Lastverteilung auf alle Systeme");
        add(ModI18n.Screen.REACTOR_FOCUS_SHIELDS, "Fokus: Maximale Schildladung & Regenerationsrate");
        add(ModI18n.Screen.REACTOR_FOCUS_WEAPONS, "Fokus: Waffenfeuer & Laser-Dauerstrahl-Kapazität");
        add(ModI18n.Screen.REACTOR_FOCUS_ENGINES, "Fokus: Schneller Schub & Hyperraum-Manöver");
        add(ModI18n.Screen.REACTOR_STATUS_OPTIMAL, "ONLINE (OPTIMAL)");
        add(ModI18n.Screen.REACTOR_STATUS_HIGH_LOAD, "HOHE LAST");
        add(ModI18n.Screen.REACTOR_STATUS_CRITICAL, "KRITISCHE ENTLADUNG (0 FE)");
        add(ModI18n.Screen.REACTOR_STATUS_STANDBY, "STANDBY (NETZ VOLL)");
        add(ModI18n.Screen.REACTOR_STATUS_UNLINKED, "NICHT VERBUNDEN");
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
        add(ModI18n.Screen.HELM_WARP_COOLDOWN, "Warp-Cooldown aktiv (%1$ss)");
        add(ModI18n.Screen.HELM_SCREEN_TITLE, "Navigation & Sprung-Computer");

        add(ModI18n.Screen.SHIELD_TITLE, "Schildgenerator Interface");
        add(ModI18n.Screen.SHIELD_TOGGLE, "Sektor-Schild An/Aus");
        add(ModI18n.Screen.SHIELD_STATUS, "Status: %1$s");
        add(ModI18n.Screen.SHIELD_ACTIVE, "Aktiv");
        add(ModI18n.Screen.SHIELD_INACTIVE, "Inaktiv");
        add(ModI18n.Screen.SHIELD_ENERGY, "Puffer: %1$s / %2$s FE");
        add(ModI18n.Screen.SHIELD_POWER_FLOW, "Reaktor-Einspeisung: +%1$s FE/t");
        add(ModI18n.Screen.SHIELD_SECTOR_ID, "Sektor #%1$d / %2$d");
        add(ModI18n.Screen.SHIELD_STATUS_OPTIMAL, "OPTIMAL (100% GELADEN)");
        add(ModI18n.Screen.SHIELD_STATUS_CHARGING, "AKTIV (LÄDT)");
        add(ModI18n.Screen.SHIELD_STATUS_COLLAPSED, "KOLLABIERT (0 FE - DURCHSCHLAG)");
        add(ModI18n.Screen.SHIELD_STATUS_RECHARGE_CD, "REGENERATION IN %1$ss");
        add(ModI18n.Screen.SHIELD_STATUS_OFFLINE, "DEAKTIVIERT");
        add(ModI18n.Screen.SHIELD_STATUS_UNLINKED, "NICHT VERBUNDEN");
        add(ModI18n.Screen.SHIELD_COVERAGE_VOXELS, "Geschützte Hüllenblöcke: %1$d / %2$d (%3$s%%)");
        add(ModI18n.Screen.SHIELD_COVERAGE_BOUNDS, "Sektor-Ausdehnung: %1$dm x %2$dm x %3$dm");
        add(ModI18n.Screen.SHIELD_COVERAGE_SPAN, "Lokale Spanne: [%1$d, %2$d, %3$d] bis [%4$d, %5$d, %6$d]");
        add(ModI18n.Screen.SHIELD_DEFICIT, "Ladebedarf: %1$s FE");
        add(ModI18n.Screen.SHIELD_FULLY_CHARGED, "Voll aufgeladen");

        add(ModI18n.Screen.CONTROL_TITLE, "Raumschiff Kontroll-Terminal");
        add(ModI18n.Screen.CONTROL_STATUS_BOUND, "VERBUNDEN (ONLINE)");
        add(ModI18n.Screen.CONTROL_STATUS_UNBOUND, "NICHT VERBUNDEN (STANDALONE)");
        add(ModI18n.Screen.CONTROL_STRUCTURAL_HEADER, "STRUKTUR-DIAGNOSE");
        add(ModI18n.Screen.CONTROL_STRUCTURAL_BLOCKS, "Verbundene Hülle: %1$s Blöcke | Masse: %2$s t");
        add(ModI18n.Screen.CONTROL_STRUCTURAL_BOUNDS, "Ausdehnung: ΔX: %1$dm × ΔY: %2$dm × ΔZ: %3$dm");
        add(ModI18n.Screen.CONTROL_STRUCTURAL_ANCHOR, "Anker-Ursprung: [%1$d, %2$d, %3$d]");
        add(ModI18n.Screen.CONTROL_SUBSYSTEM_HEADER, "SUBSYSTEM-REGISTER");
        add(ModI18n.Screen.CONTROL_SUBSYSTEM_CORES, "⚡ Reaktoren: %1$d Kerne | 🛡 Schilde: %2$d Generatoren");
        add(ModI18n.Screen.CONTROL_SUBSYSTEM_WEAPONS, "⚔ Geschütztürme: %1$d (Schwer: %2$d | Puls: %3$d | Bergbau: %4$d)");
        add(ModI18n.Screen.CONTROL_SUBSYSTEM_NAV, "🧭 Navigation: %1$d Steuerkonsole");
        add(ModI18n.Screen.CONTROL_BTN_CREATE, "Schiff binden");
        add(ModI18n.Screen.CONTROL_BTN_UPDATE, "Grenzen updaten");
        add(ModI18n.Screen.CONTROL_BTN_DISASSEMBLE, "Schiff auflösen");
        add(ModI18n.Screen.CONTROL_BTN_HIGHLIGHT, "Hülle hervorheben");
        add(ModI18n.Screen.CONTROL_HIGHLIGHT_ACTIVE, "Markierung: AKTIV");
        add(ModI18n.Screen.CONTROL_HIGHLIGHT_INACTIVE, "Markierung: AUS");
        add(ModI18n.Screen.CONTROL_BTN_SHIELD, "Schild An/Aus");
        add(ModI18n.Screen.CONTROL_SHIELD_COOLDOWN, "Schild (%1$ss)");
        add(ModI18n.Screen.CONTROL_SHIELD_ACTIVE, "Schild: Aktiv");
        add(ModI18n.Screen.CONTROL_SHIELD_INACTIVE, "Schild: Inaktiv");
        add(ModI18n.Screen.CONTROL_BTN_ROTATE_CW, "90° Rechts drehen");
        add(ModI18n.Screen.CONTROL_BTN_ROTATE_CCW, "90° Links drehen");

        add(ModI18n.Screen.HUD_TACTICAL_HEADER, "--- Taktik ---");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_CRITICAL, "Schilde: Kritisch");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_ACTIVE, "Schilde: Aktiv");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_OFFLINE, "Schilde: Offline");
        add(ModI18n.Screen.HUD_TACTICAL_REBOOT, "Reboot: %1$ss");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_DISABLED, "Schilde: Deaktiviert");
        add(ModI18n.Screen.HUD_TACTICAL_ENERGY, "Energie: %1$d%%");
        add(ModI18n.Screen.HUD_HELM_HEADER, "--- Helm HUD ---");
        add(ModI18n.Screen.HUD_HELM_WARP_COOLDOWN, "Warp Cooldown: %1$ss");
        add(ModI18n.Screen.HUD_HELM_READY, "Antrieb Bereit");
        add(ModI18n.Screen.HUD_HELM_CONTROLS, "[H] Verlassen | [M] Nav | [Pfeiltasten] Drehen | [Rechtsklick] Feuern");

        // 5. Messages (Chat / Action-Bar)
        add(ModI18n.Message.TURRET_AIM_LOCKED, "[Geschützturm] Ausrichtung eingeloggt (Yaw: %1$s°, Pitch: %2$s°)");
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
        add(ModI18n.Message.ROTATION_BLOCKED_COLLISION, "[Rotation blockiert] Hindernis oder Terrain im Drehbereich!");
        add(ModI18n.Message.MOVEMENT_BLOCKED_IMMUNE, "[Bewegung blockiert] Schiff enthält einen unverschiebbaren Block!");
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
        add(ModI18n.Keybind.ROTATE_LEFT, "Schiff links drehen (CCW)");
        add(ModI18n.Keybind.ROTATE_RIGHT, "Schiff rechts drehen (CW)");

        // 7. Tooltips & Environments
        add(ModI18n.Tooltip.REACTOR_CAPACITY, "Max. Kapazität: %1$s FE");
        add(ModI18n.Tooltip.WEAPON_TIER, "Waffenstufe: %1$s");
        add(ModI18n.Tooltip.ENERGY_COST, "Energiekosten: %1$s FE/Schuss");
        add(ModI18n.Tooltip.SHIELD_STATUS, "Schild-Matrix: %1$s");
        add(ModI18n.Structure.SHIPWRECK, "Verlassenes Raumschiff-Wrack");
        add(ModI18n.Biome.DEEP_SPACE, "Tiefraum-Vakuum");
    }
}
