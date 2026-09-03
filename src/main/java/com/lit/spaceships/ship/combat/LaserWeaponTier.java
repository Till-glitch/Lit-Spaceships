package com.lit.spaceships.ship.combat;

/**
 * Kategorisiert Raumschiff-Laserwaffen in Leistungsstufen.
 * Definiert Konstanten für Reichweite, Energiebedarf, Schadenswerte, Abklingzeiten und Renderfarben.
 */
public enum LaserWeaponTier {
    PULSE_LASER(96.0, 250, 20.0f, 20, 0.1f, 0.9f, 1.0f, 1.0f),
    HEAVY_BEAM(128.0, 50, 3.0f, 0, 1.0f, 0.35f, 0.1f, 1.0f),
    MINING_LASER(48.0, 25, 0.0f, 0, 0.2f, 1.0f, 0.3f, 1.0f);

    private final double maxRange;
    private final int energyCost;      // Pro Schuss bei Pulse, pro Tick bei Continuous
    private final float baseDamage;    // Pro Schuss bei Pulse, pro Tick bei Continuous
    private final int cooldownTicks;   // Abklingzeit nach Schuss
    private final float colorR;
    private final float colorG;
    private final float colorB;
    private final float colorA;

    LaserWeaponTier(double maxRange, int energyCost, float baseDamage, int cooldownTicks,
                    float colorR, float colorG, float colorB, float colorA) {
        this.maxRange = maxRange;
        this.energyCost = energyCost;
        this.baseDamage = baseDamage;
        this.cooldownTicks = cooldownTicks;
        this.colorR = colorR;
        this.colorG = colorG;
        this.colorB = colorB;
        this.colorA = colorA;
    }

    public double getMaxRange() {
        return maxRange;
    }

    public int getEnergyCost() {
        return energyCost;
    }

    public float getBaseDamage() {
        return baseDamage;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public float getColorR() {
        return colorR;
    }

    public float getColorG() {
        return colorG;
    }

    public float getColorB() {
        return colorB;
    }

    public float getColorA() {
        return colorA;
    }
}
