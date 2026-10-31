@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.fetch

import coil3.annotation.InternalCoilApi
import coil3.util.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.js
import org.w3c.files.Blob

/** A finished download, as the browser holds it. */
@InternalCoilApi
external interface BrowserResponse : JsAny {
    val status: Int
    val ok: Boolean
    val blob: Blob
}

/** Fetches [url] with `fetch`. Cancelling aborts the request, also while the body is on its way. */
@InternalCoilApi
suspend fun fetchInBrowser(
    url: String,
    init: JsAny? = null,
): BrowserResponse {
    val controller = AbortController()
    try {
        return download(url, init, controller.signal).await()
    } finally {
        controller.abort()
    }
}

private external interface AbortSignal : JsAny

private external class AbortController {
    val signal: AbortSignal
    fun abort()
}

/**
 * The Kotlin/JS printer drops the parentheses of an arrow function that returns an object, which
 * breaks it, so the inner callback is a plain function.
 */
// language=js
private fun download(url: String, init: JsAny?, signal: AbortSignal): Promise<BrowserResponse> = js(
    """fetch(url, Object.assign({ signal }, init)).then(response =>
        response.blob().then(function (blob) {
            return { status: response.status, ok: response.ok, blob: blob };
        })
    )""",
)
