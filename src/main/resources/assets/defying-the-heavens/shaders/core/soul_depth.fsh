#version 150

// Writes the world's depth at this pixel (Sampler0: the frame's own depth texture) over the depth a soul laid down on the sense
// layer (SoulRenderer#restoreDepth), so it hides nothing drawn there after it. Nothing of colour is written (its mask is off).

uniform sampler2D Sampler0;

out vec4 fragColor;

void main() {
    gl_FragDepth = texelFetch(Sampler0, ivec2(gl_FragCoord.xy), 0).r;
    fragColor = vec4(0.0);
}
