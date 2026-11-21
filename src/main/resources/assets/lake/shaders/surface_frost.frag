#version 120

uniform sampler2D scene;
uniform vec2 viewportOrigin;
uniform vec2 viewportSize;
uniform vec2 panelOrigin;
uniform vec2 panelSize;
uniform vec2 blurStep;
uniform float radius;
uniform float opacity;
uniform vec3 tintTop;
uniform vec3 tintBottom;

float weight(int offset) {
    if (offset == 0) return 0.38774;
    if (offset == 1 || offset == -1) return 0.24477;
    return 0.06136;
}

void main() {
    vec2 local = gl_FragCoord.xy - panelOrigin;
    vec2 corner = abs(local - panelSize * 0.5) - (panelSize * 0.5 - radius);
    float distance = length(max(corner, 0.0)) + min(max(corner.x, corner.y), 0.0) - radius;
    float mask = 1.0 - smoothstep(-0.75, 0.75, distance);
    if (mask <= 0.0) discard;

    vec2 uv = (gl_FragCoord.xy - viewportOrigin) / viewportSize;
    vec3 blurred = vec3(0.0);
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            blurred += texture2D(scene, uv + vec2(float(x), float(y)) * blurStep).rgb * weight(x) * weight(y);
        }
    }

    float vertical = clamp(local.y / panelSize.y, 0.0, 1.0);
    vec3 tint = mix(tintBottom, tintTop, vertical);
    vec3 glass = mix(blurred, tint, 0.40);
    float rim = smoothstep(-1.8, -0.25, distance) * 0.12;
    float highlight = pow(vertical, 8.0) * 0.055;
    float grain = (fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5) * 0.008;
    gl_FragColor = vec4(glass + rim + highlight + grain, mask * opacity * 0.94);
}
