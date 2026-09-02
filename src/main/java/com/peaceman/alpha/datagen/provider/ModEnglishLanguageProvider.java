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
        add(ModI18n.Screen.REACTOR_TITLE, "Spaceship Reactor Terminal");
        add(ModI18n.Screen.REACTOR_ENERGY, "Energy: %1$s FE");
        add(ModI18n.Screen.REACTOR_STORAGE_LOCAL, "Core Storage: %1$s / %2$s FE (%3$s%%)");
        add(ModI18n.Screen.REACTOR_STORAGE_GRID, "Ship Grid: %1$s / %2$s FE (%3$d Cores)");
        add(ModI18n.Screen.REACTOR_GENERATION, "Generation: +%1$s FE/t");
        add(ModI18n.Screen.REACTOR_CONSUMPTION, "Total Drain: -%1$s FE/t");
        add(ModI18n.Screen.REACTOR_NET_FLOW, "Net Flow: %1$s FE/t");
        add(ModI18n.Screen.REACTOR_DRAIN_BREAKDOWN, "Engines: %1$s | Shields: %2$s | Weapons: %3$s FE/t");
        add(ModI18n.Screen.REACTOR_PRIORITY_BTN, "Priority: %1$s");
        add(ModI18n.Screen.REACTOR_PRIORITY_BALANCED, "BALANCED (1:1:1)");
        add(ModI18n.Screen.REACTOR_PRIORITY_SHIELDS, "SHIELDS FIRST (70% Def)");
        add(ModI18n.Screen.REACTOR_PRIORITY_WEAPONS, "WEAPONS FIRST (70% Atk)");
        add(ModI18n.Screen.REACTOR_PRIORITY_ENGINES, "ENGINES FIRST (70% Spd)");
        add(ModI18n.Screen.REACTOR_ALLOCATION_RATIO, "Allocation: Engines: %1$d%% | Shields: %2$d%% | Weapons: %3$d%%");
        add(ModI18n.Screen.REACTOR_FOCUS_BALANCED, "Focus: Balanced power distribution across all systems");
        add(ModI18n.Screen.REACTOR_FOCUS_SHIELDS, "Focus: Maximum shield charging & regeneration rate");
        add(ModI18n.Screen.REACTOR_FOCUS_WEAPONS, "Focus: Weapon fire output & continuous beam capacity");
        add(ModI18n.Screen.REACTOR_FOCUS_ENGINES, "Focus: Fast thruster response & jump computer speed");
        add(ModI18n.Screen.REACTOR_STATUS_OPTIMAL, "ONLINE (OPTIMAL)");
        add(ModI18n.Screen.REACTOR_STATUS_HIGH_LOAD, "HIGH LOAD WARNING");
        add(ModI18n.Screen.REACTOR_STATUS_CRITICAL, "CRITICAL DEPLETION (0 FE)");
        add(ModI18n.Screen.REACTOR_STATUS_STANDBY, "STANDBY (GRID FULL)");
        add(ModI18n.Screen.REACTOR_STATUS_UNLINKED, "UNLINKED STANDALONE");
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
        add(ModI18n.Screen.HELM_WARP_COOLDOWN, "Warp Cooldown active (%1$ss)");
        add(ModI18n.Screen.HELM_SCREEN_TITLE, "Navigation & Jump Computer");

        add(ModI18n.Screen.SHIELD_TITLE, "Shield Generator Interface");
        add(ModI18n.Screen.SHIELD_TOGGLE, "Toggle Sector Shield");
        add(ModI18n.Screen.SHIELD_STATUS, "Status: %1$s");
        add(ModI18n.Screen.SHIELD_ACTIVE, "Active");
        add(ModI18n.Screen.SHIELD_INACTIVE, "Inactive");
        add(ModI18n.Screen.SHIELD_ENERGY, "Buffer: %1$s / %2$s FE");
        add(ModI18n.Screen.SHIELD_POWER_FLOW, "Reactor Flow: +%1$s FE/t");
        add(ModI18n.Screen.SHIELD_SECTOR_ID, "Sector #%1$d / %2$d");
        add(ModI18n.Screen.SHIELD_STATUS_OPTIMAL, "OPTIMAL (100% CHARGED)");
        add(ModI18n.Screen.SHIELD_STATUS_CHARGING, "ACTIVE (CHARGING)");
        add(ModI18n.Screen.SHIELD_STATUS_COLLAPSED, "COLLAPSED (0 FE - BREACH)");
        add(ModI18n.Screen.SHIELD_STATUS_RECHARGE_CD, "REGENERATING IN %1$ss");
        add(ModI18n.Screen.SHIELD_STATUS_OFFLINE, "OFFLINE (DISABLED)");
        add(ModI18n.Screen.SHIELD_STATUS_UNLINKED, "UNLINKED");
        add(ModI18n.Screen.SHIELD_COVERAGE_VOXELS, "Assigned Hull Blocks: %1$d / %2$d (%3$s%%)");
        add(ModI18n.Screen.SHIELD_COVERAGE_BOUNDS, "Sector Extent: %1$dm x %2$dm x %3$dm");
        add(ModI18n.Screen.SHIELD_COVERAGE_SPAN, "Local Span: [%1$d, %2$d, %3$d] to [%4$d, %5$d, %6$d]");
        add(ModI18n.Screen.SHIELD_DEFICIT, "Charge Demand: %1$s FE");
        add(ModI18n.Screen.SHIELD_FULLY_CHARGED, "Fully Charged");

        add(ModI18n.Screen.CONTROL_TITLE, "Spaceship Controller Terminal");
        add(ModI18n.Screen.CONTROL_STATUS_BOUND, "BOUND (ONLINE)");
        add(ModI18n.Screen.CONTROL_STATUS_UNBOUND, "UNBOUND (STANDALONE)");
        add(ModI18n.Screen.CONTROL_STRUCTURAL_HEADER, "STRUCTURAL DIAGNOSTICS");
        add(ModI18n.Screen.CONTROL_STRUCTURAL_BLOCKS, "Connected Hull: %1$s Blocks | Mass: %2$s t");
        add(ModI18n.Screen.CONTROL_STRUCTURAL_BOUNDS, "Dimensions: ΔX: %1$dm × ΔY: %2$dm × ΔZ: %3$dm");
        add(ModI18n.Screen.CONTROL_STRUCTURAL_ANCHOR, "Anchor Origin: [%1$d, %2$d, %3$d]");
        add(ModI18n.Screen.CONTROL_SUBSYSTEM_HEADER, "SUBSYSTEM REGISTRY");
        add(ModI18n.Screen.CONTROL_SUBSYSTEM_CORES, "⚡ Reactors: %1$d Cores | 🛡 Shields: %2$d Generators");
        add(ModI18n.Screen.CONTROL_SUBSYSTEM_WEAPONS, "⚔ Laser Turrets: %1$d (Heavy: %2$d | Pulse: %3$d | Mining: %4$d)");
        add(ModI18n.Screen.CONTROL_SUBSYSTEM_NAV, "🧭 Navigation: %1$d Helm");
        add(ModI18n.Screen.CONTROL_BTN_CREATE, "Create Ship");
        add(ModI18n.Screen.CONTROL_BTN_UPDATE, "Update Bounds");
        add(ModI18n.Screen.CONTROL_BTN_DISASSEMBLE, "Disassemble Ship");
        add(ModI18n.Screen.CONTROL_BTN_HIGHLIGHT, "Highlight Hull");
        add(ModI18n.Screen.CONTROL_HIGHLIGHT_ACTIVE, "Highlight: ACTIVE");
        add(ModI18n.Screen.CONTROL_HIGHLIGHT_INACTIVE, "Highlight: OFF");
        add(ModI18n.Screen.CONTROL_BTN_SHIELD, "Toggle Shield");
        add(ModI18n.Screen.CONTROL_SHIELD_COOLDOWN, "Shield (%1$ss)");
        add(ModI18n.Screen.CONTROL_SHIELD_ACTIVE, "Shield: Active");
        add(ModI18n.Screen.CONTROL_SHIELD_INACTIVE, "Shield: Inactive");

        add(ModI18n.Screen.HUD_TACTICAL_HEADER, "--- Tactical ---");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_CRITICAL, "Shields: Critical");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_ACTIVE, "Shields: Active");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_OFFLINE, "Shields: Offline");
        add(ModI18n.Screen.HUD_TACTICAL_REBOOT, "Reboot: %1$ss");
        add(ModI18n.Screen.HUD_TACTICAL_SHIELD_DISABLED, "Shields: Disabled");
        add(ModI18n.Screen.HUD_TACTICAL_ENERGY, "Energy: %1$d%%");
        add(ModI18n.Screen.HUD_HELM_HEADER, "--- Helm HUD ---");
        add(ModI18n.Screen.HUD_HELM_WARP_COOLDOWN, "Warp Cooldown: %1$ss");
        add(ModI18n.Screen.HUD_HELM_READY, "Engines Ready");
        add(ModI18n.Screen.HUD_HELM_CONTROLS, "[H] Exit | [M] Config | [R-Click] Fire");

        // 5. Messages (Chat / Action-Bar)
        add(ModI18n.Message.TURRET_AIM_LOCKED, "[Turret] Aim locked (Yaw: %1$s°, Pitch: %2$s°)");
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

        // 7. Tooltips & Environments
        add(ModI18n.Tooltip.REACTOR_CAPACITY, "Max Capacity: %1$s FE");
        add(ModI18n.Tooltip.WEAPON_TIER, "Weapon Tier: %1$s");
        add(ModI18n.Tooltip.ENERGY_COST, "Energy Cost: %1$s FE/shot");
        add(ModI18n.Tooltip.SHIELD_STATUS, "Shield Matrix: %1$s");
        add(ModI18n.Structure.SHIPWRECK, "Derelict Spacecraft Wreck");
        add(ModI18n.Biome.DEEP_SPACE, "Deep Space Void");
    }
}
