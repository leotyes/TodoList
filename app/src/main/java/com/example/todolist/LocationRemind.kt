package com.example.todolist

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import com.example.todolist.databinding.LocationRemindItemBinding
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.model.Place
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class LocationRemindManager(private val context: Context, private val container: LinearLayout, private val layoutInflater: LayoutInflater, private val viewModel: AddItemFragmentViewModel) {
    private val items = mutableListOf<LocationRemindItem>()
    private val locationIds = mutableMapOf<Int, String>()
    private var nextId = 1000

    fun addItem() {
        Log.d("Debugging", "$items.")
        if (items.size == 5) {
            // TODO make an error toast
            return
        }
        val binding = LocationRemindItemBinding.inflate(layoutInflater, container, false)
        viewModel.visibleLocationHint.value = false
        nextId++
        container.addView(binding.root)

        val item = LocationRemindItem(binding, this, context, nextId)

        items.add(item)
    }

    fun removeItem(item: LocationRemindItem) {
        container.removeView(item.binding.root)
        items.remove(item)
        locationIds.remove(item.nextId)
        viewModel.locationIds.value = locationIds.values.toList()
        if (items.isEmpty()) {
            viewModel.visibleLocationHint.value = true
        }
    }

    fun addLocationId(id: Int, locationId: String) {
        locationIds[id] = locationId
        viewModel.locationIds.value = locationIds.values.toList()
        Log.d("Debugging", "locationIds: $locationIds")
    }

    fun removeLocationId(id: Int) {
        if (locationIds.containsKey(id)) {
            locationIds.remove(id)
        }
    }
}

@SuppressLint("MissingPermission")
class LocationRemindItem(val binding: LocationRemindItemBinding, private val manager: LocationRemindManager, private val context: Context, val nextId: Int) {
    init {
//        val autocompleteFragment = binding.autocompleteSupportFragment.getFragment<AutocompleteSupportFragment>()
        Log.d("Debugging", "nextId: $nextId")
        binding.autocompleteSupportFragment.id = nextId
        val autocompleteFragment = AutocompleteSupportFragment.newInstance()

        val activity = context as FragmentActivity
        activity.supportFragmentManager.beginTransaction().replace(binding.autocompleteSupportFragment.id, autocompleteFragment).commitNow()
        val client = LocationServices.getFusedLocationProviderClient(context)
        lateinit var southWest: LatLng
        lateinit var northEast: LatLng

        client.lastLocation.addOnSuccessListener { location : Location? ->
            location?.let {
                southWest = LatLng(location.latitude - 0.05, location.longitude - 0.05)
                northEast = LatLng(location.latitude + 0.05, location.longitude + 0.05)
                autocompleteFragment.setLocationBias(RectangularBounds.newInstance(southWest, northEast))
            }
        }

        autocompleteFragment
            .setPlaceFields(listOf(Place.Field.ID, Place.Field.LOCATION, Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS))
            .setHint("Search a place")
            .setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onPlaceSelected(place: Place) {
                    manager.removeLocationId(nextId)
                    place.id?.let { manager.addLocationId(nextId, it) }
                }

                override fun onError(status: Status) {
                    Log.d("Debugging", "Failed")
                }
            })
        autocompleteFragment.view?.findViewById<ImageView>(com.google.android.libraries.places.R.id.places_autocomplete_search_button)?.visibility = View.GONE
        autocompleteFragment.view?.findViewById<ImageView>(com.google.android.libraries.places.R.id.places_autocomplete_clear_button)?.visibility = View.GONE

        binding.btnRemove.setOnClickListener {
            manager.removeItem(this)
        }
    }
}