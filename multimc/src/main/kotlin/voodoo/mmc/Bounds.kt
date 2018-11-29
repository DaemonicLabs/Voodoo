package voodoo.mmc

import kotlinx.serialization.Serializable
import java.awt.Rectangle

@Serializable
data class Bounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    constructor(rectangle: Rectangle) : this(rectangle.x, rectangle.y, rectangle.width, rectangle.height)
}