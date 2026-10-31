@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.network

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.decodeInBrowser
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.fetchInBrowser
import coil3.network.internal.mimeTypeOf
import coil3.request.Options
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js
import okio.Buffer
import org.khronos.webgl.toInt8Array
import org.w3c.fetch.Headers

private const val HTTP_RESPONSE_UNSATISFIABLE_REQUEST = 504

/**
 * A [Fetcher] that leaves images to the browser: `fetch` hands the body to its decoder and nothing
 * of it passes through Kotlin. The disk cache is the HTTP cache of the browser.
 *
 * This is what the web uses by default. A `NetworkFetcher.Factory` added to the components, such as
 * one with an `HttpClient` of its own, goes first and works as on other platforms.
 */
internal class BrowserNetworkFetcher(
    private val url: String,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val response = fetchInBrowser(url, newRequest())
        if (!response.ok) {
            throw HttpException(NetworkResponse(code = response.status))
        }

        val result = decodeInBrowser(response.blob, mimeTypeOf(url, response.blob.type), options)
        return ImageFetchResult(
            image = result.image,
            isSampled = result.isSampled,
            dataSource = DataSource.NETWORK,
        )
    }

    private suspend fun newRequest(): JsAny {
        val headers = Headers()
        for ((name, values) in options.httpHeaders.asMap()) {
            for (value in values) {
                headers.append(name, value)
            }
        }

        return requestOf(
            method = options.httpMethod,
            headers = headers,
            body = options.httpBody?.toInt8Array(),
            cache = cacheMode(),
        )
    }

    /** The body is in Kotlin, so the browser can only get it as a copy. */
    private suspend fun NetworkRequestBody.toInt8Array(): JsAny {
        val buffer = Buffer()
        writeTo(buffer)
        return buffer.readByteArray().toInt8Array()
    }

    private fun cacheMode(): String {
        val diskRead = options.diskCachePolicy.readEnabled
        val networkRead = options.networkCachePolicy.readEnabled
        return when {
            networkRead && diskRead -> "default"
            networkRead -> if (options.diskCachePolicy.writeEnabled) "reload" else "no-store"
            diskRead -> "force-cache"
            else -> throw HttpException(NetworkResponse(code = HTTP_RESPONSE_UNSATISFIABLE_REQUEST))
        }
    }

    class Factory : Fetcher.Factory<Uri> {

        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (data.scheme != "http" && data.scheme != "https") return null
            return BrowserNetworkFetcher(data.toString(), options)
        }
    }
}

// language=js
private fun requestOf(method: String, headers: Headers, body: JsAny?, cache: String): JsAny =
    js("({ method, headers, body, cache })")
