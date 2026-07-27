package com.tripro.app.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

object FileDownloader {

    /**
     * Hands the download off to the system DownloadManager, which saves the file into the
     * public Downloads folder and shows progress in the system notification shade — this is
     * the "download it locally to the phone" action from the attachment viewer.
     *
     * No storage permission is needed on API 29+ (scoped storage). On API 26-28,
     * WRITE_EXTERNAL_STORAGE is declared with maxSdkVersion="28" in the manifest and must be
     * granted at runtime first — see DayDetailScreen's permission launcher.
     */
    fun downloadToDeviceDownloads(context: Context, url: String, fileName: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }

    /**
     * Downloads a file into the app's own cache dir (no permission needed — it's private
     * storage) so it can be rendered in-app (PDF pages) or shared via FileProvider
     * ("Open with…" for file types we don't render ourselves).
     */
    suspend fun downloadToCache(
        context: Context,
        url: String,
        fileName: String,
        httpClient: OkHttpClient = OkHttpClient()
    ): File = withContext(Dispatchers.IO) {
        val safeName = fileName.ifBlank { "attachment" }
        val destination = File(context.cacheDir, "attachments").apply { mkdirs() }
            .resolve(safeName)

        // Reuse an already-downloaded copy instead of re-fetching every time the viewer opens.
        if (destination.exists() && destination.length() > 0) return@withContext destination

        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Download failed: ${response.code}")
            val body = response.body ?: throw IOException("Empty response body")
            destination.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
        destination
    }
}
