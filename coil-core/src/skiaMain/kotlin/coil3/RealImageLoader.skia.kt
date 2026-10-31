package coil3

import coil3.decode.SkiaImageDecoder

internal actual fun ComponentRegistry.Builder.addAndroidComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder {
    return this
        // Decoders
        .add(SkiaImageDecoder.Factory())
}
