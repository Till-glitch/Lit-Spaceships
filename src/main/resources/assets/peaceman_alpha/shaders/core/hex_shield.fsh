#version 150

in vec2 uv;
in vec3 v_LocalPos;
in vec3 v_ViewPos;
in vec4 vertexColor;

out vec4 fragColor;

// Uniforms
uniform float u_ZoneEnergies[64]; // 0.0 bis 1.0 pro Zone
uniform float u_GameTime;    // Kontinuierliche Zeit (in Ticks oder Sekunden)
uniform mat4 HexModelViewMat;

uniform vec4 u_Impact0;
uniform vec4 u_Impact1;
uniform vec4 u_Impact2;
uniform vec4 u_Impact3;

uniform int u_ActiveMaskLow;
uniform int u_ActiveMaskHigh;

// Statische Farb-Vektoren (SRGB Farbraum)
const vec3 COLOR_OPTIMAL  = vec3(0.1, 0.9, 1.0);
const vec3 COLOR_CRITICAL = vec3(0.9, 0.7, 0.2);
const vec3 COLOR_FAILURE  = vec3(1.0, 0.1, 0.1);

// Deterministische Hash-Funktion für Pseudo-Zufall
float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

vec3 calculateEnergyColor(float energy, float time) {
    // Hermitesche Interpolationsgewichte für nicht-lineares Blending
    float blendOptimalCritical = smoothstep(0.4, 0.8, energy);
    float blendCriticalFailure = smoothstep(0.15, 0.4, energy);

    // Berechnung der Instabilität für niedrige Energiezustände
    float baseFlicker = (sin(time * 20.0) * 0.5) + 0.5;
    // Diskretes Samplen des Noises für "Zucken" anstatt fließender Übergänge
    float chaoticNoise = hash(floor(time * 15.0));
    float failureIntensity = max(0.3, baseFlicker * chaoticNoise);

    // Modulierte Versagens-Farbe
    vec3 animatedFailureColor = COLOR_FAILURE * (1.0 + (failureIntensity * 0.5));

    // Kaskadierende Farbmischung
    vec3 lowerTierMixed = mix(animatedFailureColor, COLOR_CRITICAL, blendCriticalFailure);
    vec3 finalBaseColor = mix(lowerTierMixed, COLOR_OPTIMAL, blendOptimalCritical);

    return finalBaseColor;
}

float calculateRippleIntensity(vec3 fragPos, vec4 impactData) {
    float timeSinceHit = impactData.w;
    // Early-Exit zur Schonung der Fragment-ALU (< 0.0 bedeutet inaktiv)
    if (timeSinceHit < 0.0) return 0.0;

    vec3 impactPos = impactData.xyz;
    float dist = length(fragPos - impactPos);

    // Konstanten für die physikalische Simulation
    const float WAVE_SPEED = 18.0;      // Ausbreitungsgeschwindigkeit
    const float WAVE_WIDTH = 2.5;       // Schärfe/Dicke des Rings (höher = dünner)
    const float MAX_DISTANCE = 25.0;    // Räumliches Abklingen
    const float MAX_DURATION = 1.5;     // Lebensdauer in Sekunden

    // Position der Wellenfront
    float currentRadius = timeSinceHit * WAVE_SPEED;
    float distFromRadius = abs(dist - currentRadius);

    // Gaußscher Ring
    float waveShape = exp(-(distFromRadius * distFromRadius) * WAVE_WIDTH);

    // Dämpfungsfaktoren
    float spatialDecay = exp(-dist / (MAX_DISTANCE * 0.4));
    float temporalDecay = max(0.0, 1.0 - (timeSinceHit / MAX_DURATION));

    // Nur Fragmente berücksichtigen, die die Welle bereits erreicht hat
    float directionalMask = step(0.0, currentRadius - dist + 2.0);

    return waveShape * spatialDecay * temporalDecay * directionalMask;
}

void main() {
    int zoneId = int(round(vertexColor.r * 255.0));
    if (zoneId > 0 && zoneId <= 64) {
        bool isActive = false;
        if (zoneId <= 32) {
            isActive = (u_ActiveMaskLow & (1 << (zoneId - 1))) != 0;
        } else {
            isActive = (u_ActiveMaskHigh & (1 << (zoneId - 33))) != 0;
        }
        if (!isActive) {
            discard;
        }
    }

    // 1. Blickrichtungsvektor (View-Vektor zur Kamera im View-Space)
    vec3 V = normalize(-v_ViewPos);

    // 2. Normalenvektor berechnen:
    // Der Normalenvektor zeigt von der Mitte des Schildes (Local Origin) nach außen
    vec3 localNormal = length(v_LocalPos) > 0.001 ? normalize(v_LocalPos) : vec3(0.0, 1.0, 0.0);
    vec3 N = normalize(mat3(HexModelViewMat) * localNormal);

    // 3. Physik-basierter Fresnel-Effekt (Rim-Lighting)
    // In der Mitte (dot(N,V) ~ 1) -> fresnel ~ 0 (nahezu vollkommen transparent)
    // Am Rand (dot(N,V) ~ 0) -> fresnel ~ 1 (intensives Glühen)
    float NdotV = clamp(abs(dot(N, V)), 0.0, 1.0);
    float fresnel = pow(1.0 - NdotV, 3.5);

    // 4. Energie-Farbe bestimmen
    float localEnergy = 1.0;
    if (zoneId > 0 && zoneId <= 64) {
        localEnergy = u_ZoneEnergies[zoneId - 1];
    }
    vec3 energyColor = calculateEnergyColor(localEnergy, u_GameTime);

    // 5. Multi-Impact-Schockwellen akkumulieren
    float totalRipple = 0.0;
    totalRipple += calculateRippleIntensity(v_LocalPos, u_Impact0);
    totalRipple += calculateRippleIntensity(v_LocalPos, u_Impact1);
    totalRipple += calculateRippleIntensity(v_LocalPos, u_Impact2);
    totalRipple += calculateRippleIntensity(v_LocalPos, u_Impact3);
    totalRipple = clamp(totalRipple, 0.0, 2.5);

    // 6. Farbe und Rim-Glow kombinieren
    // Am Rand leuchtet die Energiefarbe kräftig auf
    vec3 rimGlowColor = energyColor * (1.0 + fresnel * 0.8);
    // Bei Treffern blitzt die Schockwelle weiß/hell über die Kugel
    vec3 finalColor = mix(rimGlowColor, vec3(1.0, 1.0, 1.0), min(totalRipple * 0.85, 1.0));

    // 7. Transparenz-Berechnung (minimalistischer Clean-Look)
    // Im Ruhezustand: extrem dezente Basis-Transparenz in der Mitte (~0.02) und sanfter Rand (~0.60)
    float baseAlpha = fresnel * 0.60 + 0.02;
    // Einschlagswellen überlagern die Kugel und machen sie temporär sichtbar/leuchtend
    float finalAlpha = clamp(baseAlpha + (totalRipple * 0.85), 0.0, 1.0);

    fragColor = vec4(finalColor, finalAlpha);
}