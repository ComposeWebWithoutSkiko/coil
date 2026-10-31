@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3

import coil3.annotation.Poko
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js
import org.w3c.dom.CanvasRenderingContext2D

@Poko
actual class ColorImage actual constructor(
    actual val color: Int,
    actual override val width: Int,
    actual override val height: Int,
    actual override val size: Long,
    actual override val shareable: Boolean,
) : Image {

    actual override fun draw(canvas: Canvas) {
        val alpha = (color ushr 24) / 255.0
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        val cssColor = "rgb($red $green $blue / $alpha)"
        if (width >= 0 && height >= 0) {
            fillRect(canvas.context, cssColor, width, height)
        } else {
            fillCanvas(canvas.context, cssColor)
        }
    }
}

// language=js
private fun fillRect(context: CanvasRenderingContext2D, color: String, width: Int, height: Int) {
    js("context.fillStyle = color; context.fillRect(0, 0, width, height)")
}

// language=js
private fun fillCanvas(context: CanvasRenderingContext2D, color: String) {
    js(
        """context.fillStyle = color;
        context.fillRect(0, 0, context.canvas.width, context.canvas.height)""",
    )
}
