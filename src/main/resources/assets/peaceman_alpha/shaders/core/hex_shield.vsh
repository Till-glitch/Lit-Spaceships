#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

// Unsere unantastbaren Matrizen
uniform mat4 HexModelViewMat;
uniform mat4 HexProjMat;

out vec2 uv;
out vec3 v_LocalPos;
out vec3 v_ViewPos;
out vec4 vertexColor;

void main() {
    vec4 viewPos = HexModelViewMat * vec4(Position, 1.0);
    gl_Position = HexProjMat * viewPos;
    
    uv = UV0;
    v_LocalPos = Position;
    v_ViewPos = viewPos.xyz;
    vertexColor = Color;
}