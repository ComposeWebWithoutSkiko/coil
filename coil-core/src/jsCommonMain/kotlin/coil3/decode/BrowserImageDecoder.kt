@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.decode

import coil3.Bitmap
import coil3.ImageLoader
import coil3.annotation.InternalCoilApi
import coil3.asImage
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.util.await
import coil3.util.component1
import coil3.util.component2
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlin.js.js
import okio.BufferedSource
import okio.use
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.ImageBitmap
import org.w3c.files.Blob

private const val MIME_TYPE_SVG = "image/svg+xml"

/**
 * Decodes the bytes of a fetcher that isn't the one of the browser. They are in Kotlin already, so
 * the browser can only get them as a copy.
 */
class BrowserImageDecoder(
    private val source: ImageSource,
    private val mimeType: String?,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val blob = source.source().use { blobOf(it) }
        return decodeInBrowser(blob, mimeType, options)
    }

    class Factory : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder {
            return BrowserImageDecoder(result.source, result.mimeType, options)
        }
    }
}

/** A [Blob] of all that is left in [source], which the [Blob] holds a copy of. */
internal expect fun blobOf(source: BufferedSource): Blob

/**
 * Decodes [blob] right into the size that [options] ask for. It stays with the browser.
 *
 * The type of an image isn't reliable on the web, so what isn't declared as an SVG is first loaded
 * as a raster: an `<img>` shows a raster whatever its type is, and an SVG only with its own type.
 */
@InternalCoilApi
suspend fun decodeInBrowser(
    blob: Blob,
    mimeType: String?,
    options: Options,
): DecodeResult {
    if (mimeType?.substringBefore(';') == MIME_TYPE_SVG) return decodeSvg(blob)
    val element = imageElementOf(blob).await() ?: return decodeSvg(blob)

    val srcWidth = element.naturalWidth
    val srcHeight = element.naturalHeight
    val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        targetSize = options.size,
        scale = options.scale,
        maxSize = options.maxBitmapSize,
    )
    var multiplier = DecodeUtils.computeSizeMultiplier(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        dstWidth = dstWidth,
        dstHeight = dstHeight,
        scale = options.scale,
        maxSize = options.maxBitmapSize,
    )

    // Only upscale the image if the options require an exact size.
    if (options.precision == Precision.INEXACT) {
        multiplier = multiplier.coerceAtMost(1.0)
    }

    val outWidth = (multiplier * srcWidth).toInt().coerceAtLeast(1)
    val outHeight = (multiplier * srcHeight).toInt().coerceAtLeast(1)
    val image = decodeImage(blob, outWidth, outHeight).await()

    return DecodeResult(
        image = Bitmap(image, outWidth, outHeight).asImage(),
        isSampled = outWidth < srcWidth || outHeight < srcHeight,
    )
}

private suspend fun decodeSvg(blob: Blob): DecodeResult {
    val image = checkNotNull(imageElementOf(withSvgType(blob)).await()) {
        "The browser cannot show the image."
    }
    check(!taintsCanvases(image)) {
        "An SVG that taints canvases, as one with a <foreignObject> does, can't be drawn."
    }

    val bitmap = Bitmap(image, image.naturalWidth, image.naturalHeight, isSvg = true)
    return DecodeResult(image = bitmap.asImage(), isSampled = false)
}

/** An `<img>` shows an SVG only when the blob has the type of one. */
// language=js
private fun withSvgType(blob: Blob): Blob = js("blob.slice(0, blob.size, 'image/svg+xml')")

// language=js
private fun decodeImage(blob: Blob, width: Int, height: Int): Promise<ImageBitmap> = js(
    "createImageBitmap(blob, { resizeWidth: width, resizeHeight: height, resizeQuality: 'high' })",
)

/** Whether drawing [image] takes away the right to read a canvas, which Compose relies on. */
// language=js
private fun taintsCanvases(image: HTMLImageElement): Boolean = js(
    """(() => {
        const canvas = document.createElement('canvas');
        const context = canvas.getContext('2d', { willReadFrequently: true });
        context.drawImage(image, 0, 0, 1, 1);
        try {
            context.getImageData(0, 0, 1, 1);
            return false;
        } catch (error) {
            if (error.name === 'SecurityError') return true;
            throw error;
        }
    })()""",
)

/**
 * An `<img>` that has loaded [blob], or `null` if it can't show it. Its contents stay with it after
 * its URL is revoked.
 */
// language=js
private fun imageElementOf(blob: Blob): Promise<HTMLImageElement?> = js(
    """new Promise(resolve => {
        const url = URL.createObjectURL(blob);
        const image = new Image();
        image.onload = () => {
            URL.revokeObjectURL(url);
            resolve(image);
        };
        image.onerror = () => {
            URL.revokeObjectURL(url);
            resolve(null);
        };
        image.src = url;
    })""",
)
