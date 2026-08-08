package com.tripro.app.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * [publicId] + [resourceType] are Cloudinary's identifiers for the uploaded asset
 * ("image", "video", or "raw") — both are needed to delete the asset later via
 * Cloudinary's destroy API, which requires the project's API Secret and therefore can't
 * be called directly from the app. See netlify/functions/delete-attachment.mjs, which
 * does this server-side; DayDetailViewModel.removeAttachment calls it over HTTP.
 */
@IgnoreExtraProperties
data class Attachment(
    val id: String = "",
    val fileName: String = "",
    val downloadUrl: String = "",
    val publicId: String = "",
    val resourceType: String = "",
    val mimeType: String = "",
    val uploadedBy: String = "",
    val uploadedAtMillis: Long = 0L
)
