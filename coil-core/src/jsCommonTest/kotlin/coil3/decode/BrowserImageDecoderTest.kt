@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.decode

import coil3.Bitmap
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.test.utils.context
import coil3.test.utils.getPixels
import coil3.test.utils.pngOf
import coil3.util.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.Buffer
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.files.Blob

class BrowserImageDecoderTest {

    private fun options(
        size: Size,
        scale: Scale = Scale.FIT,
        precision: Precision = Precision.INEXACT,
    ) = Options(context, size = size, scale = scale, precision = precision)

    private fun assertBitmap(result: DecodeResult, width: Int, height: Int) {
        val image = assertIs<BitmapImage>(result.image)
        assertEquals(width, image.width)
        assertEquals(height, image.height)
        // What the browser decoded, as opposed to what Kotlin computed.
        assertEquals(width, widthOf(image.bitmap.source))
        assertEquals(height, heightOf(image.bitmap.source))
    }

    @Test
    fun keepsTheSizeOfTheImageForTheOriginalSize() = runTest {
        val result = decodeInBrowser(pngOf(40, 20), "image/png", options(Size.ORIGINAL))
        assertBitmap(result, 40, 20)
        assertFalse(result.isSampled)
    }

    @Test
    fun decodesIntoTheTargetSizeWhenFitting() = runTest {
        val result = decodeInBrowser(pngOf(40, 20), "image/png", options(Size(10, 10)))
        assertBitmap(result, 10, 5)
        assertTrue(result.isSampled)
    }

    @Test
    fun decodesIntoTheTargetSizeWhenFilling() = runTest {
        val options = options(Size(10, 10), scale = Scale.FILL)
        val result = decodeInBrowser(pngOf(40, 20), "image/png", options)
        assertBitmap(result, 20, 10)
        assertTrue(result.isSampled)
    }

    @Test
    fun onlyUpscalesWhenTheSizeMustBeExact() = runTest {
        val blob = pngOf(40, 20)
        val inexact = decodeInBrowser(blob, "image/png", options(Size(100, 100)))
        assertBitmap(inexact, 40, 20)
        assertFalse(inexact.isSampled)

        val exact = options(Size(100, 100), precision = Precision.EXACT)
        assertBitmap(decodeInBrowser(blob, "image/png", exact), 100, 50)
    }

    @Test
    fun aVeryThinImageKeepsAPixel() = runTest {
        val result = decodeInBrowser(pngOf(400, 1), "image/png", options(Size(10, 10)))
        assertBitmap(result, 10, 1)
    }

    @Test
    fun keepsAnSvgAsAVector() = runTest {
        val result = decodeInBrowser(svgOf("image/svg+xml"), "image/svg+xml", options(Size(10, 10)))
        val image = assertIs<BitmapImage>(result.image)
        assertTrue(image.bitmap.isSvg)
        assertEquals(30, image.width)
        assertEquals(15, image.height)
        assertFalse(result.isSampled)
    }

    @Test
    fun anSvgIsShownWhateverTypeItsBlobHas() = runTest {
        val result = decodeInBrowser(svgOf("text/plain"), "image/svg+xml", options(Size.ORIGINAL))
        assertTrue(assertIs<BitmapImage>(result.image).bitmap.isSvg)
    }

    @Test
    fun anSvgIsFoundWhateverTypeItIsDeclaredWith() = runTest {
        val types = listOf(
            "image/svg+xml;charset=utf-8",
            null,
            "",
            "application/octet-stream",
            "text/xml",
            "image/svg",
            "image/png",
        )
        for (type in types) {
            val result = decodeInBrowser(svgOf(""), type, options(Size.ORIGINAL))
            assertTrue(assertIs<BitmapImage>(result.image).bitmap.isSvg, "declared as $type")
        }
    }

    @Test
    fun anSvgIsFoundFarIntoItsFile() = runTest {
        val result = decodeInBrowser(svgAfterAComment(), null, options(Size.ORIGINAL))
        assertTrue(assertIs<BitmapImage>(result.image).bitmap.isSvg)
    }

    @Test
    fun aRasterNeedsNoDeclaredType() = runTest {
        for (type in listOf(null, "", "application/octet-stream")) {
            assertBitmap(decodeInBrowser(pngOf(40, 20), type, options(Size.ORIGINAL)), 40, 20)
        }
    }

    @Test
    fun anSvgThatTaintsCanvasesIsAnErrorResult() = runTest {
        val url = urlOf(svgOfForeignContent())
        try {
            val request = ImageRequest.Builder(context).data(url).size(Size.ORIGINAL).build()
            assertIs<ErrorResult>(ImageLoader(context).execute(request))
        } finally {
            revokeUrl(url)
        }
    }

    @Test
    fun followsTheOrientationOfAPhoto() = runTest {
        val photo = rotatedJpeg().await()

        val original = decodeInBrowser(photo, "image/jpeg", options(Size.ORIGINAL))
        // The sizes are swapped from the 40 x 20 of the encoded image.
        assertBitmap(original, 20, 40)
        assertTrue(assertIs<BitmapImage>(original.image).bitmap.isRedAt(10, 5))
        assertFalse(assertIs<BitmapImage>(original.image).bitmap.isRedAt(10, 35))

        val smaller = decodeInBrowser(photo, "image/jpeg", options(Size(10, 10)))
        assertBitmap(smaller, 5, 10)
        assertTrue(assertIs<BitmapImage>(smaller.image).bitmap.isRedAt(2, 2))
        assertFalse(assertIs<BitmapImage>(smaller.image).bitmap.isRedAt(2, 8))
    }

    private fun Bitmap.isRedAt(x: Int, y: Int): Boolean {
        val pixels = getPixels()
        val at = 4 * (y * width + x)
        return (pixels[at].toInt() and 0xFF) > (pixels[at + 2].toInt() and 0xFF)
    }

    @Test
    fun failsForWhatIsNotAnImage() = runTest {
        val result = runCatching {
            decodeInBrowser(textBlob(), "image/png", options(Size.ORIGINAL))
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun aSourceBecomesABlobOfTheSameBytes() = runTest {
        val chunk = 256 * 1024
        for (size in listOf(0, 1, chunk, chunk + 1, 2 * chunk + 1)) {
            val bytes = ByteArray(size) { it.toByte() }
            val blob = blobOf(Buffer().apply { write(bytes) })

            val array = Int8Array(arrayBufferOf(blob).await())
            assertEquals(size, array.length)
            assertTrue((0 until size).all { array[it] == bytes[it] }, "$size bytes")
        }
    }

    @Test
    fun decodesBytesThatAreInKotlin() = runTest {
        val blob = pngOf(40, 20)
        val bytes = Int8Array(arrayBufferOf(blob).await()).let { array ->
            ByteArray(array.length) { array[it] }
        }
        val options = options(Size(10, 10))
        val source = ImageSource(Buffer().apply { write(bytes) }, options.fileSystem)

        assertBitmap(BrowserImageDecoder(source, "image/png", options).decode(), 10, 5)
    }
}

// language=js
private fun svgOf(type: String): Blob = js(
    """new Blob(
        ['<svg xmlns="http://www.w3.org/2000/svg" width="30" height="15"></svg>'],
        { type }
    )""",
)

// language=js
private fun svgAfterAComment(): Blob = js(
    """new Blob([
        '<!--' + ' '.repeat(2048) + '-->',
        '<svg xmlns="http://www.w3.org/2000/svg" width="30" height="15"></svg>'
    ])""",
)

// language=js
private fun svgOfForeignContent(): Blob = js(
    """new Blob([
        '<svg xmlns="http://www.w3.org/2000/svg" width="30" height="15">',
        '<foreignObject width="30" height="15">',
        '<div xmlns="http://www.w3.org/1999/xhtml">text</div>',
        '</foreignObject></svg>'
    ], { type: 'image/svg+xml' })""",
)

/**
 * A JPEG of 40 x 20, red on the left and blue on the right, with the EXIF orientation 6: shown
 * turned 90 degrees clockwise, which puts the red on top.
 */
// language=js
private fun rotatedJpeg(): Promise<Blob> = js(
    """(() => {
        const canvas = new OffscreenCanvas(40, 20);
        const context = canvas.getContext('2d');
        context.fillStyle = '#f00';
        context.fillRect(0, 0, 20, 20);
        context.fillStyle = '#00f';
        context.fillRect(20, 0, 20, 20);
        const exif = new Uint8Array([
            0xFF, 0xE1, 0x00, 0x22,
            0x45, 0x78, 0x69, 0x66, 0x00, 0x00,
            0x49, 0x49, 0x2A, 0x00, 0x08, 0x00, 0x00, 0x00,
            0x01, 0x00,
            0x12, 0x01, 0x03, 0x00, 0x01, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        ]);
        return canvas.convertToBlob({ type: 'image/jpeg', quality: 1 }).then(jpeg =>
            new Blob([jpeg.slice(0, 2), exif, jpeg.slice(2)], { type: 'image/jpeg' })
        );
    })()""",
)

// language=js
private fun urlOf(blob: Blob): String = js("URL.createObjectURL(blob)")

// language=js
private fun revokeUrl(url: String) {
    js("URL.revokeObjectURL(url)")
}

// language=js
private fun textBlob(): Blob = js("new Blob(['not an image'])")

// language=js
private fun arrayBufferOf(blob: Blob): Promise<ArrayBuffer> = js("blob.arrayBuffer()")

// language=js
private fun widthOf(source: JsAny): Int = js("source.width")

// language=js
private fun heightOf(source: JsAny): Int = js("source.height")
