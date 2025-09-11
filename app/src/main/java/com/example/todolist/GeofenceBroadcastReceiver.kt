package com.example.todolist

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import android.util.Log
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.todolist.db.TodoDatabase
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent!!.hasError()) {
            return
        }

        val pendingResult = goAsync()
        val itemDao = TodoDatabase.getInstance(context).itemDao
        val todoDao = TodoDatabase.getInstance(context).todoDao
        val geofenceTransition = geofencingEvent.geofenceTransition
        val placesClient = PlacesManager.getPlacesClient(context)

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL || geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            if (triggeringGeofences == null) return
            for (geofence in triggeringGeofences) {
                Log.d("Debugging", "Geofence triggered somewhere")
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            Log.d("Debugging", "Geofence triggered: ${geofence.requestId}")
                            val itemId = geofence.requestId.split(" ")[0].toLong()
                            val placeId = geofence.requestId.split(" ")[1]
                            val item = itemDao.getItem(itemId)
                            val group = todoDao.getGroupById(item.group)
                            val request = FetchPlaceRequest.newInstance(placeId, listOf(Place.Field.ID, Place.Field.LOCATION, Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS))
                            val response = placesClient.fetchPlace(request).await()
                            val place = response.place
                            val moshi = Moshi.Builder().build()
                            val locationIdsJsonAdapter = moshi.adapter<List<List<Any>>>(Types.newParameterizedType(List::class.java, Types.newParameterizedType(List::class.java, Any::class.java)))
                            val locationRadius = locationIdsJsonAdapter.fromJson(item.locationIds)?.first { it[0] == placeId }?.get(1) as Int
                            val builder = NotificationCompat.Builder(context, "notification_channel")
                                .setSmallIcon(R.drawable.baseline_keyboard_arrow_up_24)
                                .setContentTitle("${item.name} at ${place.formattedAddress}")
                                .setContentText("$locationRadius km away")
                                .setStyle(
                                    NotificationCompat.BigTextStyle()
                                        .setBigContentTitle("${item.name} at ${place.displayName} in group ${group.title}")
                                        .bigText(item.description))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            with(NotificationManagerCompat.from(context)) {
                                if (ActivityCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    notify(itemId.toInt(), builder.build())
                                    return@with
                                }
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } catch (e: Exception) {
                    Log.d("Debugging", "Geofence error: ${e.message}")
                    continue
                }
            }
        }
    }
}