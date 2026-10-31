package coil3.util

import coil3.Image
import coil3.Uri

internal actual fun println(
    level: Logger.Level,
    tag: String,
    message: String,
) {
    println(message)
}

internal actual fun isAssetUri(uri: Uri): Boolean {
    // Asset URIs are only supported on Android.
    return false
}

internal actual fun Image.prepareToDraw() {
    // Do nothing.
}
