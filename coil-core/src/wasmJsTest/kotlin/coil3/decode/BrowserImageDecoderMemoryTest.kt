@file:OptIn(ExperimentalWasmJsInterop::class, UnsafeWasmMemoryApi::class)

package coil3.decode

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.WebAssembly
import kotlin.wasm.unsafe.wasmMemory
import okio.Buffer

class BrowserImageDecoderMemoryTest {

    @Test
    fun aLargeSourceDoesNotGrowTheLinearMemory() {
        // The first one allocates the chunk that all of them go through.
        blobOf(Buffer().apply { writeByte(1) })
        val before = byteLength(wasmMemory)

        blobOf(Buffer().apply { write(ByteArray(4 * 1024 * 1024)) })

        assertEquals(before, byteLength(wasmMemory))
    }
}

// language=js
private fun byteLength(memory: WebAssembly.Memory): Int = js("memory.buffer.byteLength")
