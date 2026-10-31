package coil3.network.internal

import coil3.Uri
import coil3.network.BrowserNetworkFetcher
import coil3.util.FetcherServiceLoaderTarget

internal class BrowserNetworkFetcherServiceLoaderTarget : FetcherServiceLoaderTarget<Uri> {
    override fun factory() = BrowserNetworkFetcher.Factory()
    override fun type() = Uri::class
}
