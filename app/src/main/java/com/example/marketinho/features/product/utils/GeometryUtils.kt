// features/product/utils/GeometryUtils.kt
package com.example.marketinho.features.product.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

object GeometryUtils {
    fun Offset.toImageCoordinates(imageSize: Size, canvasSize: Size): Offset {
        val imageRatio = imageSize.width / imageSize.height
        val canvasRatio = canvasSize.width / canvasSize.height

        val (width, height) = if (imageRatio > canvasRatio) {
            Pair(canvasSize.width, canvasSize.width / imageRatio)
        } else {
            Pair(canvasSize.height * imageRatio, canvasSize.height)
        }

        val offsetX = (canvasSize.width - width) / 2
        val offsetY = (canvasSize.height - height) / 2

        if (x < offsetX || x > offsetX + width || y < offsetY || y > offsetY + height) {
            return Offset.Zero
        }

        val scaleX = imageSize.width / width
        val scaleY = imageSize.height / height

        return Offset((x - offsetX) * scaleX, (y - offsetY) * scaleY)
    }

    fun Rect.contains(offset: Offset): Boolean {
        return offset.x >= left && offset.x <= right &&
                offset.y >= top && offset.y <= bottom
    }

    fun Rect.transformToCanvasBounds(originalSize: Size, currentCanvasSize: Size): Rect {
        val scaleX = currentCanvasSize.width / originalSize.width
        val scaleY = currentCanvasSize.height / originalSize.height
        val offsetX = (currentCanvasSize.width - originalSize.width * scaleX) / 2
        val offsetY = (currentCanvasSize.height - originalSize.height * scaleY) / 2
        return Rect(
            left * scaleX + offsetX,
            top * scaleY + offsetY,
            right * scaleX + offsetX,
            bottom * scaleY + offsetY
        )
    }
}