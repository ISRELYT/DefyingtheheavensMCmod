#version 150

// A projected nascent soul (SoulRenderer): the player's skin turned to violet light. The violet drifts between deeper and
// paler shades over time and along the body; the edges, seen side-on, glow brightest (Fresnel), fading toward the middle.
// The vertex alpha carries how clearly the viewer sees it: low, it is a faint, grainy violet mist with nothing of the skin;
// whole, the skin's own shapes show through the violet. A vertex red of 0 marks the halo pass (the same body drawn a little
// larger, added on): only its glow, brightest at the silhouette, so the whole soul seems to shine at its edges.
// A vertex green of 0 turns the violet to cyan, deep cyan to pale aqua with an aqua-white halo: the dead (the souls that flow
// down the Soul River, and the Netherworld's ghosts) rather than a living cultivator's projected soul.

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 viewPos;
in vec3 viewNormal;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a < 0.1) {
        discard;
    }
    float clarity = clamp(vertexColor.a, 0.0, 1.0);
    bool halo = vertexColor.r < 0.5;
    bool dead = vertexColor.g < 0.5;
    float t = GameTime * 1200.0; // seconds through the day

    vec3 n = normalize(viewNormal);
    vec3 v = normalize(-viewPos);
    float fresnel = pow(1.0 - clamp(abs(dot(n, v)), 0.0, 1.0), 1.6);

    float shift = 0.5 + 0.5 * sin(t * 1.6 + viewPos.y * 2.5 + viewPos.x * 1.1);
    vec3 violet = dead ? mix(vec3(0.08, 0.58, 0.78), vec3(0.50, 0.95, 1.0), shift)
                       : mix(vec3(0.40, 0.16, 1.0), vec3(0.62, 0.40, 1.0), shift);
    vec3 haloLight = dead ? vec3(0.80, 1.0, 1.0) : vec3(0.85, 0.75, 1.0);

    // Grain: the haze shimmers, coarser and thinner the less clearly it is seen.
    float cell = mix(5.0, 2.0, clarity);
    float grain = hash(floor(gl_FragCoord.xy / cell) + floor(t * 7.0));

    if (halo) {
        float glow = (0.08 + 0.6 * fresnel) * mix(0.45, 1.0, clarity) * mix(0.5 + 0.5 * grain, 1.0, clarity);
        fragColor = vec4(mix(violet, haloLight, fresnel * 0.5), clamp(glow, 0.0, 1.0)) * ColorModulator;
        return;
    }

    float luma = dot(tex.rgb, vec3(0.299, 0.587, 0.114));
    vec3 light = violet * (0.5 + 0.7 * luma);
    vec3 seen = mix(light, tex.rgb * 0.3 + violet * 0.7, clarity * 0.5);
    vec3 color = seen + violet * fresnel * 1.2;

    float alpha = mix(0.05, 0.42, clarity) + fresnel * mix(0.18, 0.45, clarity);
    alpha *= mix(0.2 + 0.8 * grain, 1.0, clarity * clarity);

    fragColor = vec4(min(color, vec3(1.0)), clamp(alpha, 0.0, 1.0)) * ColorModulator;
}
