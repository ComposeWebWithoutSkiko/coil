@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.test.utils

import coil3.Bitmap
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get

actual fun Bitmap.getPixels(): ByteArray {
    val pixels = pixelsOf(source, width, height)
    return ByteArray(pixels.length) { pixels[it] }
}

// language=js
private fun pixelsOf(source: JsAny, width: Int, height: Int): Int8Array = js(
    """(() => {
        const context = new OffscreenCanvas(width, height).getContext('2d');
        context.drawImage(source, 0, 0);
        return new Int8Array(context.getImageData(0, 0, width, height).data.buffer);
    })()""",
)
