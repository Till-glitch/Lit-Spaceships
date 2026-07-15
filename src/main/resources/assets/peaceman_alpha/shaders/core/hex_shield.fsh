#version 150

in vec2 uv;
out vec4 fragColor;

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

    vec3 shieldColor = vec3(0.1, 0.85, 1.0);
    float alpha = 0.15 + (1.0 - edge) * 0.65;

    fragColor = vec4(shieldColor, alpha);
}