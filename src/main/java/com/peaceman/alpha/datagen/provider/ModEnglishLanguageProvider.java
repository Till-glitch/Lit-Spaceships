package com.peaceman.alpha.datagen.provider;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.registry.ModBlocks;
import com.peaceman.alpha.registry.ModI18n;
import com.peaceman.alpha.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Generiert die en_us.json Sprachdatei für die Mod Alpha.
 * Diese Klasse fungiert als primärer Datenlieferant für das I18n-System.
 */
public class ModEnglishLanguageProvider extends LanguageProvider {

    public ModEnglishLanguageProvider(PackOutput output) {
        super(output, Alpha.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // 1. Creative Tabs
        add(ModI18n.Tab.MAIN, "Mod Alpha - Spaceships");

        // 2. Blocks
        add(ModBlocks.EXAMPLE_BLOCK.getId().toLanguageKey("block"), "Example Block");
        add(ModBlocks.SPACESHIP_CONTROL.getId().toLanguageKey("block"), "Spaceship Controller");
        add(ModBlocks.SPACESHIP_HELM.getId().toLanguageKey("block"), "Spaceship Helm");
        add(ModBlocks.SPACESHIP_REACTOR.getId().toLanguageKey("block"), "Spaceship Reactor");
        add(ModBlocks.SPACESHIP_SHIELD.getId().toLanguageKey("block"), "Shield Generator");
        add(ModBlocks.PULSE_LASER.getId().toLanguageKey("block"), "Pulse Laser Cannon");
        add(ModBlocks.HEAVY_BEAM.getId().toLanguageKey("block"), "Heavy Laser Beam");
        add(ModBlocks.MINING_LASER.getId().toLanguageKey("block"), "Mining Laser");

        // 3. Items
        add(ModItems.BACKFLIP_TOOL.getId().toLanguageKey("item"), "Klasingscher Degen");

        // 4. UI / Screens
        add(ModI18n.Screen.REACTOR_TITLE, "Reactor Core Diagnostics");
        add(ModI18n.Screen.REACTOR_ENERGY, "Energy: %1$s FE");
        add(ModI18n.Screen.HELM_NAV_TITLE, "Nav-Computer & Waypoints");
        add(ModI18n.Screen.HELM_BTN_SAVE, "Save");
        add(ModI18n.Screen.HELM_BTN_FLYTO, "Fly To");
        add(ModI18n.Screen.HELM_BTN_FORWARD, "Forward");
        add(ModI18n.Screen.HELM_BTN_UP, "Up");
        add(ModI18n.Screen.HELM_BTN_DOWN, "Down");
        add(ModI18n.Screen.HELM_WAYPOINT_INPUT, "Waypoint");
        add(ModI18n.Screen.HELM_DISTANCE_INPUT, "Distance");
        add(ModI18n.Screen.HELM_SHIP_SIZE, "Ship Size: %1$d Blocks");
        add(ModI18n.Screen.HELM_REACTOR_ENERGY, "Reactor Energy: %1$s FE");
        add(ModI18n.Screen.HELM_FLIGHT_COST, "Flight Cost: %1$d FE / Block");
        add(ModI18n.Screen.HELM_MAX_JUMP, "Max Jump Range: %1$s Blocks");
        add(ModI18n.Screen.HELM_WAYPOINT_NAV, "Waypoint Navigation");
        add(ModI18n.Screen.HELM_MANUAL_JUMP, "Manual Distance Jump");
        add(ModI18n.Screen.HELM_COST_READY, "Cost for %1$dm Jump: %2$s FE (Ready)");
        add(ModI18n.Screen.HELM_COST_MISSING, "Cost: %1$s FE (Missing: %2$s FE)");
        add(ModI18n.Screen.HELM_ENTER_DISTANCE, "Enter distance in blocks");
        add(ModI18n.Screen.HELM_WARP_COOLDOWN, "Warp Cooldown active (%1$.1fs)");
        add(ModI18n.Screen.HELM_SCREEN_TITLE, "Navigation & Jump Computer");

        add(ModI18n.Screen.CONTROL_TITLE, "Spaceship Control");
        add(ModI18n.Screen.CONTROL_BTN_CREATE, "Create Ship");
        add(ModI18n.Screen.CONTROL_BTN_UPDATE, "Update Structure");
        add(ModI18n.Screen.CONTROL_BTN_DISASSEMBLE, "Disassemble Ship");
        add(ModI18n.Screen.CONTROL_BTN_HIGHLIGHT, "Toggle Highlight");
        add(ModI18n.Screen.CONTROL_BTN_SHIELD, "Toggle Shield");
        add(ModI18n.Screen.CONTROL_SHIELD_COOLDOWN, "Shield (%1$.1fs)");
        add(ModI18n.Screen.CONTROL_SHIELD_ACTIVE, "Shield: Active");
        add(ModI18n.Screen.CONTROL_SHIELD_INACTIVE, "Shield: Inactive");
        add(ModI18n.Screen.CONTROL_BTN_ROTATE_CW, "Rotate CW (90°)");
        add(ModI18n.Screen.CONTROL_BTN_ROTATE_CCW, "Rotate CCW (90°)");

        add(ModI18n.Screen.HUD_TACTICAL_HEADER, "--- Tactical ---");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_CRITICAL, "Shields: Critical");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_ACTIVE, "Shields: Active");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_OFFLINE, "Shields: Offline");
        add(ModI18n.Screen.HUD_TACTICAL_REBOOT, "Reboot: %1$.1fs");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_DISABLED, "Shields: Disabled");
        add(ModI18n.Screen.HUD_TACTICAL_ENERGY, "Energy: %1$d%%");
        add(ModI18n.Screen.HUD_HELM_HEADER, "--- Helm HUD ---");
        add(ModI18n.Screen.HUD_HELM_WARP_COOLDOWN, "Warp Cooldown: %1$.1fs");
        add(ModI18n.Screen.HUD_HELM_READY, "Engines Ready");
        add(ModI18n.Screen.HUD_HELM_CONTROLS, "[H] Exit | [M] Nav | [Arrows] Rotate | [R-Click] Fire");

        // 5. Messages (Chat / Action-Bar)
        add(ModI18n.Message.TURRET_AIM_LOCKED, "[Turret] Aim locked (Yaw: %1$.1f°, Pitch: %2$.1f°)");
        add(ModI18n.Message.TURRET_AIM_RELEASED, "[Turret] Aim released (Freelook active)");
        add(ModI18n.Message.TURRET_GIMBAL_LIMIT, "[Warning] Gimbal limit reached! Cannot aim further hull-ward.");
        add(ModI18n.Message.TURRET_OCCUPIED, "This turret is already occupied!");
        add(ModI18n.Message.COMBAT_TARGET_DESTROYED, "Target neutralized: %1$s");
        add(ModI18n.Message.SHIELD_COLLAPSE, "[Alert] Shield Generator collapsed! Energy depleted.");
        add(ModI18n.Message.SHIELD_PROTECTED_BLOCK, "This block is protected by a shield!");
        add(ModI18n.Message.REACTOR_DEPLETED, "[System] Critical energy failure. Systems shutting down.");
        add(ModI18n.Message.ENERGY_INSUFFICIENT, "Not enough energy! Required: %1$s FE | Available: %2$s FE");
        add(ModI18n.Message.WARP_PHASE_STATUS, "Warp Sequence Phase %1$d: %2$s");
        add(ModI18n.Message.WAYPOINT_NOT_FOUND, "Error: Waypoint '%1$s' does not exist!");
        add(ModI18n.Message.MOVEMENT_COOLDOWN_ACTIVE, "[Engines] Cooldown active! %1$s seconds remaining.");
        add(ModI18n.Message.COLLISION_WARNING, "[Collision Warning] Collision detected (%1$s)! Movement stopped.");
        add(ModI18n.Message.ROTATION_BLOCKED_COLLISION, "[Rotation Blocked] Obstacle or terrain in rotation path!");
        add(ModI18n.Message.HELM_CONTROL_ENTER, "[Helm] You are piloting the ship. WASD to fly, LSHIFT to descend, SPACE to ascend. H/ESC to exit.");
        add(ModI18n.Message.HELM_CONTROL_LEAVE, "[Helm] You have left helm control.");
        add(ModI18n.Message.DEV_CHEAT_ENERGY, "DEV-CHEAT: %1$s FE loaded!");
        add(ModI18n.Message.SHADER_TEST_HIT, "[Shader-Test] Hit wave triggered at (%1$d, %2$d, %3$d)!");
        add(ModI18n.Message.SHADER_TEST_NO_SHIP, "[Shader-Test] No active ship found at target location.");

        // 6. Keybindings
        add(ModI18n.Keybind.CATEGORY, "Mod Alpha Controls");
        add(ModI18n.Keybind.FIRE_ALL, "Fire All Ship Weapons");
        add(ModI18n.Keybind.NAV_MENU, "Open Navigation Menu");
        add(ModI18n.Keybind.EXIT_HELM, "Exit Helm Console");
        add(ModI18n.Keybind.OPEN_HELM_CONFIG, "Open Helm Navigation Config");
        add(ModI18n.Keybind.MOUNT_TURRET, "Man Turret Seat");
        add(ModI18n.Keybind.ROTATE_LEFT, "Rotate Ship Left (CCW)");
        add(ModI18n.Keybind.ROTATE_RIGHT, "Rotate Ship Right (CW)");

        // 7. Tooltips & Environments
        add(ModI18n.Tooltip.REACTOR_CAPACITY, "Max Capacity: %1$s FE");
        add(ModI18n.Tooltip.WEAPON_TIER, "Weapon Tier: %1$s");
        add(ModI18n.Tooltip.ENERGY_COST, "Energy Cost: %1$s FE/shot");
        add(ModI18n.Tooltip.SHIELD_STATUS, "Shield Matrix: %1$s");
        add(ModI18n.Structure.SHIPWRECK, "Derelict Spacecraft Wreck");
        add(ModI18n.Biome.DEEP_SPACE, "Deep Space Void");
    }
}
