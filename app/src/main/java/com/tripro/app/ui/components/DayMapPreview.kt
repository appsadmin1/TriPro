package com.tripro.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import com.tripro.app.data.model.ItemType

data class MapPin(
    val title: String,
    val snippet: String,
    val lat: Double,
    val lng: Double,
    val hueColor: Float = BitmapDescriptorFactory.HUE_AZURE
)

/** Item 8b: one marker hue per item type — purely a drawing choice, no extra API calls. */
fun hueForItemType(type: ItemType): Float = when (type) {
    ItemType.HOTEL -> BitmapDescriptorFactory.HUE_VIOLET
    ItemType.FLIGHT -> BitmapDescriptorFactory.HUE_AZURE
    ItemType.RESTAURANT -> BitmapDescriptorFactory.HUE_ORANGE
    ItemType.ATTRACTION -> BitmapDescriptorFactory.HUE_YELLOW
    ItemType.ACTIVITY -> BitmapDescriptorFactory.HUE_GREEN
    ItemType.TRANSPORT -> BitmapDescriptorFactory.HUE_CYAN
    ItemType.SHOW -> BitmapDescriptorFactory.HUE_MAGENTA
    ItemType.CUSTOM -> BitmapDescriptorFactory.HUE_ROSE
}

/** Item 8c: tapping a pin opens Google Maps for turn-by-turn directions via a plain
 *  Intent — free, since it's the Maps *app* doing the routing, not a Directions API call
 *  from inside TriPro. Falls back to a geo: URI if the Google Maps app isn't installed. */
private fun openDirections(context: Context, pin: MapPin) {
    val navUri = Uri.parse("google.navigation:q=${pin.lat},${pin.lng}")
    val mapsIntent = Intent(Intent.ACTION_VIEW, navUri).apply { setPackage("com.google.android.apps.maps") }
    try {
        context.startActivity(mapsIntent)
    } catch (_: ActivityNotFoundException) {
        val geoUri = Uri.parse("geo:${pin.lat},${pin.lng}?q=${Uri.encode(pin.title)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
    }
}

/** Item 6: the expand toggle just resizes this same GoogleMap instance — no second map
 *  load, so no extra cost, just a bigger view. */
@Composable
fun DayMapPreview(
    pins: List<MapPin>,
    modifier: Modifier = Modifier,
    heightDp: Int = 200
) {
    if (pins.isEmpty()) return
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val currentHeight = if (expanded) 420 else heightDp

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(pins.first().lat, pins.first().lng), 12f)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        GoogleMap(
            modifier = Modifier.fillMaxWidth().height(currentHeight.dp).clip(RoundedCornerShape(16.dp)),
            cameraPositionState = cameraPositionState,
            properties = remember { MapProperties(isMyLocationEnabled = false) },
            uiSettings = remember {
                MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, mapToolbarEnabled = false)
            }
        ) {
            pins.forEach { pin ->
                Marker(
                    state = MarkerState(position = LatLng(pin.lat, pin.lng)),
                    title = pin.title,
                    snippet = pin.snippet,
                    icon = BitmapDescriptorFactory.defaultMarker(pin.hueColor),
                    onClick = { openDirections(context, pin); true }
                )
            }
        }
        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f))
                .size(36.dp)
        ) {
            Icon(
                if (expanded) Icons.Filled.CloseFullscreen else Icons.Filled.OpenInFull,
                contentDescription = if (expanded) "Collapse map" else "Expand map",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}