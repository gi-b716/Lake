#version 120
varying vec2 uv;
uniform sampler2D scene;
uniform vec2 stepSize;
void main() {
    vec3 result = texture2D(scene, uv).rgb * 0.227027;
    result += texture2D(scene, uv + stepSize * 1.384615).rgb * 0.316216;
    result += texture2D(scene, uv - stepSize * 1.384615).rgb * 0.316216;
    result += texture2D(scene, uv + stepSize * 3.230769).rgb * 0.070270;
    result += texture2D(scene, uv - stepSize * 3.230769).rgb * 0.070270;
    gl_FragColor = vec4(result, 1.0);
}
