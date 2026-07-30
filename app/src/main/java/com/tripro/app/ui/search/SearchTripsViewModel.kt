package com.tripro.app.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.model.Trip
import com.tripro.app.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Backs the bottom-nav "Search" popup. Created once at the nav-graph level for the
 *  whole signed-in session (so the popup opens instantly later), which means this
 *  listener starts the moment someone signs in — an uncaught error here would crash the
 *  app before the person ever taps Search, so it must never propagate past this point. */
class SearchTripsViewModel(
    tripRepository: TripRepository,
    currentUid: String
) : ViewModel() {
    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()

    init {
        viewModelScope.launch {
            tripRepository.observeUserTrips(currentUid)
                .catch { e -> Log.e("SearchTripsViewModel", "Error observing trips: ${e.message}", e) }
                .collect { _trips.value = it }
        }
    }
}