#version 120
varying vec2 uv;
uniform vec2 size;
uniform float radius;
uniform float feather;
uniform float coreSigma;
uniform float diskOpacity;
uniform float coreOpacity;
void main() {
    float r = length((uv - 0.5) * size);
    float disk = (1.0 - smoothstep(radius - feather, radius + feather, r)) * diskOpacity;
    float core = exp(-r * r / (2.0 * coreSigma * coreSigma)) * coreOpacity;
    gl_FragColor = vec4(gl_Color.rgb, gl_Color.a * (1.0 - (1.0 - disk) * (1.0 - core)));
}
