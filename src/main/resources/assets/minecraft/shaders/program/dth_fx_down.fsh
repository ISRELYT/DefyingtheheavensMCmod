#version 150

// Defying the Heavens, the cinematics' bloom (client/fx/FxPost): a step down to half the size, averaging a 4x4 block of the layer
// above (four bilinear taps), its brightest light kept: below Knee the light is let through only in part, so faint haze doesn't
// bloom as much as a white-hot core.

uniform sampler2D DiffuseSampler;
uniform float Knee;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main() {
    vec3 c = texture(DiffuseSampler, texCoord + oneTexel * vec2(-1.0, -1.0)).rgb
           + texture(DiffuseSampler, texCoord + oneTexel * vec2( 1.0, -1.0)).rgb
           + texture(DiffuseSampler, texCoord + oneTexel * vec2(-1.0,  1.0)).rgb
           + texture(DiffuseSampler, texCoord + oneTexel * vec2( 1.0,  1.0)).rgb;
    c *= 0.25;
    if (Knee > 0.0) {
        float peak = max(c.r, max(c.g, c.b));
        float keep = peak <= 0.0 ? 0.0 : clamp((peak - Knee * 0.5) / (Knee * 0.5), 0.0, 1.0);
        c *= mix(0.25, 1.0, keep);
    }
    fragColor = vec4(c, 1.0);
}
