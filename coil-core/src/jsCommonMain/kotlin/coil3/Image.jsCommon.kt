@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3

import coil3.annotation.Poko
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

/**
 * An image that the browser holds: an `ImageBitmap`, a `<canvas>`, or an `<img>` with an SVG, which
 * stays a vector.
 */
actual class Bitmap(
    val source: JsAny,
    val width: Int,
    val height: Int,
    val isSvg: Boolean = false,
)

actual class Canvas(val context: CanvasRenderingContext2D)

actual fun Bitmap.asImage(shareable: Boolean): BitmapImage {
    return BitmapImage(this, shareable)
}

actual fun Image.toBitmap(
    width: Int,
    height: Int,
): Bitmap {
    if (this is BitmapImage && bitmap.width == width && bitmap.height == height) {
        return bitmap
    }

    require(width > 0 && height > 0) { "A bitmap of $width x $height can't be drawn." }
    val canvas = canvasOf(width, height)
    draw(Canvas(contextOf(canvas)))
    return Bitmap(canvas, width, height)
}

@Poko
actual class BitmapImage internal constructor(
    actual val bitmap: Bitmap,
    actual override val shareable: Boolean,
) : Image {

    actual override val size: Long
        get() = 4L * bitmap.width * bitmap.height

    actual override val width: Int
        get() = bitmap.width

    actual override val height: Int
        get() = bitmap.height

    actual override fun draw(canvas: Canvas) {
        drawImage(canvas.context, bitmap.source)
    }
}

// language=js
private fun canvasOf(width: Int, height: Int): HTMLCanvasElement =
    js("Object.assign(document.createElement('canvas'), { width: width, height: height })")

// language=js
private fun contextOf(canvas: HTMLCanvasElement): CanvasRenderingContext2D =
    js("canvas.getContext('2d')")

// language=js
private fun drawImage(context: CanvasRenderingContext2D, source: JsAny) {
    js("context.drawImage(source, 0, 0)")
}
