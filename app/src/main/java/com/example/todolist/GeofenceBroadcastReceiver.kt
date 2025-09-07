package com.example.todolist

import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent!!.hasError()) {
            // Handle error
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            if (triggeringGeofences == null) return
            for (geofence in triggeringGeofences) {
                val requestId = geofence.requestId
                geofence
            }
        }
    }
}