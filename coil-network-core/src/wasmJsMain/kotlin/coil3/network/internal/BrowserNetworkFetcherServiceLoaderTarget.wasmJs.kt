package coil3.network.internal

import coil3.annotation.InternalCoilApi
import coil3.util.ServiceLoaderComponentRegistry

@Suppress("DEPRECATION")
@OptIn(ExperimentalStdlibApi::class)
@EagerInitialization
@InternalCoilApi
@Deprecated("", level = DeprecationLevel.HIDDEN)
val initBrowserNetworkFetcherHook: Any =
    ServiceLoaderComponentRegistry.register(BrowserNetworkFetcherServiceLoaderTarget())
