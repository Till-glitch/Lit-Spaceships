#version 150

in vec3 Position;
in vec2 UV0;

// Unsere unantastbaren Matrizen
uniform mat4 HexModelViewMat;
uniform mat4 HexProjMat;

out vec2 uv;
out vec3 v_LocalPos;

void main() {
    // Rotation, Position und Sichtwinkel in einem Rutsch anwenden
    gl_Position = HexProjMat * HexModelViewMat * vec4(Position, 1.0);
    uv = UV0;
    v_LocalPos = Position;
}