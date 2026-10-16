#version 150

// A soul's figure drawn again only to give the world's depth back over it (SoulRenderer#restoreDepth): its position only.

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
