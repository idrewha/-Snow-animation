import Foundation
import SwiftUI
import MetalKit

private struct Uniforms {
    var resolutionTime: SIMD4<Float>
    var mouse: SIMD4<Float>
    var layers: Int32
    var speed: Float
    var depth: Float
    var width: Float
    var opacity: Float
    var flakeColor: SIMD3<Float>
}

struct SnowView: View {
    var settings: SnowSettings = SnowSettings()
    
    var body: some View {
        SnowMetalView(settings: settings)
            .ignoresSafeArea()
    }
}

#if os(iOS)
typealias PlatformViewRepresentable = UIViewRepresentable
#elseif os(macOS)
typealias PlatformViewRepresentable = NSViewRepresentable
#endif

struct SnowMetalView: PlatformViewRepresentable {
    var settings: SnowSettings
    
    func makeCoordinator() -> RendererCoordinator {
        RendererCoordinator(settings: settings)
    }

    #if os(iOS)
    func makeUIView(context: Context) -> MTKView {
        createMTKView(context: context)
    }

    func updateUIView(_ uiView: MTKView, context: Context) {
        context.coordinator.settings = settings
        context.coordinator.update(view: uiView)
    }
    #elseif os(macOS)
    func makeNSView(context: Context) -> MTKView {
        createMTKView(context: context)
    }

    func updateNSView(_ nsView: MTKView, context: Context) {
        context.coordinator.settings = settings
        context.coordinator.update(view: nsView)
    }
    #endif

    private func createMTKView(context: Context) -> MTKView {
        guard let device = MTLCreateSystemDefaultDevice() else {
            fatalError("Unable to create Metal device.")
        }

        let view = MTKView(frame: .zero, device: device)
        view.colorPixelFormat = .bgra8Unorm
        view.clearColor = MTLClearColor(red: 1.0, green: 1.0, blue: 1.0, alpha: 1.0)
        view.isPaused = false
        view.enableSetNeedsDisplay = false
        view.preferredFramesPerSecond = 60
        view.framebufferOnly = false

        context.coordinator.configure(with: view)
        return view
    }
}

final class RendererCoordinator: NSObject, MTKViewDelegate {
    private var device: MTLDevice?
    private var commandQueue: MTLCommandQueue?
    private var pipelineState: MTLRenderPipelineState?
    private var startTime: CFAbsoluteTime = CACurrentMediaTime()
    private var mousePosition = SIMD2<Float>(repeating: 0)
    var settings: SnowSettings
    
    init(settings: SnowSettings = SnowSettings()) {
        self.settings = settings
        super.init()
    }

    func configure(with view: MTKView) {
        if device == nil {
            device = view.device
        }
        guard let device else { return }

        if commandQueue == nil {
            commandQueue = device.makeCommandQueue()
        }

        if pipelineState == nil {
            do {
                pipelineState = try Self.buildPipelineState(device: device, pixelFormat: view.colorPixelFormat)
            } catch {
                fatalError("Failed to create Metal pipeline: \(error)")
            }
        }

        view.delegate = self
    }

    func update(view: MTKView) {
        view.isPaused = false
    }

    func draw(in view: MTKView) {
        guard let pipelineState,
              let commandQueue,
              let descriptor = view.currentRenderPassDescriptor,
              let drawable = view.currentDrawable else {
            return
        }

        let currentTime = Float(CACurrentMediaTime() - startTime)
        let resolution = SIMD2<Float>(Float(view.drawableSize.width), Float(view.drawableSize.height))
        
        // Конвертируем SwiftUI Color в RGB (0-1)
        let colorComponents = settings.flakeColor.cgColor?.components ?? [0.5, 0.5, 0.5, 1.0]
        let flakeColorRGB = SIMD3<Float>(
            Float(colorComponents[0]),
            Float(colorComponents[1]),
            Float(colorComponents[2])
        )

        var uniforms = Uniforms(
            resolutionTime: SIMD4<Float>(resolution.x, resolution.y, currentTime, 0),
            mouse: SIMD4<Float>(mousePosition.x, mousePosition.y, 0, 0),
            layers: Int32(settings.layers),
            speed: settings.speed,
            depth: settings.depth,
            width: settings.width,
            opacity: settings.opacity,
            flakeColor: flakeColorRGB
        )

        guard let commandBuffer = commandQueue.makeCommandBuffer(),
              let encoder = commandBuffer.makeRenderCommandEncoder(descriptor: descriptor) else {
            return
        }

        encoder.setRenderPipelineState(pipelineState)
        encoder.setFragmentBytes(&uniforms, length: MemoryLayout<Uniforms>.stride, index: 0)
        encoder.drawPrimitives(type: .triangleStrip, vertexStart: 0, vertexCount: 4)
        encoder.endEncoding()

        commandBuffer.present(drawable)
        commandBuffer.commit()
    }

    func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {
        // No-op for now
    }

    private static func buildPipelineState(device: MTLDevice, pixelFormat: MTLPixelFormat) throws -> MTLRenderPipelineState {
        guard let library = device.makeDefaultLibrary() else {
            throw RendererError.libraryCreationFailed
        }

        guard let vertexFunction = library.makeFunction(name: "vertexMain"),
              let fragmentFunction = library.makeFunction(name: "fragmentMain") else {
            throw RendererError.functionNotFound
        }

        let descriptor = MTLRenderPipelineDescriptor()
        descriptor.vertexFunction = vertexFunction
        descriptor.fragmentFunction = fragmentFunction
        descriptor.colorAttachments[0].pixelFormat = pixelFormat

        return try device.makeRenderPipelineState(descriptor: descriptor)
    }
}

enum RendererError: Error {
    case libraryCreationFailed
    case functionNotFound
}

