#include <metal_stdlib>
using namespace metal;

struct VertexOut {
    float4 position [[position]];
    float2 uv;
};

struct Uniforms {
    float4 resolutionTime;
    float4 mouse;
    int layers;
    float speed;
    float depth;
    float width;
    float opacity;
    float3 flakeColor;
};

static inline float mod1(float value) {
    return value - floor(value);
}

static inline float2 mod1(float2 value) {
    return value - floor(value);
}

vertex VertexOut vertexMain(uint vertexID [[vertex_id]]) {
    float2 positions[4] = {
        float2(-1.0, -1.0),
        float2( 1.0, -1.0),
        float2(-1.0,  1.0),
        float2( 1.0,  1.0)
    };

    float2 uvs[4] = {
        float2(0.0, 0.0),
        float2(1.0, 0.0),
        float2(0.0, 1.0),
        float2(1.0, 1.0)
    };

    VertexOut out;
    out.position = float4(positions[vertexID], 0.0, 1.0);
    out.uv = uvs[vertexID];
    return out;
}

fragment float4 fragmentMain(VertexOut in [[stage_in]],
                             constant Uniforms& uniforms [[buffer(0)]]) {
    float2 resolution = max(uniforms.resolutionTime.xy, float2(1.0));
    float iTime = uniforms.resolutionTime.z;
    float2 iMouse = uniforms.mouse.xy;
    
    // Используем динамические параметры из uniforms
    int LAYERS = uniforms.layers;
    float SPEED = uniforms.speed;
    float DEPTH = uniforms.depth;
    float WIDTH = uniforms.width;

    const float3x3 p = float3x3(float3(13.323122, 23.5112, 21.71123),
                                float3(21.1212, 28.7312, 11.9312),
                                float3(21.8112, 14.7212, 61.3934));

    float2 fragCoord = in.uv * resolution;
    float2 uv = iMouse / resolution + float2(1.0, resolution.y / resolution.x) * fragCoord / resolution;

    float3 acc = float3(0.0);
    float dof = 5.0 * sin(iTime * 0.1);

    for (int i = 0; i < LAYERS; ++i) {
        float fi = float(i);
        float2 q = uv * (1.0 + fi * DEPTH);
        q += float2(q.y * (WIDTH * mod1(fi * 7.238917) - WIDTH * 0.5),
                    SPEED * iTime / (1.0 + fi * DEPTH * 0.03));

        float3 n = float3(floor(q), 31.189 + fi);
        float3 m = floor(n) * 0.00001 + fract(n);
        float3 mp = (float3(31415.9) + m) / fract(p * m);
        float3 r = fract(mp);

        float2 s = abs(mod1(q) - 0.5 + 0.9 * r.xy - 0.45);
        s += 0.01 * abs(2.0 * fract(10.0 * q.yx) - 1.0);
        float d = 0.6 * max(s.x - s.y, s.x + s.y) + max(s.x, s.y) - 0.01;
        float edge = 0.005 + 0.05 * min(0.5 * abs(fi - 5.0 - dof), 1.0);

        float contribution = smoothstep(edge, -edge, d) * (r.x / (1.0 + 0.02 * fi * DEPTH));
        acc += float3(contribution);
    }

    float intensity = clamp(acc.x, 0.0, 1.0);
    // Используем цвет из настроек вместо хардкода
    float3 color = mix(float3(1.0), uniforms.flakeColor, intensity);
    return float4(color, uniforms.opacity);
}

