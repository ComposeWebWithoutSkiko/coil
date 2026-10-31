@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.fetch

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.request.Options
import coil3.size.Size
import coil3.test.utils.context
import coil3.test.utils.pngOf
import coil3.toUri
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.w3c.files.Blob

class BlobUriFetcherTest {
    private val factory = BlobUriFetcher.Factory()

    @Test
    fun handlesBlobUriWithHttpOrigin() {
        val uri = "blob:http://localhost:8081/a3953f30-e6ef-47b6-8750-731ce8afe0f8".toUri()
        assertNotNull(factory.create(uri, Options(context), ImageLoader(context)))
    }

    @Test
    fun handlesBlobUriWithOpaqueOrigin() {
        val uri = "blob:null/a3953f30-e6ef-47b6-8750-731ce8afe0f8".toUri()
        assertNotNull(factory.create(uri, Options(context), ImageLoader(context)))
    }

    @Test
    fun ignoresNonBlobUri() {
        val uri = "https://www.example.com/image.jpg".toUri()
        assertNull(factory.create(uri, Options(context), ImageLoader(context)))
    }

    @Test
    fun decodesTheBlobInTheBrowser() = runTest {
        val url = createBlobUrl(pngOf(4, 2))
        try {
            val options = Options(context, size = Size.ORIGINAL)
            val fetcher = assertNotNull(
                factory.create(url.toUri(), options, ImageLoader(context)),
            )
            val result = assertIs<ImageFetchResult>(fetcher.fetch())
            assertEquals(4, result.image.width)
            assertEquals(2, result.image.height)
            assertFalse(result.isSampled)
            assertEquals(DataSource.MEMORY, result.dataSource)
        } finally {
            revokeBlobUrl(url)
        }
    }
}

// language=js
private fun createBlobUrl(blob: Blob): String = js("URL.createObjectURL(blob)")

// language=js
private fun revokeBlobUrl(url: String) {
    js("URL.revokeObjectURL(url)")
}
