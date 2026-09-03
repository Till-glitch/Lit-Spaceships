package com.lit.spaceships.ship.domain;

/**
 * Taktische Priorisierungs-Modi für die Reaktor-Energieverteilung an Bord des Raumschiffs.
 */
public enum PowerPriority {
    BALANCED(0, "balanced", 0.34f, 0.33f, 0.33f),
    SHIELDS_FIRST(1, "shields_first", 0.70f, 0.15f, 0.15f),
    WEAPONS_FIRST(2, "weapons_first", 0.15f, 0.70f, 0.15f),
    ENGINES_FIRST(3, "engines_first", 0.15f, 0.15f, 0.70f);

    private final int id;
    private final String key;
    private final float shieldShare;
    private final float weaponShare;
    private final float engineShare;

    PowerPriority(int id, String key, float shieldShare, float weaponShare, float engineShare) {
        this.id = id;
        this.key = key;
        this.shieldShare = shieldShare;
        this.weaponShare = weaponShare;
        this.engineShare = engineShare;
    }

    public int getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public float getShieldShare() {
        return shieldShare;
    }

    public float getWeaponShare() {
        return weaponShare;
    }

    public float getEngineShare() {
        return engineShare;
    }

    public PowerPriority next() {
        PowerPriority[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static PowerPriority fromId(int id) {
        for (PowerPriority priority : values()) {
            if (priority.id == id) {
                return priority;
            }
        }
        return BALANCED;
    }
}
