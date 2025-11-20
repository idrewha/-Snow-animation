// Портирование Metal шейдеров в GLSL для WebGL
// Оригинал: SnowShader.metal

// Vertex Shader - портирован из Metal
const vertexShader = `
varying vec2 vUv;

void main() {
    vUv = uv;
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
}
`;

// Fragment Shader - портирован из Metal fragmentMain
const fragmentShader = `
uniform vec4 uResolutionTime; // xy: resolution, z: time, w: unused
uniform vec4 uMouse;          // xy: mouse position, zw: unused
uniform int uLayers;          // Количество слоёв снега
uniform float uDepth;         // Глубина эффекта
uniform float uWidth;         // Ширина разброса
uniform float uSpeed;         // Скорость падения
uniform vec3 uFlakeColor;     // Цвет снежинок
uniform float uOpacity;       // Прозрачность

varying vec2 vUv;

// Портировано из Metal: mod1 функция
float mod1(float value) {
    return value - floor(value);
}

vec2 mod1(vec2 value) {
    return value - floor(value);
}

void main() {
    vec2 resolution = max(uResolutionTime.xy, vec2(1.0));
    float iTime = uResolutionTime.z;
    vec2 iMouse = uMouse.xy;

    // Матрица из оригинального Metal шейдера
    mat3 p = mat3(
        vec3(13.323122, 23.5112, 21.71123),
        vec3(21.1212, 28.7312, 11.9312),
        vec3(21.8112, 14.7212, 61.3934)
    );

                vec2 fragCoord = vUv * resolution;
                vec2 uv = vec2(1.0, resolution.y / resolution.x) * fragCoord / resolution;

    vec3 acc = vec3(0.0);
    float dof = 5.0 * sin(iTime * 0.1);

    // Основной цикл рендеринга снежинок - портирован 1:1 из Metal
    for (int i = 0; i < 250; ++i) {
        if (i >= uLayers) break; // Динамическое ограничение слоёв
        
        float fi = float(i);
        vec2 q = uv * (1.0 + fi * uDepth);
        q += vec2(
            q.y * (uWidth * mod1(fi * 7.238917) - uWidth * 0.5),
            uSpeed * iTime / (1.0 + fi * uDepth * 0.03)
        );

        vec3 n = vec3(floor(q), 31.189 + fi);
        vec3 m = floor(n) * 0.00001 + fract(n);
        vec3 mp = (vec3(31415.9) + m) / fract(p * m);
        vec3 r = fract(mp);

        vec2 s = abs(mod1(q) - 0.5 + 0.9 * r.xy - 0.45);
        s += 0.01 * abs(2.0 * fract(10.0 * q.yx) - 1.0);
        float d = 0.6 * max(s.x - s.y, s.x + s.y) + max(s.x, s.y) - 0.01;
        float edge = 0.005 + 0.05 * min(0.5 * abs(fi - 5.0 - dof), 1.0);

        float contribution = smoothstep(edge, -edge, d) * (r.x / (1.0 + 0.02 * fi * uDepth));
        acc += vec3(contribution);
    }

    float intensity = clamp(acc.x, 0.0, 1.0);
    vec3 color = mix(vec3(1.0), uFlakeColor, intensity); // Смешивание с белым фоном
    
    gl_FragColor = vec4(color, uOpacity);
}
`;

// Конфигурации режимов снега (из Metal: LIGHT_SNOW и HEAVY_SNOW)
const snowModes = {
    light: {
        layers: 50,
        depth: 0.5,
        width: 0.3,
        speed: 1.2
    },
    heavy: {
        layers: 200,
        depth: 0.1,
        width: 0.8,
        speed: 1.5
    }
};

// Основной класс для управления Three.js сценой
class SnowScene {
    constructor() {
        this.canvas = document.getElementById('canvas');
        this.mode = 'light';
        this.startTime = Date.now();
        this.frameCount = 0;
        this.lastFpsUpdate = Date.now();
        
        this.initThree();
        this.initMaterial();
        this.createScene();
        this.setupEventListeners();
        this.animate();
    }

    initThree() {
        // Инициализация Three.js - аналог MTLDevice и MTKView
        this.renderer = new THREE.WebGLRenderer({
            canvas: this.canvas,
            antialias: false,
            alpha: false
        });
        this.renderer.setPixelRatio(window.devicePixelRatio);
        this.renderer.setSize(window.innerWidth, window.innerHeight);

        // Ортографическая камера для fullscreen quad
        this.camera = new THREE.OrthographicCamera(-1, 1, 1, -1, 0, 1);
        this.scene = new THREE.Scene();
    }

    initMaterial() {
        // Создание ShaderMaterial с портированными шейдерами
        const config = snowModes[this.mode];
        
        this.uniforms = {
            uResolutionTime: { value: new THREE.Vector4(
                window.innerWidth,
                window.innerHeight,
                0.0,
                0.0
            )},
            uMouse: { value: new THREE.Vector4(0, 0, 0, 0) },
            uLayers: { value: config.layers },
            uDepth: { value: config.depth },
            uWidth: { value: config.width },
            uSpeed: { value: config.speed },
            uFlakeColor: { value: new THREE.Vector3(0.5, 0.5, 0.5) },
            uOpacity: { value: 1.0 }
        };

        this.material = new THREE.ShaderMaterial({
            uniforms: this.uniforms,
            vertexShader: vertexShader,
            fragmentShader: fragmentShader,
            transparent: true
        });
    }

    createScene() {
        // Создание fullscreen quad (аналог triangle strip с 4 вершинами в Metal)
        const geometry = new THREE.PlaneGeometry(2, 2);
        const mesh = new THREE.Mesh(geometry, this.material);
        this.scene.add(mesh);
    }

    setupEventListeners() {
        // Обработка изменения размера окна
        window.addEventListener('resize', () => this.onResize());

        // Переключение режимов снега
        document.querySelectorAll('input[name="mode"]').forEach(radio => {
            radio.addEventListener('change', (e) => this.changeMode(e.target.value));
        });

        // Обработка слайдера количества слоёв
        const layersSlider = document.getElementById('layersSlider');
        const layersValue = document.getElementById('layersValue');
        
        if (layersSlider && layersValue) {
            layersSlider.addEventListener('input', (e) => {
                const value = parseInt(e.target.value);
                layersValue.textContent = value;
                this.setLayers(value);
            });
        }

        // Обработка слайдера скорости падения
        const speedSlider = document.getElementById('speedSlider');
        const speedValue = document.getElementById('speedValue');
        
        if (speedSlider && speedValue) {
            speedSlider.addEventListener('input', (e) => {
                const value = parseFloat(e.target.value);
                speedValue.textContent = value.toFixed(1);
                this.setSpeed(value);
            });
        }

        // Обработка слайдера прозрачности
        const opacitySlider = document.getElementById('opacitySlider');
        const opacityValue = document.getElementById('opacityValue');
        
        if (opacitySlider && opacityValue) {
            opacitySlider.addEventListener('input', (e) => {
                const value = parseFloat(e.target.value);
                opacityValue.textContent = value.toFixed(2);
                this.setOpacity(value);
            });
        }

        // Обработка color picker
        const colorPicker = document.getElementById('colorPicker');
        const colorHex = document.getElementById('colorHex');
        
        if (colorPicker && colorHex) {
            colorPicker.addEventListener('input', (e) => {
                const hex = e.target.value;
                colorHex.textContent = hex;
                this.setFlakeColor(hex);
            });
        }
    }

    onResize() {
        const width = window.innerWidth;
        const height = window.innerHeight;
        
        this.renderer.setSize(width, height);
        this.uniforms.uResolutionTime.value.x = width;
        this.uniforms.uResolutionTime.value.y = height;
    }

    changeMode(newMode) {
        this.mode = newMode;
        const config = snowModes[newMode];
        
        // Обновление uniforms при смене режима
        this.uniforms.uLayers.value = config.layers;
        this.uniforms.uDepth.value = config.depth;
        this.uniforms.uWidth.value = config.width;
        this.uniforms.uSpeed.value = config.speed;

        // Синхронизируем слайдеры с выбранным режимом
        const layersSlider = document.getElementById('layersSlider');
        const layersValue = document.getElementById('layersValue');
        const speedSlider = document.getElementById('speedSlider');
        const speedValue = document.getElementById('speedValue');
        
        if (layersSlider && layersValue) {
            layersSlider.value = config.layers;
            layersValue.textContent = config.layers;
        }
        
        if (speedSlider && speedValue) {
            speedSlider.value = config.speed;
            speedValue.textContent = config.speed.toFixed(1);
        }

        console.log(`Режим изменён на: ${newMode}`, config);
    }

    setLayers(layers) {
        this.uniforms.uLayers.value = layers;
        console.log(`Количество слоёв: ${layers}`);
    }

    setSpeed(speed) {
        this.uniforms.uSpeed.value = speed;
        console.log(`Скорость падения: ${speed}`);
    }

    setOpacity(opacity) {
        this.uniforms.uOpacity.value = opacity;
        console.log(`Прозрачность: ${opacity}`);
    }

    setFlakeColor(hexColor) {
        // Конвертируем HEX в RGB (0.0-1.0)
        const r = parseInt(hexColor.slice(1, 3), 16) / 255;
        const g = parseInt(hexColor.slice(3, 5), 16) / 255;
        const b = parseInt(hexColor.slice(5, 7), 16) / 255;
        
        this.uniforms.uFlakeColor.value.set(r, g, b);
        console.log(`Цвет снежинок: ${hexColor} (${r.toFixed(2)}, ${g.toFixed(2)}, ${b.toFixed(2)})`);
    }

    updateFPS() {
        this.frameCount++;
        const now = Date.now();
        
        if (now - this.lastFpsUpdate >= 1000) {
            const fps = Math.round(this.frameCount * 1000 / (now - this.lastFpsUpdate));
            document.getElementById('fps').textContent = fps;
            this.frameCount = 0;
            this.lastFpsUpdate = now;
        }
    }

    animate() {
        requestAnimationFrame(() => this.animate());

        // Обновление времени (аналог CACurrentMediaTime в Metal)
        const currentTime = (Date.now() - this.startTime) / 1000.0;
        this.uniforms.uResolutionTime.value.z = currentTime;

        // Рендеринг кадра (аналог draw(in view: MTKView) в Metal)
        this.renderer.render(this.scene, this.camera);
        
        this.updateFPS();
    }
}

// Инициализация при загрузке страницы
window.addEventListener('DOMContentLoaded', () => {
    new SnowScene();
    console.log('Snow animation initialized - портировано из Metal');
});

