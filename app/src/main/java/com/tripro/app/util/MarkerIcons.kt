package com.tripro.app.util

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/** BitmapDescriptorFactory.defaultMarker() only accepts a hue, not an arbitrary color —
 *  so pins are drawn as small filled circles instead, letting Settings colors be exact. */
object MarkerIcons {
    private val cache = mutableMapOf<Int, BitmapDescriptor>()

    fun forColor(colorInt: Int, sizeDp: Int = 32): BitmapDescriptor = cache.getOrPut(colorInt) {
        val density = Resources.getSystem().displayMetrics.density
        val size = (sizeDp * density).toInt().coerceAtLeast(24)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val borderWidth = size * 0.12f
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorInt; style = Paint.Style.FILL }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = borderWidth }
        val radius = size / 2f - borderWidth
        val center = size / 2f
        canvas.drawCircle(center, center, radius, fillPaint)
        canvas.drawCircle(center, center, radius, borderPaint)
        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}