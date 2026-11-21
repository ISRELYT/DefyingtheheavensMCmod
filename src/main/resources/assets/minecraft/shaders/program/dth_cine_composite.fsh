#version 150

// Defying the Heavens, the cinematics' frame (client/fx/FxPost): the world graded (for the breakthrough player only; everyone else's
// grade is left as it is), then the effects' layer laid over it with its bloom, then a vignette.
//   The grade: exposure (open sky darkened further by SkyDark, read from the world's depth), a tint, saturation and contrast.
//   The layer is premultiplied: its light is added, its smoke and cloud cover the world by their alpha. Only the light added is
//   rolled off (softly, spilling into the other channels, so piled-up gold runs to cream-white instead of clipping to flat yellow):
//   a pixel nothing is added to comes out as it went in, so a watcher's own view is never dimmed.
//   The first-person hand is drawn after the layer took the world's depth: where the frame's depth now is nearer than the world's
//   was (the hand, and what it holds), the layer is left off; its bloom still glows over it, as light round the hand would.

uniform sampler2D DiffuseSampler;
uniform sampler2D FxSampler;
uniform sampler2D DepthSampler;
uniform sampler2D MainDepthSampler;
uniform sampler2D Bloom1Sampler;
uniform sampler2D Bloom2Sampler;
uniform sampler2D Bloom3Sampler;

uniform float Exposure;
uniform float SkyDark;
uniform float Saturation;
uniform float Contrast;
uniform vec3 Tint;
uniform float Vignette;
uniform float FxBoost;
uniform float BloomStrength;
uniform float FxCover; // 1 when the layer was drawn this frame; 0 with only a grade set (the last layer's smoke covers nothing then)

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

vec3 rolloff(vec3 c) {
    // Light over 1 in one channel spills into the others (toward white), then everything past 0.75 eases toward 1.
    float over = max(max(c.r, max(c.g, c.b)) - 1.0, 0.0);
    c += vec3(over * 0.35);
    vec3 knee = vec3(0.75);
    vec3 past = max(c - knee, 0.0);
    return min(c, knee) + (1.0 - knee) * (1.0 - exp(-past / (1.0 - knee)));
}

void main() {
    vec3 world = texture(DiffuseSampler, texCoord).rgb;
    float depth = texture(DepthSampler, texCoord).r;
    float sky = depth >= 0.99999 ? 1.0 : 0.0;
    // The far land (from about 65 blocks to about 230, by the depth) darkened most of the way to the sky's darkness: a pale cloud bank or
    // snowy range on the horizon never outshines the storm's dark (only where a grade is set: SkyDark is 1 for everyone else).
    float farLand = smoothstep(0.9993, 0.99985, depth) * 0.8;

    vec3 c = world * Exposure * mix(1.0, SkyDark, max(sky, farLand)) * Tint;
    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = mix(vec3(luma), c, Saturation);
    c = max((c - 0.45) * Contrast + 0.45, 0.0);

    vec4 fx = texture(FxSampler, texCoord);
    float hand = texture(MainDepthSampler, texCoord).r < depth - 0.00002 ? 1.0 : 0.0;
    fx *= 1.0 - hand;
    vec3 bloom = texture(Bloom1Sampler, texCoord).rgb * 0.35 + texture(Bloom2Sampler, texCoord).rgb * 0.45
               + texture(Bloom3Sampler, texCoord).rgb * 0.55;
    vec3 light = fx.rgb * FxBoost + bloom * BloomStrength;
    vec3 under = c * (1.0 - clamp(fx.a, 0.0, 1.0) * FxCover);
    c = under + rolloff(under + light) - rolloff(under);

    vec2 d = (texCoord - 0.5) * vec2(1.0, 0.8);
    c *= 1.0 - Vignette * smoothstep(0.22, 0.72, length(d));
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
