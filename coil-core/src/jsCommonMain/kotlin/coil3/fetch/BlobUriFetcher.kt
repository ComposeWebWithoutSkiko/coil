@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.fetch

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.decodeInBrowser
import coil3.request.Options
import kotlin.js.ExperimentalWasmJsInterop

/**
 * Fetches images from blob URLs created by `URL.createObjectURL` on the web
 * (e.g. "blob:http://localhost:8081/a3953f30-e6ef-47b6-8750-731ce8afe0f8").
 */
internal class BlobUriFetcher(
    private val uri: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val blob = fetchInBrowser(uri.toString()).blob
        val result = decodeInBrowser(blob, blob.type, options)
        return ImageFetchResult(
            image = result.image,
            isSampled = result.isSampled,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (!data.toString().startsWith("blob:")) return null
            return BlobUriFetcher(data, options)
        }
    }
}
