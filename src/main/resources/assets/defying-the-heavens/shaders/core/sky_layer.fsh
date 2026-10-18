#version 150

// The sky layer (SkyLayer): the realm's own sky, drawn whole (Sampler0), laid over a shader pack's frame only where the
// world's depth (Sampler1, kept from before the hand) is the far plane: open sky, nothing of the world before it.

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    if (texture(Sampler1, texCoord).r < 0.99999) {
        discard;
    }
    fragColor = vec4(texture(Sampler0, texCoord).rgb, 1.0);
}
