package com.tripro.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.tasks.await

data class PlaceResult(
    val name: String,
    val address: String,
    val latLng: LatLng?
)

@Composable
fun PlaceSearchField(
    label: String,
    initialValue: String,
    onPlaceSelected: (PlaceResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val placesClient = remember { Places.createClient(context) }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }
    
    var query by remember { mutableStateOf(initialValue) }
    var predictions by remember { mutableStateOf<List<com.google.android.libraries.places.api.model.AutocompletePrediction>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                isSearching = true
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )

        if (isSearching && query.length > 2) {
            LaunchedEffect(query) {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setSessionToken(sessionToken)
                    .setQuery(query)
                    .build()
                
                try {
                    val response = placesClient.findAutocompletePredictions(request).await()
                    predictions = response.autocompletePredictions
                } catch (e: Exception) {
                    predictions = emptyList()
                }
            }

            if (predictions.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(predictions) { prediction ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val placeId = prediction.placeId
                                    val placeFields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                                    val fetchPlaceRequest = FetchPlaceRequest.builder(placeId, placeFields).build()
                                    
                                    placesClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener { response ->
                                        val place = response.place
                                        onPlaceSelected(PlaceResult(
                                            name = place.name ?: prediction.getPrimaryText(null).toString(),
                                            address = place.address ?: prediction.getSecondaryText(null).toString(),
                                            latLng = place.latLng
                                        ))
                                        query = place.name ?: prediction.getPrimaryText(null).toString()
                                        isSearching = false
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Text(prediction.getPrimaryText(null).toString(), style = MaterialTheme.typography.bodyMedium)
                            Text(prediction.getSecondaryText(null).toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Divider()
                    }
                }
            }
        }
    }
}
