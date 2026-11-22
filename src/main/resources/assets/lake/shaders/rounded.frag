#version 120
varying vec2 uv;
uniform vec2 size;
uniform float radius;
uniform float fadeEnd;
void main() {
    vec2 q = abs(uv * size - size * 0.5) - size * 0.5 + radius;
    float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    float aa = max(fwidth(d), 0.001);
    float fade = fadeEnd > 0.0 ? 1.0 - smoothstep(max(0.0, size.x - fadeEnd), size.x, uv.x * size.x) : 1.0;
    gl_FragColor = vec4(gl_Color.rgb, gl_Color.a * fade * (1.0 - smoothstep(-aa * 0.5, aa * 0.5, d)));
}
