#version 120
varying vec2 uv;
uniform sampler2D scene;
uniform vec2 footprint;
void main() {
    vec3 color = vec3(0.0);
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            color += texture2D(scene, uv + vec2(float(x), float(y)) * footprint / 3.0).rgb;
        }
    }
    gl_FragColor = vec4(color / 9.0, 1.0);
}
