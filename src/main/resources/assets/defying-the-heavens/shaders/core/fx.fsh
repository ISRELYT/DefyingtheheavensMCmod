#version 150

// The cinematics' effects (client/fx/Fx), drawn into their own high-range layer (FxLayer), premultiplied: light is added without
// covering anything (alpha 0), smoke and cloud cover what's behind them. The blending itself is set by Fx, per batch.
//   FxMode 0, light in the texture's own colours: its colour times its alpha, times the vertex colour and its alpha, times FxGain.
//   FxMode 1, light through the heat ramp: the texture's alpha times the vertex alpha is the heat; the hottest runs to cream-white,
//             cooler light takes the vertex colour, so every ribbon, ring and flame has a white-hot core and coloured edges.
//   FxMode 2, cover: the texture's colour times the vertex colour, covering by their alphas (cloud, smoke, dust).

uniform sampler2D Sampler0;
uniform float FxMode;
uniform float FxGain;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 t = texture(Sampler0, texCoord0);
    if (FxMode < 0.5) {
        fragColor = vec4(t.rgb * t.a * vertexColor.rgb * vertexColor.a * FxGain, 0.0);
    } else if (FxMode < 1.5) {
        float heat = t.a * vertexColor.a;
        vec3 hot = vec3(1.0, 0.975, 0.86);
        vec3 c = mix(vertexColor.rgb, hot, smoothstep(0.5, 1.0, heat));
        fragColor = vec4(c * heat * FxGain, 0.0);
    } else {
        float a = t.a * vertexColor.a;
        fragColor = vec4(t.rgb * vertexColor.rgb * a, a);
    }
}
