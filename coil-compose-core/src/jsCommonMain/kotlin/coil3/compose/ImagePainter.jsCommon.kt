@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.compose

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.webcanvas.composeImageBitmapOf
import androidx.compose.ui.webcanvas.svgImageBitmapOf
import coil3.Bitmap
import coil3.BitmapImage
import coil3.ColorImage
import coil3.Image
import coil3.PlatformContext
import coil3.toBitmap
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.math.roundToInt

actual fun Image.asPainter(
    context: PlatformContext,
    filterQuality: FilterQuality,
): Painter = when (this) {
    is BitmapImage -> BitmapPainter(
        image = bitmap.asComposeImageBitmap(),
        filterQuality = filterQuality,
    )
    else -> ImagePainter(this)
}

/**
 * The canvas of Compose has no native one on the web, so an [Image] of its own draws into a canvas
 * of its own at every draw. There is no cache: an [Image] can change without telling, as an animated
 * one does.
 */
internal actual fun DrawScope.drawImage(image: Image) {
    if (image is ColorImage) {
        val width = if (image.width >= 0) image.width.toFloat() else size.width
        val height = if (image.height >= 0) image.height.toFloat() else size.height
        drawRect(Color(image.color), size = Size(width, height))
        return
    }

    val width = if (image.width > 0) image.width else size.width.roundToInt()
    val height = if (image.height > 0) image.height else size.height.roundToInt()
    if (width == 0 || height == 0) return
    drawImage(image.toBitmap(width, height).asComposeImageBitmap())
}

@OptIn(InternalComposeUiApi::class)
private fun Bitmap.asComposeImageBitmap(): ImageBitmap =
    if (isSvg) svgImageBitmapOf(source) else composeImageBitmapOf(source)
