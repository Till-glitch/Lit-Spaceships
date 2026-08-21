#version 150

in vec2 uv;
in vec3 v_LocalPos;

out vec4 fragColor;

// Uniforms
uniform float u_EnergyLevel; // 0.0 bis 1.0
uniform float u_GameTime;    // Kontinuierliche Zeit (in Ticks oder Sekunden)

uniform vec4 u_Impact0;
uniform vec4 u_Impact1;
uniform vec4 u_Impact2;
uniform vec4 u_Impact3;

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

float hexGrid(vec2 p) {
    p *= 50.0;
    vec2 q = vec2(p.x * 1.1547005, p.y + p.x * 0.5773502);
    vec2 pi = floor(q);
    vec2 pf = fract(q);
    float v = mod(pi.x + pi.y, 3.0);
    float ca = step(1.0, v);
    float cb = step(2.0, v);
    vec2  ma = step(pf.xy, pf.yx);
    float d = dot(ma, 1.0 - pf.yx) + (1.0 - ca) - cb;
    return fract(d);
}

void main() {
    float hexPattern = hexGrid(uv);
    float edge = smoothstep(0.0, 0.15, hexPattern) * smoothstep(1.0, 0.85, hexPattern);
    float hexAlpha = 0.15 + (1.0 - edge) * 0.65;

    vec3 baseColor = calculateEnergyColor(u_EnergyLevel, u_GameTime);

    float totalRipple = 0.0;
    totalRipple += calculateRippleIntensity(v_LocalPos, u_Impact0);
    totalRipple += calculateRippleIntensity(v_LocalPos, u_Impact1);
    totalRipple += calculateRippleIntensity(v_LocalPos, u_Impact2);
    totalRipple += calculateRippleIntensity(v_LocalPos, u_Impact3);
    totalRipple = clamp(totalRipple, 0.0, 2.0);

    // Der Ripple verschiebt die Farbe ins Weiße (Übersteuerung / Flash)
    vec3 finalColor = mix(baseColor, vec3(1.0, 1.0, 1.0), min(totalRipple * 0.7, 1.0));

    // Der Ripple erhöht lokal die Opazität
    float finalAlpha = clamp(hexAlpha + (totalRipple * 0.6), 0.0, 1.0);

    fragColor = vec4(finalColor, finalAlpha);
}