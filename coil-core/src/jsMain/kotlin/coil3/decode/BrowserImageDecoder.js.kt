package coil3.decode

import okio.BufferedSource
import org.w3c.files.Blob

/** On Kotlin/JS a [ByteArray] is an `Int8Array`, which the Blob copies. */
internal actual fun blobOf(source: BufferedSource): Blob = blobOfBytes(source.readByteArray())

// language=js
private fun blobOfBytes(bytes: ByteArray): Blob = js("new Blob([bytes])")
