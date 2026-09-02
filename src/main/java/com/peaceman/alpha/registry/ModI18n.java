package com.peaceman.alpha.registry;

import com.peaceman.alpha.Alpha;

/**
 * Single-Source-of-Truth für alle Translation Keys der Mod Alpha.
 * Diese Klasse bündelt alle textuellen Bezeichner zur Vermeidung von
 * Magic Strings in der Geschäfts- und Darstellungskoordination.
 */
public final class ModI18n {

    private ModI18n() {}

    public static final class Tab {
        public static final String MAIN = "itemGroup." + Alpha.MODID + ".main";
    }

    public static final class Screen {
        // Reactor Screen
        public static final String REACTOR_TITLE = "screen." + Alpha.MODID + ".reactor.title";
        public static final String REACTOR_ENERGY = "screen." + Alpha.MODID + ".reactor.energy";
        public static final String REACTOR_STORAGE_LOCAL = "screen." + Alpha.MODID + ".reactor.storage_local";
        public static final String REACTOR_STORAGE_GRID = "screen." + Alpha.MODID + ".reactor.storage_grid";
        public static final String REACTOR_GENERATION = "screen." + Alpha.MODID + ".reactor.generation";
        public static final String REACTOR_CONSUMPTION = "screen." + Alpha.MODID + ".reactor.consumption";
        public static final String REACTOR_NET_FLOW = "screen." + Alpha.MODID + ".reactor.net_flow";
        public static final String REACTOR_DRAIN_BREAKDOWN = "screen." + Alpha.MODID + ".reactor.drain_breakdown";
        public static final String REACTOR_PRIORITY_BTN = "screen." + Alpha.MODID + ".reactor.priority_btn";
        public static final String REACTOR_PRIORITY_BALANCED = "screen." + Alpha.MODID + ".reactor.priority_balanced";
        public static final String REACTOR_PRIORITY_SHIELDS = "screen." + Alpha.MODID + ".reactor.priority_shields";
        public static final String REACTOR_PRIORITY_WEAPONS = "screen." + Alpha.MODID + ".reactor.priority_weapons";
        public static final String REACTOR_PRIORITY_ENGINES = "screen." + Alpha.MODID + ".reactor.priority_engines";
        public static final String REACTOR_ALLOCATION_RATIO = "screen." + Alpha.MODID + ".reactor.allocation_ratio";
        public static final String REACTOR_FOCUS_BALANCED = "screen." + Alpha.MODID + ".reactor.focus_balanced";
        public static final String REACTOR_FOCUS_SHIELDS = "screen." + Alpha.MODID + ".reactor.focus_shields";
        public static final String REACTOR_FOCUS_WEAPONS = "screen." + Alpha.MODID + ".reactor.focus_weapons";
        public static final String REACTOR_FOCUS_ENGINES = "screen." + Alpha.MODID + ".reactor.focus_engines";
        public static final String REACTOR_STATUS_OPTIMAL = "screen." + Alpha.MODID + ".reactor.status_optimal";
        public static final String REACTOR_STATUS_HIGH_LOAD = "screen." + Alpha.MODID + ".reactor.status_high_load";
        public static final String REACTOR_STATUS_CRITICAL = "screen." + Alpha.MODID + ".reactor.status_critical";
        public static final String REACTOR_STATUS_STANDBY = "screen." + Alpha.MODID + ".reactor.status_standby";
        public static final String REACTOR_STATUS_UNLINKED = "screen." + Alpha.MODID + ".reactor.status_unlinked";

        // Helm Config Screen
        public static final String HELM_NAV_TITLE = "screen." + Alpha.MODID + ".helm.navigation";
        public static final String HELM_BTN_SAVE = "screen." + Alpha.MODID + ".helm.btn.save";
        public static final String HELM_BTN_FLYTO = "screen." + Alpha.MODID + ".helm.btn.flyto";
        public static final String HELM_BTN_FORWARD = "screen." + Alpha.MODID + ".helm.btn.forward";
        public static final String HELM_BTN_UP = "screen." + Alpha.MODID + ".helm.btn.up";
        public static final String HELM_BTN_DOWN = "screen." + Alpha.MODID + ".helm.btn.down";
        public static final String HELM_WAYPOINT_INPUT = "screen." + Alpha.MODID + ".helm.input.waypoint";
        public static final String HELM_DISTANCE_INPUT = "screen." + Alpha.MODID + ".helm.input.distance";
        public static final String HELM_SHIP_SIZE = "screen." + Alpha.MODID + ".helm.ship_size";
        public static final String HELM_REACTOR_ENERGY = "screen." + Alpha.MODID + ".helm.reactor_energy";
        public static final String HELM_FLIGHT_COST = "screen." + Alpha.MODID + ".helm.flight_cost";
        public static final String HELM_MAX_JUMP = "screen." + Alpha.MODID + ".helm.max_jump";
        public static final String HELM_WAYPOINT_NAV = "screen." + Alpha.MODID + ".helm.waypoint_nav";
        public static final String HELM_MANUAL_JUMP = "screen." + Alpha.MODID + ".helm.manual_jump";
        public static final String HELM_COST_READY = "screen." + Alpha.MODID + ".helm.cost_ready";
        public static final String HELM_COST_MISSING = "screen." + Alpha.MODID + ".helm.cost_missing";
        public static final String HELM_ENTER_DISTANCE = "screen." + Alpha.MODID + ".helm.enter_distance";
        public static final String HELM_WARP_COOLDOWN = "screen." + Alpha.MODID + ".helm.warp_cooldown";
        public static final String HELM_SCREEN_TITLE = "screen." + Alpha.MODID + ".helm.title";

        // Control Screen
        public static final String CONTROL_TITLE = "screen." + Alpha.MODID + ".control.title";
        public static final String CONTROL_STATUS_BOUND = "screen." + Alpha.MODID + ".control.status_bound";
        public static final String CONTROL_STATUS_UNBOUND = "screen." + Alpha.MODID + ".control.status_unbound";
        public static final String CONTROL_STRUCTURAL_HEADER = "screen." + Alpha.MODID + ".control.structural_header";
        public static final String CONTROL_STRUCTURAL_BLOCKS = "screen." + Alpha.MODID + ".control.structural_blocks";
        public static final String CONTROL_STRUCTURAL_BOUNDS = "screen." + Alpha.MODID + ".control.structural_bounds";
        public static final String CONTROL_STRUCTURAL_ANCHOR = "screen." + Alpha.MODID + ".control.structural_anchor";
        public static final String CONTROL_SUBSYSTEM_HEADER = "screen." + Alpha.MODID + ".control.subsystem_header";
        public static final String CONTROL_SUBSYSTEM_CORES = "screen." + Alpha.MODID + ".control.subsystem_cores";
        public static final String CONTROL_SUBSYSTEM_WEAPONS = "screen." + Alpha.MODID + ".control.subsystem_weapons";
        public static final String CONTROL_SUBSYSTEM_NAV = "screen." + Alpha.MODID + ".control.subsystem_nav";
        public static final String CONTROL_BTN_CREATE = "screen." + Alpha.MODID + ".control.btn.create";
        public static final String CONTROL_BTN_UPDATE = "screen." + Alpha.MODID + ".control.btn.update";
        public static final String CONTROL_BTN_DISASSEMBLE = "screen." + Alpha.MODID + ".control.btn.disassemble";
        public static final String CONTROL_BTN_HIGHLIGHT = "screen." + Alpha.MODID + ".control.btn.highlight";
        public static final String CONTROL_HIGHLIGHT_ACTIVE = "screen." + Alpha.MODID + ".control.highlight_active";
        public static final String CONTROL_HIGHLIGHT_INACTIVE = "screen." + Alpha.MODID + ".control.highlight_inactive";
        public static final String CONTROL_BTN_SHIELD = "screen." + Alpha.MODID + ".control.btn.shield";
        public static final String CONTROL_SHIELD_COOLDOWN = "screen." + Alpha.MODID + ".control.shield_cooldown";
        public static final String CONTROL_SHIELD_ACTIVE = "screen." + Alpha.MODID + ".control.shield_active";
        public static final String CONTROL_SHIELD_INACTIVE = "screen." + Alpha.MODID + ".control.shield_inactive";

        // HUD Layers
        public static final String HUD_TACTICAL_HEADER = "screen." + Alpha.MODID + ".hud.tactical.header";
        public static final String HUD_TACTICAL_SHIELD_CRITICAL = "screen." + Alpha.MODID + ".hud.tactical.shield_critical";
        public static final String HUD_TACTICAL_SHIELD_ACTIVE = "screen." + Alpha.MODID + ".hud.tactical.shield_active";
        public static final String HUD_TACTICAL_SHIELD_OFFLINE = "screen." + Alpha.MODID + ".hud.tactical.shield_offline";
        public static final String HUD_TACTICAL_REBOOT = "screen." + Alpha.MODID + ".hud.tactical.reboot";
        public static final String HUD_TACTICAL_SHIELD_DISABLED = "screen." + Alpha.MODID + ".hud.tactical.shield_disabled";
        
        // Shield Screen
        public static final String SHIELD_TITLE = "screen." + Alpha.MODID + ".shield.title";
        public static final String SHIELD_TOGGLE = "screen." + Alpha.MODID + ".shield.toggle";
        public static final String SHIELD_STATUS = "screen." + Alpha.MODID + ".shield.status";
        public static final String SHIELD_ACTIVE = "screen." + Alpha.MODID + ".shield.active";
        public static final String SHIELD_INACTIVE = "screen." + Alpha.MODID + ".shield.inactive";
        public static final String SHIELD_ENERGY = "screen." + Alpha.MODID + ".shield.energy";
        public static final String SHIELD_POWER_FLOW = "screen." + Alpha.MODID + ".shield.power_flow";
        public static final String SHIELD_SECTOR_ID = "screen." + Alpha.MODID + ".shield.sector_id";
        public static final String SHIELD_STATUS_OPTIMAL = "screen." + Alpha.MODID + ".shield.status_optimal";
        public static final String SHIELD_STATUS_CHARGING = "screen." + Alpha.MODID + ".shield.status_charging";
        public static final String SHIELD_STATUS_COLLAPSED = "screen." + Alpha.MODID + ".shield.status_collapsed";
        public static final String SHIELD_STATUS_RECHARGE_CD = "screen." + Alpha.MODID + ".shield.status_recharge_cd";
        public static final String SHIELD_STATUS_OFFLINE = "screen." + Alpha.MODID + ".shield.status_offline";
        public static final String SHIELD_STATUS_UNLINKED = "screen." + Alpha.MODID + ".shield.status_unlinked";
        public static final String SHIELD_COVERAGE_VOXELS = "screen." + Alpha.MODID + ".shield.coverage_voxels";
        public static final String SHIELD_COVERAGE_BOUNDS = "screen." + Alpha.MODID + ".shield.coverage_bounds";
        public static final String SHIELD_COVERAGE_SPAN = "screen." + Alpha.MODID + ".shield.coverage_span";
        public static final String SHIELD_DEFICIT = "screen." + Alpha.MODID + ".shield.deficit";
        public static final String SHIELD_FULLY_CHARGED = "screen." + Alpha.MODID + ".shield.fully_charged";
        public static final String HUD_TACTICAL_ENERGY = "screen." + Alpha.MODID + ".hud.tactical.energy";
        public static final String HUD_HELM_HEADER = "screen." + Alpha.MODID + ".hud.helm.header";
        public static final String HUD_HELM_WARP_COOLDOWN = "screen." + Alpha.MODID + ".hud.helm.warp_cooldown";
        public static final String HUD_HELM_READY = "screen." + Alpha.MODID + ".hud.helm.ready";
        public static final String HUD_HELM_CONTROLS = "screen." + Alpha.MODID + ".hud.helm.controls";
    }

    public static final class Message {
        // Combat & Turrets
        public static final String TURRET_AIM_LOCKED = "message." + Alpha.MODID + ".turret.aim_locked";
        public static final String TURRET_AIM_RELEASED = "message." + Alpha.MODID + ".turret.aim_released";
        public static final String TURRET_GIMBAL_LIMIT = "message." + Alpha.MODID + ".turret.gimbal_limit";
        public static final String TURRET_OCCUPIED = "message." + Alpha.MODID + ".turret.occupied";
        public static final String COMBAT_TARGET_DESTROYED = "message." + Alpha.MODID + ".combat.destroyed";

        // Shield & Energy
        public static final String SHIELD_COLLAPSE = "message." + Alpha.MODID + ".shield.collapse";
        public static final String SHIELD_PROTECTED_BLOCK = "message." + Alpha.MODID + ".shield.protected_block";
        public static final String REACTOR_DEPLETED = "message." + Alpha.MODID + ".reactor.depleted";
        public static final String ENERGY_INSUFFICIENT = "message." + Alpha.MODID + ".energy.insufficient";

        // Navigation & Warp
        public static final String WARP_PHASE_STATUS = "message." + Alpha.MODID + ".warp.phase";
        public static final String WAYPOINT_NOT_FOUND = "message." + Alpha.MODID + ".waypoint.not_found";
        public static final String MOVEMENT_COOLDOWN_ACTIVE = "message." + Alpha.MODID + ".movement.cooldown_active";
        public static final String COLLISION_WARNING = "message." + Alpha.MODID + ".collision.warning";
        public static final String HELM_CONTROL_ENTER = "message." + Alpha.MODID + ".helm.control.enter";
        public static final String HELM_CONTROL_LEAVE = "message." + Alpha.MODID + ".helm.control.leave";

        // Dev & Tests
        public static final String DEV_CHEAT_ENERGY = "message." + Alpha.MODID + ".dev.cheat_energy";
        public static final String SHADER_TEST_HIT = "message." + Alpha.MODID + ".test.shader_hit";
        public static final String SHADER_TEST_NO_SHIP = "message." + Alpha.MODID + ".test.no_ship";
    }

    public static final class Keybind {
        public static final String CATEGORY = "key.categories." + Alpha.MODID;
        public static final String FIRE_ALL = "key." + Alpha.MODID + ".fire_all";
        public static final String NAV_MENU = "key." + Alpha.MODID + ".nav_menu";
        public static final String EXIT_HELM = "key." + Alpha.MODID + ".exit_helm";
        public static final String OPEN_HELM_CONFIG = "key." + Alpha.MODID + ".open_helm_config";
        public static final String MOUNT_TURRET = "key." + Alpha.MODID + ".mount_turret";
    }

    public static final class Tooltip {
        public static final String REACTOR_CAPACITY = "tooltip." + Alpha.MODID + ".reactor.capacity";
        public static final String WEAPON_TIER = "tooltip." + Alpha.MODID + ".weapon.tier";
        public static final String ENERGY_COST = "tooltip." + Alpha.MODID + ".weapon.energy_cost";
        public static final String SHIELD_STATUS = "tooltip." + Alpha.MODID + ".shield.status";
    }

    public static final class Structure {
        public static final String SHIPWRECK = "structure." + Alpha.MODID + ".shipwreck";
    }

    public static final class Biome {
        public static final String DEEP_SPACE = "biome." + Alpha.MODID + ".deep_space";
    }
}
