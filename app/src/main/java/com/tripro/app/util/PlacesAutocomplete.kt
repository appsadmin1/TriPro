package com.tripro.app.util

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

/** Everything TriPro's forms need out of a Places search result. */
data class PickedPlace(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val placeId: String?
)

private val PLACE_FIELDS = listOf(
    Place.Field.ID,
    Place.Field.NAME,
    Place.Field.ADDRESS,
    Place.Field.LAT_LNG
)

/**
 * Returns a `launch()` function that opens Google's full-screen Place Autocomplete
 * search UI (a search box plus live results — no map or pin-dropping needed to find a
 * place by name) and reports the chosen place back through [onPlacePicked]. This is the
 * "pick it from Google Maps" flow used for hotels, airports, and itinerary stops instead
 * of typing an address by hand.
 *
 * Requires `Places.initialize(...)` to have already run once (see TriProApplication) and
 * the **Places API** enabled for the same Google Cloud project/key as MAPS_API_KEY — the
 * Maps SDK and Places API are separate APIs on the same key, both need to be turned on.
 * See README "Setup" step 5.
 *
 * [typesFilter] narrows results to specific Google Place types — e.g. `listOf("airport")`
 * for flights or `listOf("lodging")` for hotels; leave it empty for an unrestricted
 * search (used for generic itinerary stops). `setTypesFilter(List<String>)` needs a
 * reasonably current Places SDK release; if your version only exposes the older
 * `setTypeFilter(TypeFilter)` enum, swap this call for e.g.
 * `.setTypeFilter(TypeFilter.ESTABLISHMENT)` and drop the free-form type strings.
 */
@Composable
fun rememberPlacePicker(
    typesFilter: List<String> = emptyList(),
    onPlacePicked: (PickedPlace) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val place = Autocomplete.getPlaceFromIntent(data)
            val latLng = place.latLng
            if (latLng != null) {
                onPlacePicked(
                    PickedPlace(
                        name = place.name.orEmpty(),
                        address = place.address.orEmpty(),
                        lat = latLng.latitude,
                        lng = latLng.longitude,
                        placeId = place.id
                    )
                )
            }
        }
        // RESULT_CANCELED just means the user backed out of the search — nothing to do.
    }

    return {
        val intentBuilder = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, PLACE_FIELDS)
        if (typesFilter.isNotEmpty()) intentBuilder.setTypesFilter(typesFilter)
        launcher.launch(intentBuilder.build(context))
    }
}
