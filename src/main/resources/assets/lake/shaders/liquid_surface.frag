#version 120
varying vec2 uv;
uniform sampler2D scene;
uniform vec2 viewportOrigin;
uniform vec2 viewportSize;
uniform vec2 size;
uniform float radius;
uniform float opacity;
uniform float transmission;
uniform float refraction;
void main() {
    vec2 p = (uv - 0.5) * size;
    vec2 q = abs(p) - size * 0.5 + radius;
    float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    float aa = max(fwidth(d), 0.001);
    float mask = 1.0 - smoothstep(-aa * 0.5, aa * 0.5, d);
    if (mask <= 0.0) discard;
    vec2 edge = p - clamp(p, -size * 0.5 + radius, size * 0.5 - radius);
    vec2 normal = edge / max(length(edge), 0.001);
    float lens = exp(-max(-d, 0.0) / max(radius * 0.35, 0.001));
    vec2 sceneUv = (gl_FragCoord.xy - viewportOrigin + normal * vec2(1.0, -1.0) * lens * refraction) / viewportSize;
    vec3 background = texture2D(scene, sceneUv).rgb;
    vec3 glass = background * transmission + vec3(0.055);
    gl_FragColor = vec4(glass, mask * opacity);
}
