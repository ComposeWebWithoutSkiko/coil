package coil3.test

import coil3.Canvas
import coil3.ColorImage
import coil3.Image
import coil3.annotation.Poko

@Deprecated(
    message = "ColorImage supports the same functionality, has `color` as the first argument, " +
        "and is more easily accessible in coil-core instead of coil-test.",
    replaceWith = ReplaceWith("ColorImage", "coil3.ColorImage"),
    level = DeprecationLevel.WARNING,
)
@Poko
actual class FakeImage actual constructor(
    actual override val width: Int,
    actual override val height: Int,
    actual override val size: Long,
    actual override val shareable: Boolean,
    actual val color: Int,
) : Image {

    actual override fun draw(canvas: Canvas) {
        ColorImage(color, width, height, size, shareable).draw(canvas)
    }
}
