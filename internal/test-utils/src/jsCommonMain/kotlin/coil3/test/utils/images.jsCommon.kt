@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.test.utils

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlin.js.asJsException
import kotlin.js.js
import org.w3c.files.Blob

/** A PNG of the given size, as the browser holds it. */
suspend fun pngOf(width: Int, height: Int): Blob = suspendCoroutine { continuation ->
    encodePng(width, height).then(
        onFulfilled = { continuation.resume(it); null },
        onRejected = { continuation.resumeWithException(it.asJsException()); null },
    )
}

// language=js
private fun encodePng(width: Int, height: Int): Promise<Blob> = js(
    """(() => {
        const canvas = new OffscreenCanvas(width, height);
        canvas.getContext('2d');
        return canvas.convertToBlob();
    })()""",
)
