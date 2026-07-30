package com.tripro.app.util

import android.app.Activity
import android.content.Context
import android.content.Intent

object ShareUtils {
    /** "Invite friends" — a plain Android share sheet so people pick WhatsApp/SMS/email
     *  themselves. No app-store link exists yet since TriPro isn't published; swap one
     *  in once it is.
     *
     *  If [context] isn't an Activity (e.g. an Application context slipped in), Android
     *  requires FLAG_ACTIVITY_NEW_TASK on the launched Intent or it throws — this is
     *  exactly what crashed when NavGraph passed the Application context here. Adding
     *  the flag only in that case keeps normal Activity-context calls behaving exactly
     *  as before. */
    fun shareAppInvite(context: Context) {
        val message = "Come plan our next trip together on TriPro — a shared itinerary, " +
                "hotel & flight info, and a map of everywhere we're going, all in one place."
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val chooser = Intent.createChooser(sendIntent, "Invite a friend to TriPro")
        if (context !is Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}