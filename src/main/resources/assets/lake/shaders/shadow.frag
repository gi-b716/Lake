#version 120
varying vec2 uv;
uniform vec2 size;
uniform float radius;
uniform float padding;
uniform float sigma;
void main() {
    vec2 p = (uv - 0.5) * (size + 2.0 * padding);
    vec2 q = abs(p) - size * 0.5 + radius;
    float d = max(length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius, 0.0);
    float alpha = exp(-d * d / (2.0 * sigma * sigma));
    gl_FragColor = vec4(gl_Color.rgb, gl_Color.a * alpha);
}
