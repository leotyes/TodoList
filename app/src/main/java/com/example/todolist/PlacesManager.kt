package com.example.todolist

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient

object PlacesManager {
    private lateinit var placesClient: PlacesClient

    fun getPlacesClient(context: Context): PlacesClient {
        if (!::placesClient.isInitialized) {
            placesClient = Places.createClient(context)
        }
        return placesClient
    }
}