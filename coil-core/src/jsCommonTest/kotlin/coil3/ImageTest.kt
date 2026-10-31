@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3

import coil3.decode.decodeInBrowser
import coil3.request.Options
import coil3.size.Size
import coil3.test.utils.context
import coil3.test.utils.pngOf
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest

private const val SEMI_TRANSPARENT_RED = 0x80FF0000.toInt()

class ImageTest {

    @Test
    fun aColorImageHasTheColorItWasGivenInAllItsChannels() {
        val bitmap = ColorImage(SEMI_TRANSPARENT_RED, width = 2, height = 2).toBitmap()
        assertEquals(255, channel(bitmap.source, 0))
        assertEquals(0, channel(bitmap.source, 1))
        assertEquals(0, channel(bitmap.source, 2))
        assertEquals(128, channel(bitmap.source, 3))
    }

    @Test
    fun aColorImageWithoutASizeFillsTheBitmap() {
        val bitmap = ColorImage(SEMI_TRANSPARENT_RED).toBitmap(2, 2)
        assertEquals(128, channel(bitmap.source, 3))
    }

    @Test
    fun anImageWithoutASizeNeedsOneForABitmap() {
        assertFailsWith<IllegalArgumentException> { ColorImage(SEMI_TRANSPARENT_RED).toBitmap() }
    }

    @Test
    fun aBitmapOfTheSameSizeIsItself() = runTest {
        val image = decoded(4, 2)
        assertSame(image.bitmap, image.toBitmap())
    }

    @Test
    fun anImageIsDrawnIntoABitmapOfAnotherSize() = runTest {
        val bitmap = decoded(4, 2).toBitmap(8, 6)
        assertEquals(8, bitmap.width)
        assertEquals(6, bitmap.height)
        assertEquals(8, widthOf(bitmap.source))
    }

    private suspend fun decoded(width: Int, height: Int): BitmapImage {
        val options = Options(context, size = Size.ORIGINAL)
        val result = decodeInBrowser(pngOf(width, height), "image/png", options)
        return result.image as BitmapImage
    }
}

/** A channel of the first pixel, by the 2D canvas it gets drawn into. */
// language=js
private fun channel(source: JsAny, index: Int): Int = js(
    """(() => {
        const context = new OffscreenCanvas(1, 1).getContext('2d');
        context.drawImage(source, 0, 0);
        return context.getImageData(0, 0, 1, 1).data[index];
    })()""",
)

// language=js
private fun widthOf(source: JsAny): Int = js("source.width")
