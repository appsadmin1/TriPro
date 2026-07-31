package com.tripro.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.tripro.app.util.MarkerIcons
import com.tripro.app.util.openGoogleMapsDirections

data class MapPin(
    val title: String,
    val snippet: String,
    val lat: Double,
    val lng: Double,
    val colorInt: Int = android.graphics.Color.parseColor("#405F91")
)

/** Zooms/pans to fit every pin (instead of a fixed zoom on the first one), colors each
 *  pin by activity type, and opens Google Maps directions when a pin is tapped. */
@Composable
fun DayMapPreview(pins: List<MapPin>, modifier: Modifier = Modifier, heightDp: Int = 200) {
    if (pins.isEmpty()) return
    val context = LocalContext.current

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(pins.first().lat, pins.first().lng), 12f)
    }
    var mapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(pins, mapLoaded) {
        if (!mapLoaded) return@LaunchedEffect
        runCatching {
            if (pins.size == 1) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(pins.first().lat, pins.first().lng), 14f))
            } else {
                val bounds = LatLngBounds.Builder().apply { pins.forEach { include(LatLng(it.lat, it.lng)) } }.build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 96))
            }
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxWidth().height(heightDp.dp).clip(RoundedCornerShape(16.dp)),
        cameraPositionState = cameraPositionState,
        properties = remember { MapProperties(isMyLocationEnabled = false) },
        uiSettings = remember { MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, mapToolbarEnabled = false) },
        onMapLoaded = { mapLoaded = true }
    ) {
        pins.forEach { pin ->
            Marker(
                state = MarkerState(position = LatLng(pin.lat, pin.lng)),
                title = pin.title,
                snippet = pin.snippet,
                icon = MarkerIcons.forColor(pin.colorInt),
                onClick = { openGoogleMapsDirections(context, pin.lat, pin.lng); true }
            )
        }
    }
}