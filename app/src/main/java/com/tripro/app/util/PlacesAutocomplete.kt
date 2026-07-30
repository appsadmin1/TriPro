package com.tripro.app.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay

data class PickedPlace(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val placeId: String?
)

private data class AutocompleteRow(val placeId: String, val primaryText: String, val secondaryText: String)

/**
 * Item 8a. An inline search box + results list + map preview inside a Dialog, replacing
 * the old full-screen Autocomplete Activity — same billing shape as before (Google bills
 * one Autocomplete "session" when fetchPlace() is called below; typing predictions
 * within a session is free either way), just without leaving the current screen, so you
 * can see the pin land before committing to it.
 *
 * `setTypesFilter(List<String>)` needs a reasonably current Places SDK release; if your
 * pinned version only exposes the older `setTypeFilter(TypeFilter)` enum, swap that one
 * call for the enum equivalent and drop the free-form type strings (same caveat as the
 * old rememberPlacePicker had).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSearchMapDialog(
    visible: Boolean,
    typesFilter: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onPlacePicked: (PickedPlace) -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val placesClient = remember { Places.createClient(context) }
    var sessionToken by remember { mutableStateOf(AutocompleteSessionToken.newInstance()) }
    var query by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<AutocompleteRow>>(emptyList()) }
    var selected by remember { mutableStateOf<PickedPlace?>(null) }

    LaunchedEffect(query) {
        if (query.length < 2) { predictions = emptyList(); return@LaunchedEffect }
        delay(300) // debounce so a fast typist doesn't fire a request per keystroke
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(sessionToken)
            .setQuery(query)
            .apply { if (typesFilter.isNotEmpty()) setTypesFilter(typesFilter) }
            .build()
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                predictions = response.autocompletePredictions.map {
                    AutocompleteRow(it.placeId, it.getPrimaryText(null).toString(), it.getSecondaryText(null).toString())
                }
            }
            .addOnFailureListener { predictions = emptyList() }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Search for a place", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; selected = null },
                    label = { Text("Search") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                val picked = selected
                if (picked == null) {
                    LazyColumn(modifier = Modifier.height(220.dp)) {
                        items(predictions, key = { it.placeId }) { row ->
                            ListItem(
                                headlineContent = { Text(row.primaryText) },
                                supportingContent = { Text(row.secondaryText) },
                                leadingContent = { Icon(Icons.Filled.Place, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                                    val fetchRequest = FetchPlaceRequest.builder(row.placeId, fields)
                                        .setSessionToken(sessionToken)
                                        .build()
                                    placesClient.fetchPlace(fetchRequest).addOnSuccessListener { response ->
                                        val place = response.place
                                        val latLng = place.latLng ?: return@addOnSuccessListener
                                        selected = PickedPlace(
                                            name = place.name.orEmpty(), address = place.address.orEmpty(),
                                            lat = latLng.latitude, lng = latLng.longitude, placeId = place.id
                                        )
                                        // Session just got billed/closed — fresh token for next time.
                                        sessionToken = AutocompleteSessionToken.newInstance()
                                    }
                                }
                            )
                        }
                    }
                } else {
                    val cameraState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(LatLng(picked.lat, picked.lng), 15f)
                    }
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(picked.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(picked.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        GoogleMap(
                            modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 8.dp),
                            cameraPositionState = cameraState
                        ) {
                            Marker(state = MarkerState(position = LatLng(picked.lat, picked.lng)))
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            TextButton(onClick = { selected = null }) { Text("Search again") }
                            Spacer(Modifier.weight(1f))
                            Button(onClick = { onPlacePicked(picked) }) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text("Use this place")
                            }
                        }
                    }
                }
            }
        }
    }
}