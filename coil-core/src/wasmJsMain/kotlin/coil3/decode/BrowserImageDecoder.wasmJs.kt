@file:OptIn(ExperimentalWasmJsInterop::class, UnsafeWasmMemoryApi::class)

package coil3.decode

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.WebAssembly
import kotlin.wasm.unsafe.wasmMemory
import kotlin.wasm.unsafe.withScopedMemoryAllocator
import okio.BufferedSource
import org.w3c.files.Blob

/**
 * The size of the chunk of linear memory the bytes go through: a transfer grows the memory, which
 * never shrinks, by this at most. The same as in Compose, which measured it.
 */
private const val CHUNK_SIZE = 256 * 1024

/**
 * JS can't read the garbage-collected heap, so the bytes go from [source] to a chunk of linear
 * memory, which JS views, and from there to a Blob. A Blob made of Blobs refers to their data.
 */
internal actual fun blobOf(source: BufferedSource): Blob {
    val parts = newArray()
    withScopedMemoryAllocator { allocator ->
        val start = allocator.allocate(CHUNK_SIZE)
        while (!source.exhausted()) {
            var length = 0
            while (length < CHUNK_SIZE && !source.exhausted()) {
                (start + length++).storeByte(source.readByte())
            }
            addBlobOf(parts, uint8View(wasmMemory, start.address.toInt(), length))
        }
    }
    return blobOfParts(parts)
}

// language=js
private fun uint8View(memory: WebAssembly.Memory, offset: Int, length: Int): JsAny =
    js("new Uint8Array(memory.buffer, offset, length)")

// language=js
private fun newArray(): JsAny = js("[]")

// language=js
private fun addBlobOf(parts: JsAny, bytes: JsAny) {
    js("parts.push(new Blob([bytes]))")
}

// language=js
private fun blobOfParts(parts: JsAny): Blob = js("new Blob(parts)")
