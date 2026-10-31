package coil3.compose

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.registerWebCanvasComposeImplementation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import coil3.Canvas as CoilCanvas
import coil3.ColorImage
import coil3.Image
import coil3.asImage
import coil3.test.utils.context
import coil3.toBitmap
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val SIZE = 20
private val RED = Color.Red.toArgb()

class ImagePainterTest {

    @OptIn(InternalComposeUiApi::class)
    @BeforeTest
    fun registerGraphics() = registerWebCanvasComposeImplementation()

    private fun pixelsOf(painter: Painter, size: Float = SIZE.toFloat()): PixelMap {
        val bitmap = ImageBitmap(SIZE, SIZE)
        val drawSize = Size(size, size)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), drawSize) {
            with(painter) { draw(drawSize) }
        }
        return bitmap.toPixelMap()
    }

    private fun assertFilled(pixels: PixelMap) {
        assertEquals(Color.Red, pixels[0, 0])
        assertEquals(Color.Red, pixels[SIZE - 1, SIZE - 1])
    }

    @Test
    fun aBitmapIsDrawnFromTheBrowser() {
        val bitmap = ColorImage(RED, width = 4, height = 4).toBitmap().asImage()
        assertFilled(pixelsOf(bitmap.asPainter(context)))
    }

    @Test
    fun aColorImageWithoutASizeFillsThePainter() {
        assertFilled(pixelsOf(ColorImage(RED).asPainter(context)))
    }

    @Test
    fun aColorImageWithASizeIsScaledToThePainter() {
        assertFilled(pixelsOf(ColorImage(RED, width = 5, height = 5).asPainter(context)))
    }

    @Test
    fun aColorImageWithASizeOnOneAxisFillsTheOther() {
        assertFilled(pixelsOf(ColorImage(RED, width = 5).asPainter(context)))
    }

    @Test
    fun anImageOfItsOwnIsDrawn() {
        assertFilled(pixelsOf(RedImage(width = 5, height = 5).asPainter(context)))
    }

    @Test
    fun anImageOfItsOwnWithoutASizeFillsThePainter() {
        assertFilled(pixelsOf(RedImage(width = -1, height = -1).asPainter(context)))
    }

    @Test
    fun anImageOfItsOwnIsSkippedWhenItRoundsToNothing() {
        pixelsOf(RedImage(width = -1, height = -1).asPainter(context), size = 0.4f)
    }
}

private class RedImage(
    override val width: Int,
    override val height: Int,
) : Image {
    override val size: Long get() = 0
    override val shareable: Boolean get() = true

    override fun draw(canvas: CoilCanvas) {
        ColorImage(RED).draw(canvas)
    }
}
