#version 150

// Defying the Heavens, the cinematics' radial blur (client/fx/FxPost): the frame smeared outward from Center (where the push or the
// whip is aimed), more the further out, by Strength (0: a plain copy). Twelve taps toward the centre.

uniform sampler2D DiffuseSampler;
uniform vec2 Center;
uniform float Strength;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main() {
    if (Strength <= 0.001) {
        fragColor = vec4(texture(DiffuseSampler, texCoord).rgb, 1.0);
        return;
    }
    vec2 toward = Center - texCoord;
    vec3 sum = vec3(0.0);
    float weight = 0.0;
    for (int i = 0; i < 12; i++) {
        float f = float(i) / 11.0;
        float w = 1.0 - f * 0.6;
        sum += texture(DiffuseSampler, texCoord + toward * f * Strength * 0.22).rgb * w;
        weight += w;
    }
    fragColor = vec4(sum / weight, 1.0);
}
