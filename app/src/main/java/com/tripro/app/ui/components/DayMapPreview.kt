package com.tripro.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

data class MapPin(
    val title: String,
    val snippet: String,
    val lat: Double,
    val lng: Double,
    val hueColor: Float = BitmapDescriptorFactory.HUE_AZURE
)

/**
 * Read-only map preview for a day's pins. DESIGN.md doesn't specify a map component, so
 * this borrows the same rounded-16dp / bordered-card language used everywhere else.
 */
@Composable
fun DayMapPreview(
    pins: List<MapPin>,
    modifier: Modifier = Modifier,
    heightDp: Int = 200
) {
    if (pins.isEmpty()) return

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(pins.first().lat, pins.first().lng), 12f)
    }

    GoogleMap(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(16.dp)),
        cameraPositionState = cameraPositionState,
        properties = remember { MapProperties(isMyLocationEnabled = false) },
        uiSettings = remember {
            MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            )
        }
    ) {
        pins.forEach { pin ->
            Marker(
                state = MarkerState(position = LatLng(pin.lat, pin.lng)),
                title = pin.title,
                snippet = pin.snippet,
                icon = BitmapDescriptorFactory.defaultMarker(pin.hueColor)
            )
        }
    }
}
