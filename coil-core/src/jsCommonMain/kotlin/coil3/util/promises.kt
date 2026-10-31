@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.util

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.asJsException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Promise.await isn't available in the source sets shared by js and wasmJs yet. */
internal suspend fun <T : JsAny?> Promise<T>.await(): T = suspendCancellableCoroutine { continuation ->
    then(
        onFulfilled = { continuation.resume(it); null },
        onRejected = { continuation.resumeWithException(it.asJsException()); null },
    )
}
