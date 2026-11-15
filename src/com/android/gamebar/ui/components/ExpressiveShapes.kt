/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.gamebar.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MorphShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = morph.toPath(progress = progress, path = Path())
        val matrix = Matrix().apply {
            scale(size.width, size.height)
        }
        path.transform(matrix)
        val center = Offset(size.width / 2f, size.height / 2f)
        val pathCenter = path.getBounds().center
        path.translate(center - pathCenter)
        return Outline.Generic(path)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object GameBarExpressiveShapes {
    fun polygonForTab(route: String): RoundedPolygon = when (route) {
        "home" -> MaterialShapes.Sunny
        "features" -> MaterialShapes.Cookie9Sided
        "customization" -> MaterialShapes.Clover4Leaf
        "records" -> MaterialShapes.Pill
        "presets" -> MaterialShapes.SoftBurst
        else -> MaterialShapes.Sunny
    }

    fun polygonForTab(page: Int): RoundedPolygon = when (page) {
        0 -> MaterialShapes.Sunny
        1 -> MaterialShapes.Cookie9Sided
        2 -> MaterialShapes.Clover4Leaf
        3 -> MaterialShapes.Pill
        4 -> MaterialShapes.SoftBurst
        else -> MaterialShapes.Sunny
    }

    fun polygonForHomeMenu(index: Int): RoundedPolygon = when (index % 4) {
        0 -> MaterialShapes.Sunny
        1 -> MaterialShapes.Cookie9Sided
        2 -> MaterialShapes.Clover4Leaf
        else -> MaterialShapes.SoftBurst
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Modifier.squishable(
    enabled: Boolean = true,
    scaleDown: Float = 0.94f,
): Modifier {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "squish_scale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (enabled) {
                try {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        try {
                            waitForUpOrCancellation()
                        } finally {
                            isPressed = false
                        }
                    }
                } finally {
                    isPressed = false
                }
            }
        }
}
