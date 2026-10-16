package com.swordfish.lemuroid.app.mobile.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

/** PSP XMB-style flowing wave background. */
@Composable
fun XmbBackground(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "xmb-wave")
    val phase by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 24_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "xmb-wave-phase",
        )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        drawRect(
            brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            Color(0xFF060B23),
                            Color(0xFF10254E),
                            Color(0xFF050A1C),
                        ),
                    startY = 0f,
                    endY = height,
                ),
            size = size,
        )

        // Soft translucent filled bands.
        for (i in 0..1) {
            val bandAlpha = if (i == 0) 0.05f else 0.035f
            val bandPhase = phase * (1f + i * 0.4f) + i * 2.4f
            val path = Path()
            val bottom = Path()
            var first = true
            var x = 0f
            while (x <= width) {
                val y =
                    height * (0.52f + i * 0.18f) +
                        height * 0.10f * sin(x / width * 3.4f * PI.toFloat() + bandPhase) +
                        height * 0.04f * sin(x / width * 7.2f * PI.toFloat() - bandPhase * 1.6f)
                if (first) {
                    path.moveTo(x, y)
                    bottom.moveTo(x, y + height * 0.10f + i * 40f)
                    first = false
                } else {
                    path.lineTo(x, y)
                    bottom.lineTo(x, y + height * 0.10f + i * 40f)
                }
                x += 8f
            }
            path.lineTo(width, height)
            path.lineTo(0f, height)
            path.close()
            bottom.lineTo(width, height)
            bottom.lineTo(0f, height)
            bottom.close()
            drawPath(path, Color.White.copy(alpha = bandAlpha))
            drawPath(bottom, Color(0xFF3E7BFF).copy(alpha = bandAlpha))
        }

        // Thin flowing strokes on top.
        for (i in 0..2) {
            val strokePhase = phase * (0.7f + i * 0.35f) + i * 1.9f
            val path = Path()
            var first = true
            var x = 0f
            while (x <= width) {
                val y =
                    height * (0.42f + i * 0.17f) +
                        height * 0.12f * sin(x / width * 3.0f * PI.toFloat() + strokePhase) +
                        height * 0.05f * sin(x / width * 8.0f * PI.toFloat() - strokePhase * 1.3f)
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += 8f
            }
            drawPath(
                path,
                Color.White.copy(alpha = 0.10f + i * 0.05f),
                style = Stroke(width = 1.5f + i * 1.5f, cap = StrokeCap.Round),
            )
        }
    }
}
