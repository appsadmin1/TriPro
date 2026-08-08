package com.tripro.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun ColorPickerWheel(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var hsv by remember {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(AndroidColor.argb(
            (initialColor.alpha * 255).toInt(),
            (initialColor.red * 255).toInt(),
            (initialColor.green * 255).toInt(),
            (initialColor.blue * 255).toInt()
        ), hsv)
        mutableStateOf(hsv)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newHsv = calculateHsvFromOffset(offset, size.width.toFloat(), hsv[2])
                        if (newHsv != null) {
                            hsv = newHsv
                            onColorChanged(hsvToColor(hsv))
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val newHsv = calculateHsvFromOffset(change.position, size.width.toFloat(), hsv[2])
                        if (newHsv != null) {
                            hsv = newHsv
                            onColorChanged(hsvToColor(hsv))
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Draw hue/saturation wheel
                val bitmap = createColorWheelBitmap(size.width.toInt(), size.height.toInt(), hsv[2])
                drawImage(bitmap.asImageBitmap())

                // Draw selector
                val angle = Math.toRadians(hsv[0].toDouble())
                val satRadius = hsv[1] * radius
                val selectorX = center.x + (satRadius * cos(angle)).toFloat()
                val selectorY = center.y + (satRadius * sin(angle)).toFloat()

                drawCircle(
                    color = if (hsv[2] > 0.5f) Color.Black else Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(selectorX, selectorY),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Brightness")
        Slider(
            value = hsv[2],
            onValueChange = {
                hsv = floatArrayOf(hsv[0], hsv[1], it)
                onColorChanged(hsvToColor(hsv))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
    }
}

private fun calculateHsvFromOffset(offset: Offset, size: Float, v: Float): FloatArray? {
    val centerX = size / 2
    val centerY = size / 2
    val dx = offset.x - centerX
    val dy = offset.y - centerY
    val dist = hypot(dx, dy)
    val radius = size / 2

    if (dist > radius) return null

    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (angle < 0) angle += 360f

    val saturation = (dist / radius).coerceIn(0f, 1f)
    return floatArrayOf(angle, saturation, v)
}

private fun hsvToColor(hsv: FloatArray): Color {
    val colorInt = AndroidColor.HSVToColor(hsv)
    return Color(colorInt)
}

private fun createColorWheelBitmap(width: Int, height: Int, value: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = width.coerceAtMost(height) / 2f
    val centerX = width / 2f
    val centerY = height / 2f

    val huePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = SweepGradient(centerX, centerY, intArrayOf(
            AndroidColor.RED, AndroidColor.YELLOW, AndroidColor.GREEN,
            AndroidColor.CYAN, AndroidColor.BLUE, AndroidColor.MAGENTA, AndroidColor.RED
        ), null)
    }
    canvas.drawCircle(centerX, centerY, radius, huePaint)

    val saturationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(centerX, centerY, radius, AndroidColor.WHITE, 0x00FFFFFF, Shader.TileMode.CLAMP)
    }
    canvas.drawCircle(centerX, centerY, radius, saturationPaint)

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.BLACK
        alpha = ((1f - value) * 255).toInt()
    }
    canvas.drawCircle(centerX, centerY, radius, valuePaint)

    return bitmap
}

@Preview(showBackground = true)
@Composable
fun ColorPickerWheelPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        ColorPickerWheel(
            initialColor = Color.Blue,
            onColorChanged = {}
        )
    }
}
