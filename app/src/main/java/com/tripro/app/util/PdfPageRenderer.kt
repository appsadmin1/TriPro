package com.tripro.app.util

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PdfPageRenderer {

    /**
     * Rasterizes every page of [file] to a Bitmap using android.graphics.pdf.PdfRenderer
     * (built into the platform since API 21 — no external PDF library needed). Pages are
     * rendered at ~2x their native size for a crisp look on modern screens without
     * ballooning memory on very long documents.
     */
    suspend fun renderPages(file: File, scale: Float = 2f): List<Bitmap> = withContext(Dispatchers.Default) {
        val pages = mutableListOf<Bitmap>()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (index in 0 until renderer.pageCount) {
                    renderer.openPage(index).use { page ->
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pages += bitmap
                    }
                }
            }
        }
        pages
    }
}
