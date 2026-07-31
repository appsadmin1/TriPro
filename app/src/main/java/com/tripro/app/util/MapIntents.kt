package com.tripro.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

fun openGoogleMapsDirections(context: Context, lat: Double, lng: Double) {
    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
    val mapsIntent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
    if (mapsIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapsIntent)
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}