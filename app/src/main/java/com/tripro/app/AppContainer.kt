package com.tripro.app

import android.content.Context
import com.tripro.app.data.repository.ActivityRepository
import com.tripro.app.data.repository.AuthRepository
import com.tripro.app.data.repository.CloudinaryRepository
import com.tripro.app.data.repository.FlightLookupRepository
import com.tripro.app.data.repository.PushNotificationRepository
import com.tripro.app.data.repository.TripRepository
import com.tripro.app.data.repository.UserRepository
import com.tripro.app.data.repository.WeatherRepository

class AppContainer(context: Context) {
    val authRepository = AuthRepository(context.applicationContext)
    val userRepository = UserRepository()
    val tripRepository = TripRepository()
    val cloudinaryRepository = CloudinaryRepository()
    val weatherRepository = WeatherRepository()
    val pushNotificationRepository = PushNotificationRepository()
    val activityRepository = ActivityRepository()
    val flightLookupRepository = FlightLookupRepository()
}