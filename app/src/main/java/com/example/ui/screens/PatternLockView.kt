package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun PatternLockView(
    onPatternComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDots by remember { mutableStateOf(listOf<Int>()) }
    var currentTouch by remember { mutableStateOf<Offset?>(null) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    Box(
        modifier = modifier
            .size(300.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val dot = getTouchedDot(offset, size.width.toFloat(), size.height.toFloat())
                        if (dot != null && !selectedDots.contains(dot)) {
                            selectedDots = listOf(dot)
                        }
                    },
                    onDrag = { change, _ ->
                        currentTouch = change.position
                        val dot = getTouchedDot(change.position, size.width.toFloat(), size.height.toFloat())
                        if (dot != null && !selectedDots.contains(dot)) {
                            selectedDots = selectedDots + dot
                        }
                    },
                    onDragEnd = {
                        currentTouch = null
                        if (selectedDots.isNotEmpty()) {
                            onPatternComplete(selectedDots.joinToString(","))
                            selectedDots = emptyList()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 3f
            val cy = h / 3f

            val centers = listOf(
                Offset(cx * 0.5f, cy * 0.5f),
                Offset(cx * 1.5f, cy * 0.5f),
                Offset(cx * 2.5f, cy * 0.5f),
                Offset(cx * 0.5f, cy * 1.5f),
                Offset(cx * 1.5f, cy * 1.5f),
                Offset(cx * 2.5f, cy * 1.5f),
                Offset(cx * 0.5f, cy * 2.5f),
                Offset(cx * 1.5f, cy * 2.5f),
                Offset(cx * 2.5f, cy * 2.5f)
            )

            // Draw connecting lines
            for (i in 0 until selectedDots.size - 1) {
                val start = centers[selectedDots[i]]
                val end = centers[selectedDots[i + 1]]
                drawLine(
                    color = primaryColor,
                    start = start,
                    end = end,
                    strokeWidth = 8f
                )
            }

            // Draw active drag line
            if (selectedDots.isNotEmpty() && currentTouch != null) {
                val lastCenter = centers[selectedDots.last()]
                drawLine(
                    color = primaryColor.copy(alpha = 0.7f),
                    start = lastCenter,
                    end = currentTouch!!,
                    strokeWidth = 6f
                )
            }

            // Draw dots
            for (i in centers.indices) {
                val center = centers[i]
                val isSelected = selectedDots.contains(i)
                drawCircle(
                    color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.4f),
                    radius = if (isSelected) 28f else 18f,
                    center = center
                )
                if (isSelected) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.2f),
                        radius = 45f,
                        center = center
                    )
                }
            }
        }
    }
}

private fun getTouchedDot(offset: Offset, width: Float, height: Float): Int? {
    val cx = width / 3f
    val cy = height / 3f
    val centers = listOf(
        Offset(cx * 0.5f, cy * 0.5f),
        Offset(cx * 1.5f, cy * 0.5f),
        Offset(cx * 2.5f, cy * 0.5f),
        Offset(cx * 0.5f, cy * 1.5f),
        Offset(cx * 1.5f, cy * 1.5f),
        Offset(cx * 2.5f, cy * 1.5f),
        Offset(cx * 0.5f, cy * 2.5f),
        Offset(cx * 1.5f, cy * 2.5f),
        Offset(cx * 2.5f, cy * 2.5f)
    )

    for (i in centers.indices) {
        val dist = (centers[i] - offset).getDistance()
        if (dist < 60f) {
            return i
        }
    }
    return null
}
