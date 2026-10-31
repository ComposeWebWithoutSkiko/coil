package coil3

import coil3.decode.BrowserImageDecoder

internal actual fun ComponentRegistry.Builder.addAndroidComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder {
    return this
        // Decoders
        .add(BrowserImageDecoder.Factory())
}
