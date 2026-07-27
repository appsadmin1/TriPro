package com.tripro.app.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.tripro.app.BuildConfig
import com.tripro.app.data.model.Attachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

/**
 * Uploads itinerary attachments (hotel reservations, flight docs, restaurant
 * confirmations, ...) to Cloudinary using an UNSIGNED upload preset — this is designed
 * to be safe to embed in a client app, unlike the API Secret (see below).
 *
 * Deleting a file requires Cloudinary's API Secret, which must never ship inside an
 * APK, so this repository only uploads. Removing an attachment in the app detaches it
 * from Firestore immediately (feels instant to the user) and separately asks the
 * Netlify function to delete the real file server-side — see
 * DayDetailViewModel.removeAttachment and netlify/functions/delete-attachment.mjs.
 *
 * Setup reminder (see README): free-plan accounts must enable "Allow delivery of PDF
 * and ZIP files" under Console Settings > Security, or PDF attachments will upload fine
 * but fail to load when viewed/downloaded.
 */
class CloudinaryRepository(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    suspend fun upload(
        contentResolver: ContentResolver,
        fileUri: Uri,
        fileName: String,
        uploadedBy: String
    ): Attachment = withContext(Dispatchers.IO) {
        val mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"
        val bytes = contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
            ?: throw IOException("Couldn't read the selected file")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET)
            .addFormDataPart("file", fileName, bytes.toRequestBody(mimeType.toMediaTypeOrNull()))
            .build()

        // "auto" lets Cloudinary pick image/video/raw based on the file itself, so one
        // endpoint handles photos, PDFs (uploaded as type "image" — Cloudinary can
        // rasterize them), and anything else (as "raw") without us branching on mimeType.
        val url = "https://api.cloudinary.com/v1_1/${BuildConfig.CLOUDINARY_CLOUD_NAME}/auto/upload"
        val request = Request.Builder().url(url).post(requestBody).build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Empty response from Cloudinary")
            if (!response.isSuccessful) {
                val message = JSONObject(body).optJSONObject("error")?.optString("message")
                throw IOException("Cloudinary upload failed: ${message ?: "${response.code} ${response.message}"}")
            }
            val json = JSONObject(body)

            Attachment(
                id = UUID.randomUUID().toString(),
                fileName = fileName,
                downloadUrl = json.getString("secure_url"),
                publicId = json.getString("public_id"),
                resourceType = json.optString("resource_type", "raw"),
                mimeType = mimeType,
                uploadedBy = uploadedBy,
                uploadedAtMillis = System.currentTimeMillis()
            )
        }
    }
}
