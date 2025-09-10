package com.example.todolist

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.FragmentActivity
import com.example.todolist.databinding.LocationRemindItemBinding
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.model.Place
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class EditLocationRemindManager(private val context: Context, private val container: LinearLayout, private val layoutInflater: LayoutInflater, private val viewModel: HomeFragmentViewModel) {
    private val items = mutableListOf<EditLocationRemindItem>()
    private val locationIds = mutableMapOf<Int, List<Any>>()
    private var nextId = 1000

    fun addItem() {
        Log.d("Debugging", "$items.")
        if (items.size == 5) {
            viewModel.toastText.value = "Maximum of 5 locations reached"
            return
        }
        val binding = LocationRemindItemBinding.inflate(layoutInflater, container, false)
        viewModel.visibleLocationHint.value = false
        nextId++
        container.addView(binding.root)

        val item = EditLocationRemindItem(binding, this, context, nextId)

        items.add(item)
    }

    fun addSavedItem(placeId: String, radius: Int) {
        val binding = LocationRemindItemBinding.inflate(layoutInflater, container, false)
        viewModel.visibleLocationHint.value = false
        nextId++
        container.addView(binding.root)

        val item = EditLocationRemindItem(binding, this, context, nextId)
        item.setSavedLocation(placeId, radius)

        items.add(item)
    }

    fun removeItem(item: EditLocationRemindItem) {
        container.removeView(item.binding.root)
        items.remove(item)
        locationIds.remove(item.nextId)
        viewModel.locationIds.value = locationIds.values.toList()
        if (items.isEmpty()) {
            viewModel.visibleLocationHint.value = true
        }
    }

    fun editRadius(id: Int, radius: Int) {
        if (locationIds.containsKey(id)) {
            locationIds[id] = listOf(locationIds[id]!![0], radius, locationIds[id]!![2])
            viewModel.locationIds.value = locationIds.values.toList()
        }
    }

    fun addLocationId(id: Int, locationId: String, radius: Int, location: LatLng) {
        for ((key, value) in locationIds) {
            if (value[0] == locationId && key != id) {
                viewModel.toastText.value = "Location already added, this will not be added"
                return
            }
        }
        locationIds[id] = listOf(locationId, radius, location)
        viewModel.locationIds.value = locationIds.values.toList()
        Log.d("Debugging", "locationIds: $locationIds")
    }

    fun removeLocationId(id: Int) {
        if (locationIds.containsKey(id)) {
            locationIds.remove(id)
        }
    }

    fun clearOnFinish() {
        for (item in items) {
            container.removeView(item.binding.root)
        }
        viewModel.visibleLocationHint.value = true
        viewModel.locationIds.value = listOf()
        items.clear()
        locationIds.clear()
        nextId = 1000
    }
}

@SuppressLint("MissingPermission")
class EditLocationRemindItem(val binding: LocationRemindItemBinding, private val manager: EditLocationRemindManager, private val context: Context, val nextId: Int) {
    val autocompleteFragment = AutocompleteSupportFragment.newInstance()

    init {
        Log.d("Debugging", "nextId: $nextId")
        binding.autocompleteSupportFragment.id = nextId


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
                    if (binding.etRadius.text.toString().isDigitsOnly() && binding.etRadius.text.toString().isNotEmpty()) {
                        place.id?.let { manager.addLocationId(nextId, it, binding.etRadius.text.toString().toInt(), place.location) }
                    } else {
                        place.id?.let { manager.addLocationId(nextId, it, 1, place.location) }
                    }
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

        binding.etRadius.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                if (binding.etRadius.text.toString().isDigitsOnly() && binding.etRadius.text.toString().isNotEmpty()) {
                    manager.editRadius(nextId, binding.etRadius.text.toString().toInt())
                } else {
                    manager.editRadius(nextId, 1)
                }
            }
        })
    }

    fun setSavedLocation(placeId: String, radius: Int) {
        autocompleteFragment.view?.findViewById<ImageView>(com.google.android.libraries.places.R.id.places_autocomplete_clear_button)?.visibility = View.VISIBLE

        val placesClient = PlacesManager.getPlacesClient(context)
        val request = FetchPlaceRequest.newInstance(placeId, listOf(Place.Field.ID, Place.Field.LOCATION, Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS))

        placesClient.fetchPlace(request).addOnSuccessListener {
            val place = it.place
            autocompleteFragment.view?.findViewById<EditText>(com.google.android.libraries.places.R.id.places_autocomplete_search_input)?.setText(place.displayName)
            manager.addLocationId(nextId, placeId, radius, place.location)
        }

        binding.etRadius.setText(radius.toString())
    }
}