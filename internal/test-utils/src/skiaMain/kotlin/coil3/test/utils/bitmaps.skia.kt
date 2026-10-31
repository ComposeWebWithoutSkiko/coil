package coil3.test.utils

import coil3.Bitmap

actual fun Bitmap.getPixels(): ByteArray = readPixels() ?: byteArrayOf()
