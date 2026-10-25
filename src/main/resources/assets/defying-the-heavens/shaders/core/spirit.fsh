#version 150

// A spirit the spells call up (SpellGhosts), drawn as the soul shader draws the dead but in colours of its own: a body of light (Smoke 0:
// White Impermanence's bone-white, the Heaven-Demon Avatar's gold and crimson) or of smoke (Smoke 1: Black Impermanence's ink, dense and
// dark with its edges lit). The skin's own shapes show faintly through; its edges, seen side-on, glow (Fresnel). Core and Pale are the
// body's two shades (it drifts between them over time and along it), Edge the glow at its edges and its halo's.
// The vertex alpha carries how clearly it shows; a vertex red of 0 marks the halo pass (the same body drawn a little larger, added on: only
// its edge glow). A vertex green below 1 marks how far toward Second (a second set of colours, Core2, Pale2, Edge2) it leans there: the
// avatar's side of heaven and its side of the demon, met in a burning seam.

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;
uniform vec3 Core;
uniform vec3 Pale;
uniform vec3 Edge;
uniform vec3 Core2;
uniform vec3 Pale2;
uniform vec3 Edge2;
uniform float Smoke;

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
    float second = clamp(1.0 - vertexColor.g, 0.0, 1.0);
    float t = GameTime * 1200.0;

    vec3 n = normalize(viewNormal);
    vec3 v = normalize(-viewPos);
    float fresnel = pow(1.0 - clamp(abs(dot(n, v)), 0.0, 1.0), 1.6);

    float shift = 0.5 + 0.5 * sin(t * 1.4 + viewPos.y * 2.2 + viewPos.x * 0.9);
    // Where the two sides meet, a seam of fire: brightest where it leans half and half.
    float seam = 1.0 - abs(second * 2.0 - 1.0);
    vec3 core = mix(Core, Core2, second), pale = mix(Pale, Pale2, second), edge = mix(Edge, Edge2, second);
    vec3 shade = mix(core, pale, shift);

    float grain = hash(floor(gl_FragCoord.xy / 2.0) + floor(t * 7.0));

    if (halo) {
        float glow = (0.1 + 0.65 * fresnel) * mix(0.45, 1.0, clarity);
        fragColor = vec4(edge * (1.0 + seam * 0.8), clamp(glow, 0.0, 1.0)) * ColorModulator;
        return;
    }

    float luma = dot(tex.rgb, vec3(0.299, 0.587, 0.114));
    vec3 color;
    float alpha;
    if (Smoke > 0.5) {
        // Smoke: dark, the skin's shapes a little darker yet, roiling; its edges lit.
        float roil = 0.75 + 0.25 * sin(t * 2.0 + viewPos.y * 3.0 + grain * 2.0);
        color = shade * (0.25 + 0.35 * luma) * roil + edge * fresnel * 1.4;
        alpha = mix(0.35, 0.82, clarity) + fresnel * 0.15;
    } else {
        // Light: the skin's shapes faint within it, its edges brightest.
        color = shade * (0.55 + 0.6 * luma) + edge * fresnel * 1.1;
        alpha = mix(0.08, 0.55, clarity) + fresnel * mix(0.2, 0.4, clarity);
    }
    color += edge * seam * 0.9;
    fragColor = vec4(min(color, vec3(1.0)), clamp(alpha, 0.0, 1.0)) * ColorModulator;
}
