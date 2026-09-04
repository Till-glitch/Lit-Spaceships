package com.lit.spaceships.registry;

import com.lit.spaceships.LitSpaceships;

/**
 * Single-Source-of-Truth für alle Translation Keys von Lit Spaceships.
 * Diese Klasse bündelt alle textuellen Bezeichner zur Vermeidung von
 * Magic Strings in der Geschäfts- und Darstellungskoordination.
 */
public final class ModI18n {

    private ModI18n() {}

    public static final class Tab {
        public static final String MAIN = "itemGroup." + LitSpaceships.MODID + ".main";
    }

    public static final class Screen {
        // Reactor Screen
        public static final String REACTOR_TITLE = "screen." + LitSpaceships.MODID + ".reactor.title";
        public static final String REACTOR_ENERGY = "screen." + LitSpaceships.MODID + ".reactor.energy";
        public static final String REACTOR_STORAGE_LOCAL = "screen." + LitSpaceships.MODID + ".reactor.storage_local";
        public static final String REACTOR_STORAGE_GRID = "screen." + LitSpaceships.MODID + ".reactor.storage_grid";
        public static final String REACTOR_GENERATION = "screen." + LitSpaceships.MODID + ".reactor.generation";
        public static final String REACTOR_CONSUMPTION = "screen." + LitSpaceships.MODID + ".reactor.consumption";
        public static final String REACTOR_NET_FLOW = "screen." + LitSpaceships.MODID + ".reactor.net_flow";
        public static final String REACTOR_DRAIN_BREAKDOWN = "screen." + LitSpaceships.MODID + ".reactor.drain_breakdown";
        public static final String REACTOR_PRIORITY_BTN = "screen." + LitSpaceships.MODID + ".reactor.priority_btn";
        public static final String REACTOR_PRIORITY_BALANCED = "screen." + LitSpaceships.MODID + ".reactor.priority_balanced";
        public static final String REACTOR_PRIORITY_SHIELDS = "screen." + LitSpaceships.MODID + ".reactor.priority_shields";
        public static final String REACTOR_PRIORITY_WEAPONS = "screen." + LitSpaceships.MODID + ".reactor.priority_weapons";
        public static final String REACTOR_PRIORITY_ENGINES = "screen." + LitSpaceships.MODID + ".reactor.priority_engines";
        public static final String REACTOR_ALLOCATION_RATIO = "screen." + LitSpaceships.MODID + ".reactor.allocation_ratio";
        public static final String REACTOR_FOCUS_BALANCED = "screen." + LitSpaceships.MODID + ".reactor.focus_balanced";
        public static final String REACTOR_FOCUS_SHIELDS = "screen." + LitSpaceships.MODID + ".reactor.focus_shields";
        public static final String REACTOR_FOCUS_WEAPONS = "screen." + LitSpaceships.MODID + ".reactor.focus_weapons";
        public static final String REACTOR_FOCUS_ENGINES = "screen." + LitSpaceships.MODID + ".reactor.focus_engines";
        public static final String REACTOR_STATUS_OPTIMAL = "screen." + LitSpaceships.MODID + ".reactor.status_optimal";
        public static final String REACTOR_STATUS_HIGH_LOAD = "screen." + LitSpaceships.MODID + ".reactor.status_high_load";
        public static final String REACTOR_STATUS_CRITICAL = "screen." + LitSpaceships.MODID + ".reactor.status_critical";
        public static final String REACTOR_STATUS_STANDBY = "screen." + LitSpaceships.MODID + ".reactor.status_standby";
        public static final String REACTOR_STATUS_UNLINKED = "screen." + LitSpaceships.MODID + ".reactor.status_unlinked";

        // Helm Config Screen
        public static final String HELM_NAV_TITLE = "screen." + LitSpaceships.MODID + ".helm.navigation";
        public static final String HELM_BTN_SAVE = "screen." + LitSpaceships.MODID + ".helm.btn.save";
        public static final String HELM_BTN_FLYTO = "screen." + LitSpaceships.MODID + ".helm.btn.flyto";
        public static final String HELM_BTN_FORWARD = "screen." + LitSpaceships.MODID + ".helm.btn.forward";
        public static final String HELM_BTN_UP = "screen." + LitSpaceships.MODID + ".helm.btn.up";
        public static final String HELM_BTN_DOWN = "screen." + LitSpaceships.MODID + ".helm.btn.down";
        public static final String HELM_WAYPOINT_INPUT = "screen." + LitSpaceships.MODID + ".helm.input.waypoint";
        public static final String HELM_DISTANCE_INPUT = "screen." + LitSpaceships.MODID + ".helm.input.distance";
        public static final String HELM_SHIP_SIZE = "screen." + LitSpaceships.MODID + ".helm.ship_size";
        public static final String HELM_REACTOR_ENERGY = "screen." + LitSpaceships.MODID + ".helm.reactor_energy";
        public static final String HELM_FLIGHT_COST = "screen." + LitSpaceships.MODID + ".helm.flight_cost";
        public static final String HELM_MAX_JUMP = "screen." + LitSpaceships.MODID + ".helm.max_jump";
        public static final String HELM_WAYPOINT_NAV = "screen." + LitSpaceships.MODID + ".helm.waypoint_nav";
        public static final String HELM_MANUAL_JUMP = "screen." + LitSpaceships.MODID + ".helm.manual_jump";
        public static final String HELM_COST_READY = "screen." + LitSpaceships.MODID + ".helm.cost_ready";
        public static final String HELM_COST_MISSING = "screen." + LitSpaceships.MODID + ".helm.cost_missing";
        public static final String HELM_ENTER_DISTANCE = "screen." + LitSpaceships.MODID + ".helm.enter_distance";
        public static final String HELM_WARP_COOLDOWN = "screen." + LitSpaceships.MODID + ".helm.warp_cooldown";
        public static final String HELM_SCREEN_TITLE = "screen." + LitSpaceships.MODID + ".helm.title";

        // Control Screen
        public static final String CONTROL_TITLE = "screen." + LitSpaceships.MODID + ".control.title";
        public static final String CONTROL_STATUS_BOUND = "screen." + LitSpaceships.MODID + ".control.status_bound";
        public static final String CONTROL_STATUS_UNBOUND = "screen." + LitSpaceships.MODID + ".control.status_unbound";
        public static final String CONTROL_STRUCTURAL_HEADER = "screen." + LitSpaceships.MODID + ".control.structural_header";
        public static final String CONTROL_STRUCTURAL_BLOCKS = "screen." + LitSpaceships.MODID + ".control.structural_blocks";
        public static final String CONTROL_STRUCTURAL_BOUNDS = "screen." + LitSpaceships.MODID + ".control.structural_bounds";
        public static final String CONTROL_STRUCTURAL_ANCHOR = "screen." + LitSpaceships.MODID + ".control.structural_anchor";
        public static final String CONTROL_SUBSYSTEM_HEADER = "screen." + LitSpaceships.MODID + ".control.subsystem_header";
        public static final String CONTROL_SUBSYSTEM_CORES = "screen." + LitSpaceships.MODID + ".control.subsystem_cores";
        public static final String CONTROL_SUBSYSTEM_WEAPONS = "screen." + LitSpaceships.MODID + ".control.subsystem_weapons";
        public static final String CONTROL_SUBSYSTEM_NAV = "screen." + LitSpaceships.MODID + ".control.subsystem_nav";
        public static final String CONTROL_BTN_CREATE = "screen." + LitSpaceships.MODID + ".control.btn.create";
        public static final String CONTROL_BTN_UPDATE = "screen." + LitSpaceships.MODID + ".control.btn.update";
        public static final String CONTROL_BTN_DISASSEMBLE = "screen." + LitSpaceships.MODID + ".control.btn.disassemble";
        public static final String CONTROL_BTN_HIGHLIGHT = "screen." + LitSpaceships.MODID + ".control.btn.highlight";
        public static final String CONTROL_HIGHLIGHT_ACTIVE = "screen." + LitSpaceships.MODID + ".control.highlight_active";
        public static final String CONTROL_HIGHLIGHT_INACTIVE = "screen." + LitSpaceships.MODID + ".control.highlight_inactive";
        public static final String CONTROL_BTN_SHIELD = "screen." + LitSpaceships.MODID + ".control.btn.shield";
        public static final String CONTROL_SHIELD_COOLDOWN = "screen." + LitSpaceships.MODID + ".control.shield_cooldown";
        public static final String CONTROL_SHIELD_ACTIVE = "screen." + LitSpaceships.MODID + ".control.shield_active";
        public static final String CONTROL_SHIELD_INACTIVE = "screen." + LitSpaceships.MODID + ".control.shield_inactive";
        public static final String CONTROL_BTN_ROTATE_CW = "screen." + LitSpaceships.MODID + ".control.btn.rotate_cw";
        public static final String CONTROL_BTN_ROTATE_CCW = "screen." + LitSpaceships.MODID + ".control.btn.rotate_ccw";

        // HUD Layers
        public static final String HUD_TACTICAL_HEADER = "screen." + LitSpaceships.MODID + ".hud.tactical.header";
        public static final String HUD_TACTICAL_SHIELD_CRITICAL = "screen." + LitSpaceships.MODID + ".hud.tactical.shield_critical";
        public static final String HUD_TACTICAL_SHIELD_ACTIVE = "screen." + LitSpaceships.MODID + ".hud.tactical.shield_active";
        public static final String HUD_TACTICAL_SHIELD_OFFLINE = "screen." + LitSpaceships.MODID + ".hud.tactical.shield_offline";
        public static final String HUD_TACTICAL_REBOOT = "screen." + LitSpaceships.MODID + ".hud.tactical.reboot";
        public static final String HUD_TACTICAL_SHIELD_DISABLED = "screen." + LitSpaceships.MODID + ".hud.tactical.shield_disabled";
        
        // Shield Screen
        public static final String SHIELD_TITLE = "screen." + LitSpaceships.MODID + ".shield.title";
        public static final String SHIELD_TOGGLE = "screen." + LitSpaceships.MODID + ".shield.toggle";
        public static final String SHIELD_STATUS = "screen." + LitSpaceships.MODID + ".shield.status";
        public static final String SHIELD_ACTIVE = "screen." + LitSpaceships.MODID + ".shield.active";
        public static final String SHIELD_INACTIVE = "screen." + LitSpaceships.MODID + ".shield.inactive";
        public static final String SHIELD_ENERGY = "screen." + LitSpaceships.MODID + ".shield.energy";
        public static final String SHIELD_POWER_FLOW = "screen." + LitSpaceships.MODID + ".shield.power_flow";
        public static final String SHIELD_SECTOR_ID = "screen." + LitSpaceships.MODID + ".shield.sector_id";
        public static final String SHIELD_STATUS_OPTIMAL = "screen." + LitSpaceships.MODID + ".shield.status_optimal";
        public static final String SHIELD_STATUS_CHARGING = "screen." + LitSpaceships.MODID + ".shield.status_charging";
        public static final String SHIELD_STATUS_COLLAPSED = "screen." + LitSpaceships.MODID + ".shield.status_collapsed";
        public static final String SHIELD_STATUS_RECHARGE_CD = "screen." + LitSpaceships.MODID + ".shield.status_recharge_cd";
        public static final String SHIELD_STATUS_OFFLINE = "screen." + LitSpaceships.MODID + ".shield.status_offline";
        public static final String SHIELD_STATUS_UNLINKED = "screen." + LitSpaceships.MODID + ".shield.status_unlinked";
        public static final String SHIELD_COVERAGE_VOXELS = "screen." + LitSpaceships.MODID + ".shield.coverage_voxels";
        public static final String SHIELD_COVERAGE_BOUNDS = "screen." + LitSpaceships.MODID + ".shield.coverage_bounds";
        public static final String SHIELD_COVERAGE_SPAN = "screen." + LitSpaceships.MODID + ".shield.coverage_span";
        public static final String SHIELD_DEFICIT = "screen." + LitSpaceships.MODID + ".shield.deficit";
        public static final String SHIELD_FULLY_CHARGED = "screen." + LitSpaceships.MODID + ".shield.fully_charged";
        public static final String HUD_TACTICAL_ENERGY = "screen." + LitSpaceships.MODID + ".hud.tactical.energy";
        public static final String HUD_HELM_HEADER = "screen." + LitSpaceships.MODID + ".hud.helm.header";
        public static final String HUD_HELM_WARP_COOLDOWN = "screen." + LitSpaceships.MODID + ".hud.helm.warp_cooldown";
        public static final String HUD_HELM_READY = "screen." + LitSpaceships.MODID + ".hud.helm.ready";
        public static final String HUD_HELM_CONTROLS = "screen." + LitSpaceships.MODID + ".hud.helm.controls";

        // Warp Engine Screen
        public static final String WARP_TITLE = "screen." + LitSpaceships.MODID + ".warp.title";
        public static final String WARP_ENGAGE = "screen." + LitSpaceships.MODID + ".warp.engage";
        public static final String WARP_ABORT = "screen." + LitSpaceships.MODID + ".warp.abort";
        public static final String WARP_ENERGY = "screen." + LitSpaceships.MODID + ".warp.energy";
        public static final String WARP_STATUS_READY = "screen." + LitSpaceships.MODID + ".warp.status_ready";
        public static final String WARP_STATUS_CHARGING = "screen." + LitSpaceships.MODID + ".warp.status_charging";
        public static final String WARP_STATUS_COUNTDOWN = "screen." + LitSpaceships.MODID + ".warp.status_countdown";
        public static final String WARP_STATUS_COOLDOWN = "screen." + LitSpaceships.MODID + ".warp.status_cooldown";
        public static final String WARP_STATUS_UNLINKED = "screen." + LitSpaceships.MODID + ".warp.status_unlinked";
        public static final String WARP_DESTINATION = "screen." + LitSpaceships.MODID + ".warp.destination";
        public static final String WARP_DEST_SPACE = "screen." + LitSpaceships.MODID + ".warp.dest_space";
        public static final String WARP_DEST_OVERWORLD = "screen." + LitSpaceships.MODID + ".warp.dest_overworld";
    }

    public static final class Message {
        // Combat & Turrets
        public static final String TURRET_AIM_LOCKED = "message." + LitSpaceships.MODID + ".turret.aim_locked";
        public static final String TURRET_AIM_RELEASED = "message." + LitSpaceships.MODID + ".turret.aim_released";
        public static final String TURRET_GIMBAL_LIMIT = "message." + LitSpaceships.MODID + ".turret.gimbal_limit";
        public static final String TURRET_OCCUPIED = "message." + LitSpaceships.MODID + ".turret.occupied";
        public static final String COMBAT_TARGET_DESTROYED = "message." + LitSpaceships.MODID + ".combat.destroyed";

        // Shield & Energy
        public static final String SHIELD_COLLAPSE = "message." + LitSpaceships.MODID + ".shield.collapse";
        public static final String SHIELD_PROTECTED_BLOCK = "message." + LitSpaceships.MODID + ".shield.protected_block";
        public static final String REACTOR_DEPLETED = "message." + LitSpaceships.MODID + ".reactor.depleted";
        public static final String ENERGY_INSUFFICIENT = "message." + LitSpaceships.MODID + ".energy.insufficient";

        // Navigation & Warp
        public static final String WARP_PHASE_STATUS = "message." + LitSpaceships.MODID + ".warp.phase";
        public static final String WAYPOINT_NOT_FOUND = "message." + LitSpaceships.MODID + ".waypoint.not_found";
        public static final String MOVEMENT_COOLDOWN_ACTIVE = "message." + LitSpaceships.MODID + ".movement.cooldown_active";
        public static final String COLLISION_WARNING = "message." + LitSpaceships.MODID + ".collision.warning";
        public static final String ROTATION_BLOCKED_COLLISION = "message." + LitSpaceships.MODID + ".rotation.blocked_collision";
        public static final String MOVEMENT_BLOCKED_IMMUNE = "message." + LitSpaceships.MODID + ".movement.blocked_immune";
        public static final String WARP_COUNTDOWN_ABORTED = "message." + LitSpaceships.MODID + ".warp.countdown_aborted";
        public static final String WARP_COUNTDOWN_ABORTED_MOVEMENT = "message." + LitSpaceships.MODID + ".warp.countdown_aborted_movement";
        public static final String WARP_COOLDOWN_ACTIVE = "message." + LitSpaceships.MODID + ".warp.cooldown_active";
        public static final String WARP_ENERGY_INSUFFICIENT = "message." + LitSpaceships.MODID + ".warp.energy_insufficient";
        public static final String WARP_OBSTRUCTED = "message." + LitSpaceships.MODID + ".warp.obstructed";
        public static final String WARP_COUNTDOWN_TICK = "message." + LitSpaceships.MODID + ".warp.countdown_tick";
        public static final String HELM_CONTROL_ENTER = "message." + LitSpaceships.MODID + ".helm.control.enter";
        public static final String HELM_CONTROL_LEAVE = "message." + LitSpaceships.MODID + ".helm.control.leave";

        // Dev & Tests
        public static final String DEV_CHEAT_ENERGY = "message." + LitSpaceships.MODID + ".dev.cheat_energy";
        public static final String SHADER_TEST_HIT = "message." + LitSpaceships.MODID + ".test.shader_hit";
        public static final String SHADER_TEST_NO_SHIP = "message." + LitSpaceships.MODID + ".test.no_ship";
    }

    public static final class Keybind {
        public static final String CATEGORY = "key.categories." + LitSpaceships.MODID;
        public static final String FIRE_ALL = "key." + LitSpaceships.MODID + ".fire_all";
        public static final String NAV_MENU = "key." + LitSpaceships.MODID + ".nav_menu";
        public static final String EXIT_HELM = "key." + LitSpaceships.MODID + ".exit_helm";
        public static final String OPEN_HELM_CONFIG = "key." + LitSpaceships.MODID + ".open_helm_config";
        public static final String MOUNT_TURRET = "key." + LitSpaceships.MODID + ".mount_turret";
        public static final String ROTATE_LEFT = "key." + LitSpaceships.MODID + ".rotate_left";
        public static final String ROTATE_RIGHT = "key." + LitSpaceships.MODID + ".rotate_right";
    }

    public static final class Tooltip {
        public static final String REACTOR_CAPACITY = "tooltip." + LitSpaceships.MODID + ".reactor.capacity";
        public static final String WEAPON_TIER = "tooltip." + LitSpaceships.MODID + ".weapon.tier";
        public static final String ENERGY_COST = "tooltip." + LitSpaceships.MODID + ".weapon.energy_cost";
        public static final String SHIELD_STATUS = "tooltip." + LitSpaceships.MODID + ".shield.status";
        public static final String WARP_ENGINE_CAPACITY = "tooltip." + LitSpaceships.MODID + ".warp.capacity";
    }

    public static final class Structure {
        public static final String SHIPWRECK = "structure." + LitSpaceships.MODID + ".shipwreck";
    }

    public static final class Biome {
        public static final String DEEP_SPACE = "biome." + LitSpaceships.MODID + ".deep_space";
    }
}
