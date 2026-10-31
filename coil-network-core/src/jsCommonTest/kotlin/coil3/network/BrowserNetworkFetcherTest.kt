@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.network

import coil3.BitmapImage
import coil3.Extras
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.fetch.ImageFetchResult
import coil3.network.internal.BrowserNetworkFetcherServiceLoaderTarget
import coil3.request.CachePolicy
import coil3.request.Options
import coil3.size.Size
import coil3.test.utils.context
import coil3.test.utils.pngOf
import coil3.toUri
import coil3.util.ServiceLoaderComponentRegistry
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.encodeUtf8
import org.w3c.files.Blob

private const val URL = "https://example.com/image.png"

class BrowserNetworkFetcherTest {
    private val options = Options(context, size = Size.ORIGINAL)

    @AfterTest
    fun restoreFetch() = restoreBrowserFetch()

    private fun fetcher(options: Options = this.options) = BrowserNetworkFetcher(URL, options)

    @Test
    fun handlesHttpAndHttps() {
        val factory = BrowserNetworkFetcher.Factory()
        val loader = ImageLoader(context)
        assertNotNull(factory.create("http://example.com/a.png".toUri(), options, loader))
        assertNotNull(factory.create("https://example.com/a.png".toUri(), options, loader))
        assertNull(factory.create("file:///a.png".toUri(), options, loader))
        assertNull(factory.create("blob:null/a".toUri(), options, loader))
    }

    @Test
    fun isRegisteredByDefault() {
        assertTrue(
            ServiceLoaderComponentRegistry.fetchers.any {
                it is BrowserNetworkFetcherServiceLoaderTarget
            },
        )
    }

    @Test
    fun decodesTheResponseWithoutADecoder() = runTest {
        stubFetch(pngOf(4, 2), status = 200)

        val result = assertIs<ImageFetchResult>(fetcher().fetch())

        val image = assertIs<BitmapImage>(result.image)
        assertEquals(4, image.width)
        assertEquals(2, image.height)
        assertEquals(DataSource.NETWORK, result.dataSource)
    }

    @Test
    fun aFailedResponseIsAnHttpException() = runTest {
        stubFetch(pngOf(4, 2), status = 404)

        val exception = assertFailsWith<HttpException> { fetcher().fetch() }

        assertEquals(404, exception.response.code)
    }

    @Test
    fun sendsTheMethodTheHeadersAndTheBody() = runTest {
        stubFetch(pngOf(4, 2), status = 200)
        val headers = NetworkHeaders.Builder()
            .add("X-First", "1")
            .add("X-First", "2")
            .add("X-Second", "3")
            .build()
        val extras = options.extras.newBuilder()
            .set(Extras.Key.httpMethod, "POST")
            .set(Extras.Key.httpHeaders, headers)
            .set(Extras.Key.httpBody, NetworkRequestBody("body".encodeUtf8()))
            .build()

        fetcher(options.copy(extras = extras)).fetch()

        assertEquals("POST", requestMethod())
        assertEquals("1, 2", requestHeader("X-First"))
        assertEquals("3", requestHeader("X-Second"))
        assertEquals(4, requestBodySize())
    }

    @Test
    fun aRequestWithoutABodySendsNone() = runTest {
        stubFetch(pngOf(4, 2), status = 200)

        fetcher().fetch()

        assertEquals("GET", requestMethod())
        assertTrue(requestHasNoBody())
    }

    @Test
    fun theCachePoliciesAreTheCacheModesOfFetch() = runTest {
        stubFetch(pngOf(4, 2), status = 200)

        suspend fun cacheModeFor(disk: CachePolicy, network: CachePolicy): String {
            fetcher(options.copy(diskCachePolicy = disk, networkCachePolicy = network)).fetch()
            return requestCache()
        }

        assertEquals("default", cacheModeFor(CachePolicy.ENABLED, CachePolicy.ENABLED))
        assertEquals("reload", cacheModeFor(CachePolicy.WRITE_ONLY, CachePolicy.ENABLED))
        assertEquals("no-store", cacheModeFor(CachePolicy.DISABLED, CachePolicy.ENABLED))
        assertEquals("force-cache", cacheModeFor(CachePolicy.ENABLED, CachePolicy.DISABLED))
    }

    @Test
    fun readingFromNeitherTheCacheNorTheNetworkFails() = runTest {
        stubFetch(pngOf(4, 2), status = 200)
        val options = options.copy(
            diskCachePolicy = CachePolicy.DISABLED,
            networkCachePolicy = CachePolicy.DISABLED,
        )

        val exception = assertFailsWith<HttpException> { fetcher(options).fetch() }

        assertEquals(504, exception.response.code)
    }

    @Test
    fun cancellingAbortsTheRequest() = runTest {
        stubPendingFetch()

        val job = launch(start = CoroutineStart.UNDISPATCHED) { fetcher().fetch() }
        job.cancelAndJoin()

        assertTrue(requestWasAborted())
    }
}

// language=js
private fun stubFetch(blob: Blob, status: Int) {
    js(
        """
        globalThis.browserFetch = globalThis.browserFetch || globalThis.fetch;
        globalThis.fetch = (url, init) => {
            globalThis.lastFetch = init;
            return Promise.resolve(new Response(blob, { status }));
        };
        """,
    )
}

// language=js
private fun stubPendingFetch() {
    js(
        """
        globalThis.browserFetch = globalThis.browserFetch || globalThis.fetch;
        globalThis.fetch = (url, init) => {
            globalThis.lastFetch = init;
            return new Promise(() => {});
        };
        """,
    )
}

// language=js
private fun restoreBrowserFetch() {
    js("if (globalThis.browserFetch) globalThis.fetch = globalThis.browserFetch")
}

// language=js
private fun requestMethod(): String = js("globalThis.lastFetch.method")

// language=js
private fun requestHeader(name: String): String? = js("globalThis.lastFetch.headers.get(name)")

// language=js
private fun requestBodySize(): Int = js("globalThis.lastFetch.body.length")

// language=js
private fun requestHasNoBody(): Boolean = js("globalThis.lastFetch.body == null")

// language=js
private fun requestCache(): String = js("globalThis.lastFetch.cache")

// language=js
private fun requestWasAborted(): Boolean = js("globalThis.lastFetch.signal.aborted")
