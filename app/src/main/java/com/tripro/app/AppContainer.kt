package com.tripro.app

import android.content.Context
import com.tripro.app.data.repository.AuthRepository
import com.tripro.app.data.repository.CloudinaryRepository
import com.tripro.app.data.repository.PushNotificationRepository
import com.tripro.app.data.repository.TripRepository
import com.tripro.app.data.repository.UserRepository
import com.tripro.app.data.repository.WeatherRepository

/**
 * Manual service locator. A real app of any size should switch this to Hilt
 * (add the plugin + @HiltAndroidApp + @Inject constructors) — this exists so the
 * scaffold has zero annotation-processing setup to get wrong on a first build.
 */
class AppContainer(context: Context) {
    val authRepository = AuthRepository(context.applicationContext)
    val userRepository = UserRepository()
    val tripRepository = TripRepository()
    val cloudinaryRepository = CloudinaryRepository()
    val weatherRepository = WeatherRepository()
    val pushNotificationRepository = PushNotificationRepository()
}
