package com.tripro.app.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tripro.app.TriProApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Registered in AndroidManifest.xml. FCM calls onNewToken whenever a token is created or
 * rotated (fresh install, app data cleared, token expiry, ...) and onMessageReceived for
 * every push while the app process is alive.
 *
 * Important Android behavior: FCM only auto-displays a system notification for you when
 * the app is backgrounded/killed. When the app is in the foreground, onMessageReceived
 * fires instead and *nothing shows up on screen unless you build it yourself* — that's
 * what the notification+data combined payload from the Cloud Functions is for (see
 * functions/index.js): notification fields drive the auto-display case, and this method
 * uses the data fields to build the same look manually for the foreground case.
 */
class TriProMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val container = (application as TriProApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { container.userRepository.registerFcmToken(uid, token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "TriPro"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val tripId = message.data["tripId"]
        val date = message.data["date"]
        NotificationHelper.showNotification(applicationContext, title, body, tripId, date)
    }
}
