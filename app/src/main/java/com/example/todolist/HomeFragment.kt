package com.example.todolist

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.FragmentHomeBinding
import com.example.todolist.db.GroupInfo
import com.example.todolist.db.ItemDao
import com.example.todolist.db.ItemInfo
import com.example.todolist.db.TodoDao
import com.example.todolist.db.TodoDatabase
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.text.SimpleDateFormat
import kotlin.math.abs

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: HomeFragmentViewModel
    private lateinit var todoListRecyclerViewAdapter: TodoListRecyclerViewAdapter
    private lateinit var todoDao: TodoDao
    private lateinit var itemDao: ItemDao
    private lateinit var locationRemindManager: EditLocationRemindManager
    private lateinit var geofencingClient: GeofencingClient
    val moshi = Moshi.Builder().build()
    val locationIdsJsonAdapter = moshi.adapter<List<List<Any>>>(Types.newParameterizedType(List::class.java, Types.newParameterizedType(List::class.java, Any::class.java)))

    private val notifPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
        if (granted) {
            Toast.makeText(requireContext(), "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Notification permission denied, notifications will not work", Toast.LENGTH_SHORT).show()
        }

        checkPermissionChain()
    }
    private val fineLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
        if (granted) {
            Toast.makeText(requireContext(), "Fine location permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Fine location permission denied, location notifications may not be accurate", Toast.LENGTH_SHORT).show()
        }

        checkPermissionChain()
    }
    private val coarseLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
        if (granted) {
            Toast.makeText(requireContext(), "Coarse location permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Coarse location permission denied, location notifications will not work", Toast.LENGTH_SHORT).show()
        }

        checkPermissionChain()
    }
    private val backgroundLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
        if (granted) {
            Toast.makeText(requireContext(), "Background location permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Background location permission denied, location notifications may not work", Toast.LENGTH_SHORT).show()
        }

        checkPermissionChain()
    }

    private fun checkPermissionChain() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED -> {
                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                coarseLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                fineLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                backgroundLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        todoDao = TodoDatabase.getInstance(requireContext().applicationContext).todoDao
        itemDao = TodoDatabase.getInstance(requireContext().applicationContext).itemDao
        val factory = HomeViewModelFactory(todoDao, itemDao, requireActivity().application)
        viewModel = ViewModelProvider(this, factory).get(HomeFragmentViewModel(todoDao, itemDao, requireActivity().application)::class.java)
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        locationRemindManager = EditLocationRemindManager(requireContext(), binding.llLocationRemind, inflater, viewModel)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // API 23+
            val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)) {
                Toast.makeText(requireContext(), "Please disable battery optimizations for this app to ensure reminders work properly", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }
        }
        viewModel.locationRemindManager.value = locationRemindManager
        viewModel.visibleLocationHint.observe(viewLifecycleOwner) {
            if (it) {
                binding.tvLocationHint.visibility = View.VISIBLE
            } else {
                binding.tvLocationHint.visibility = View.GONE
            }
        }
        viewModel.toastText.observe(viewLifecycleOwner) {
            if (it != "") {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.toastText.value = ""
            }
        }
        binding.btnLocationRemind.setOnClickListener {
            locationRemindManager.addItem()
        }
        binding.rvGroups.layoutManager = LinearLayoutManager(requireContext().applicationContext)
        viewModel.visibleEdit.observe(viewLifecycleOwner) {
            binding.cvEditItem.visibility = if (it) View.VISIBLE else View.GONE
            binding.view.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.visibleEditRanges.observe(viewLifecycleOwner) {
            binding.llRangeStart.visibility = if (it) View.VISIBLE else View.GONE
            binding.llRangeEnd.visibility = if (it) View.VISIBLE else View.GONE
            binding.llDate.visibility = if (it) View.GONE else View.VISIBLE
        }
        viewModel.visibleEditTimes.observe(viewLifecycleOwner) {
            binding.llStartTime.visibility = if (it) View.VISIBLE else View.GONE
            binding.llEndTime.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.visibleEditRemind.observe(viewLifecycleOwner) {
            binding.llRemind.visibility = if (it) View.VISIBLE else View.GONE
        }
        binding.view.setOnClickListener {
            viewModel.visibleEdit.value = false
            binding.cvEditName.visibility = View.GONE
            binding.view.visibility = View.GONE
            viewModel.locationIds.value = listOf()
            locationRemindManager.clearOnFinish()
            viewModel.checkedEditLocation.value = false
        }
        checkPermissionChain()
        todoListRecyclerViewAdapter = TodoListRecyclerViewAdapter(viewModel,
            {
                selectedGroup: GroupInfo -> viewModel.editClicked(selectedGroup.id)
                binding.view.visibility = View.VISIBLE
                binding.cvEditName.visibility = View.VISIBLE
            },
            {
                selectedGroup: GroupInfo -> viewModel.deleteGroup(selectedGroup)
                binding.fabAdd.visibility = View.VISIBLE
            },
            {
                selectedGroup: GroupInfo -> viewModel.addItem(selectedGroup)
                val action = HomeFragmentDirections.actionHomeFragmentToAddItemFragment(selectedGroup.id)
                findNavController().navigate(action)
            },
            {
                item, checked -> viewModel.itemChecked(item, checked)
            },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("notification_channel", "NotifChannel", importance).apply {
                description = "Notifications Channel"
            }
            val notificationManager: NotificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        binding.rvGroups.adapter = todoListRecyclerViewAdapter
        binding.rvGroups.scrollToPosition(0)
        binding.btnDoneName.setOnClickListener {
            Toast.makeText(requireContext(), viewModel.doneNameClicked(), Toast.LENGTH_LONG).show()
            binding.cvEditName.visibility = View.GONE
            binding.view.visibility = View.GONE
        }
        viewModel.groups.observe(viewLifecycleOwner, {
            todoListRecyclerViewAdapter.setList(it)
            todoListRecyclerViewAdapter.notifyDataSetChanged()
        })
        binding.fabAdd.setOnClickListener {
            viewModel.addGroup()
            findNavController().navigate(R.id.action_homeFragment_to_addGroupFragment)
        }
        binding.btnTimeDue.setOnClickListener {
            val timeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                viewModel.calDue.set(Calendar.HOUR_OF_DAY, hour)
                viewModel.calDue.set(Calendar.MINUTE, minute)
                viewModel.textEditDueTime.value = SimpleDateFormat("HH:mm").format(viewModel.calDue.time)
            }
            TimePickerDialog(requireContext(), android.R.style.Theme_Material_Dialog, timeSetListener, viewModel.calDue.get(
                Calendar.HOUR_OF_DAY), viewModel.calDue.get(Calendar.MINUTE), true).show()
        }
        binding.btnDayDue.setOnClickListener {
            val dateSetListener = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
                viewModel.calDue.set(Calendar.DAY_OF_MONTH, day)
                viewModel.calDue.set(Calendar.MONTH, month)
                viewModel.calDue.set(Calendar.YEAR, year)
                viewModel.textEditDueDate.value = SimpleDateFormat("dd/MM/yyyy").format(viewModel.calDue.time)
            }
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                dateSetListener,
                2025,
                0,
                1
            )
            datePickerDialog.datePicker.minDate = viewModel.minDateInMillisDue.value!!
            datePickerDialog.show()
        }
        binding.btnItemDay.setOnClickListener {
            val dateSetListener = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
                viewModel.calDate.set(Calendar.DAY_OF_MONTH, day)
                viewModel.calDate.set(Calendar.MONTH, month)
                viewModel.calDate.set(Calendar.YEAR, year)
                viewModel.textEditItemDate.value = SimpleDateFormat("dd/MM/yyyy").format(viewModel.calDate.time)
            }
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                dateSetListener,
                2025,
                0,
                1
            )
            datePickerDialog.datePicker.minDate = viewModel.minDateInMillisStart.value!!
            datePickerDialog.show()
        }
        val startTimeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
            viewModel.calDate.set(Calendar.HOUR_OF_DAY, hour)
            viewModel.calDate.set(Calendar.MINUTE, minute)
            viewModel.textEditItemStart.value = SimpleDateFormat("HH:mm").format(viewModel.calDate.time)
            viewModel.calcMinEndTime(hour, minute, 0)
        }
        val endTimeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
            viewModel.calStartDate.set(Calendar.HOUR_OF_DAY, hour)
            viewModel.calStartDate.set(Calendar.MINUTE, minute)
            viewModel.textEditItemEnd.value = SimpleDateFormat("HH:mm").format(viewModel.calStartDate.time)
            viewModel.calcMinEndTime(hour, minute, 1)
        }
        binding.btnItemStart.setOnClickListener {
            TimePickerDialog(requireContext(), android.R.style.Theme_Material_Dialog, startTimeSetListener, viewModel.calDate.get(
                Calendar.HOUR_OF_DAY), viewModel.calDate.get(Calendar.MINUTE), true).show()
        }
        binding.btnItemEnd.setOnClickListener {
            TimePickerDialog(requireContext(), android.R.style.Theme_Material_Dialog, endTimeSetListener, viewModel.calStartDate.get(
                Calendar.HOUR_OF_DAY), viewModel.calStartDate.get(Calendar.MINUTE), true).show()
        }
        binding.cbDate.setOnClickListener {
            if (viewModel.checkedEditItemDate.value == false) {
                if (viewModel.checkedEditRangeStart.value != true && viewModel.checkedEditRangeEnd.value != true && viewModel.checkedEditDaily.value != true) {
                    viewModel.visibleEditTimes.value = false
                }
            } else {
                viewModel.visibleEditTimes.value = true
            }
            viewModel.visibleEditRemind.value = viewModel.remindVisibility(binding.llStartTime.visibility)
        }
        binding.btnItemDone.setOnClickListener {
            val result = viewModel.editItemFinish()
            if (viewModel.checkedEditLocation.value == true && result == "Item edited successfully") {
                val editingItem = viewModel.editingItem.value!!
                if (!editingItem.locationIds.isNullOrBlank() && !editingItem.locationIds.isNullOrEmpty()) {
                    val locationIds = locationIdsJsonAdapter.fromJson(editingItem.locationIds)
                    val removeList = mutableListOf<String>()
                    for (location in locationIds!!) {
                        removeList.add("${editingItem.id} ${location[0]}")
                    }
                    geofencingClient.removeGeofences(removeList)
                }
                if (viewModel.locationIds.value!! != listOf<Any>()) {
                    val geofencingRequest = viewModel.reinitializeGeofencing()
                    val geofencePendingIntent: PendingIntent by lazy {
                        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
                        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                    }
                    if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                            addOnSuccessListener {
                                Log.d("Debugging", "Geofences added")
                            }
                            addOnFailureListener {
                                Log.d("Debugging", "Geofences not added")
                            }
                        }
                    }
                }
            }
            Toast.makeText(requireContext(), result, Toast.LENGTH_LONG).show()
            binding.cvEditItem.visibility = View.GONE
            binding.view.visibility = View.GONE
        }
        binding.btnGroup.setOnClickListener {
            val titleList = viewModel.groups.value!!.map { it.title }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Choose Group")
                .setNegativeButton("Cancel", null)
                .setItems(titleList) { dialog, which ->
                    val selected = titleList[which]
                }
                .show()
        }
        binding.cbDaily.setOnClickListener {
            if (viewModel.checkedEditDaily.value == true) {
                viewModel.checkedEditWeekly.value = false
                viewModel.checkedEditMonthly.value = false
                viewModel.checkedEditItemDate.value = false
                viewModel.visibleEditRanges.value = true
                viewModel.visibleEditTimes.value = true
                viewModel.calcMinEndDate()
            } else {
                viewModel.visibleEditRanges.value = false
                viewModel.checkedEditRangeStart.value = false
                viewModel.checkedEditRangeEnd.value = false
                if (viewModel.checkedEditItemDate.value == true) {
                    viewModel.visibleEditTimes.value = true
                } else {
                    viewModel.visibleEditTimes.value = false
                }
            }
            viewModel.visibleEditRemind.value = viewModel.remindVisibility(binding.llStartTime.visibility)
        }
        binding.cbWeekly.setOnClickListener {
            if (viewModel.checkedEditWeekly.value == true) {
                viewModel.checkedEditDaily.value = false
                viewModel.checkedEditMonthly.value = false
                viewModel.checkedEditItemDate.value = false
                viewModel.visibleEditRanges.value = true
                if (viewModel.checkedEditRangeStart.value != true && viewModel.checkedEditRangeEnd.value != true) {
                    viewModel.visibleEditTimes.value = false
                }
                viewModel.calcMinEndDate()
            } else {
                viewModel.visibleEditRanges.value = false
                viewModel.checkedEditRangeStart.value = false
                viewModel.checkedEditRangeEnd.value = false
                if (viewModel.checkedEditItemDate.value == true) {
                    viewModel.visibleEditTimes.value = true
                } else {
                    viewModel.visibleEditTimes.value = false
                }
            }
            viewModel.visibleEditRemind.value = viewModel.remindVisibility(binding.llStartTime.visibility)
        }
        binding.cbMonthly.setOnClickListener {
            if (viewModel.checkedEditMonthly.value == true) {
                viewModel.checkedEditWeekly.value = false
                viewModel.checkedEditDaily.value = false
                viewModel.checkedEditItemDate.value = false
                viewModel.visibleEditRanges.value = true
                if (viewModel.checkedEditRangeStart.value != true && viewModel.checkedEditRangeEnd.value != true) {
                    viewModel.visibleEditTimes.value = false
                }
                viewModel.calcMinEndDate()
            } else {
                viewModel.visibleEditRanges.value = true
                if (viewModel.checkedEditItemDate.value == true) {
                    viewModel.visibleEditTimes.value = true
                } else {
                    viewModel.visibleEditTimes.value = false
                }
            }
            viewModel.visibleEditRemind.value = viewModel.remindVisibility(binding.llStartTime.visibility)
        }
        binding.cbStartDate.setOnClickListener {
            if (viewModel.checkedEditRangeStart.value == true || viewModel.checkedEditRangeEnd.value == true) {
                viewModel.visibleEditTimes.value = true
            } else {
                if (viewModel.checkedEditItemDate.value != true && viewModel.checkedEditDaily.value != true) {
                    viewModel.visibleEditTimes.value = false
                }
            }
            viewModel.visibleEditRemind.value = viewModel.remindVisibility(binding.llStartTime.visibility)
        }
        binding.cbEndDate.setOnClickListener {
            if (viewModel.checkedEditRangeStart.value == true || viewModel.checkedEditRangeEnd.value == true) {
                viewModel.visibleEditTimes.value = true
            } else {
                if (viewModel.checkedEditItemDate.value != true && viewModel.checkedEditDaily.value != true) {
                    viewModel.visibleEditTimes.value = false
                }
            }
            viewModel.visibleEditRemind.value = viewModel.remindVisibility(binding.llStartTime.visibility)
        }
        binding.cbStartTime.setOnClickListener {
            if (viewModel.checkedEditItemStart.value == true) {
                viewModel.visibleEditRemind.value = viewModel.remindVisibility(binding.llStartTime.visibility)
                viewModel.calcMinEndTime(source = 2)
            } else {
                viewModel.visibleEditRemind.value = viewModel.remindVisibility(binding.llStartTime.visibility)
            }
        }
        val dateStartSetListener = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
            viewModel.calStartDate.set(Calendar.DAY_OF_MONTH, day)
            viewModel.calStartDate.set(Calendar.MONTH, month)
            viewModel.calStartDate.set(Calendar.YEAR, year)
            viewModel.textEditDateStart.value = SimpleDateFormat("dd/MM/yyyy").format(viewModel.calStartDate.time)
            viewModel.calcMinEndDate()
        }
        val dateEndSetListener = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
            viewModel.calEndDate.set(Calendar.DAY_OF_MONTH, day)
            viewModel.calEndDate.set(Calendar.MONTH, month)
            viewModel.calEndDate.set(Calendar.YEAR, year)
            viewModel.textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(viewModel.calEndDate.time)
            viewModel.roundEndDate(year, month, day)
        }
        binding.btnStartDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                dateStartSetListener,
                2025,
                0,
                1
            )
            datePickerDialog.datePicker.minDate = viewModel.minDateInMillisStart.value!!
            datePickerDialog.show()
        }
        binding.btnEndDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                dateEndSetListener,
                2025,
                0,
                1
            )
            datePickerDialog.datePicker.minDate = viewModel.minDateInMillisEnd.value!!
            datePickerDialog.show()
        }
        return binding.root
    }
}