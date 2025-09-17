package com.example.todolist

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.FragmentCalendarBinding
import com.example.todolist.db.ItemDao
import com.example.todolist.db.TodoDao
import com.example.todolist.db.TodoDatabase
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar

class CalendarFragment : Fragment() {
    private lateinit var binding: FragmentCalendarBinding
    private lateinit var itemDao: ItemDao
    private lateinit var todoDao: TodoDao
    private lateinit var viewModel: CalendarFragmentViewModel
    private lateinit var calendarListAdapter: CalendarListRecyclerViewAdapter
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var locationRemindManager: EditLocationRemindCalendarManager
    private val args: CalendarFragmentArgs by navArgs()
    val moshi = Moshi.Builder().build()
    val locationIdsJsonAdapter = moshi.adapter<List<List<Any>>>(Types.newParameterizedType(List::class.java, Types.newParameterizedType(List::class.java, Any::class.java)))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemDao = TodoDatabase.getInstance(requireContext().applicationContext).itemDao
        todoDao = TodoDatabase.getInstance(requireContext().applicationContext).todoDao
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        val factory = CalendarViewModelFactory(requireActivity().application, itemDao, todoDao)
        viewModel = ViewModelProvider(this, factory).get(CalendarFragmentViewModel(requireActivity().application, itemDao, todoDao)::class.java)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            viewModel.allItems.postValue(itemDao.getItemsList())
            viewModel.groups.postValue(todoDao.getItemsList())
            if (args.date != "") {
                viewModel.selectedCal.time = SimpleDateFormat("dd/MM/yyyy").parse(args.date)
            }
            viewModel.setCurrentDayInfo()
            viewModel.getSelectedDayItems()
            viewModel.getWeekDays()
            val resourceId = when (viewModel.selectedDay.value) {
                1 -> R.id.viewDay1
                2 -> R.id.viewDay2
                3 -> R.id.viewDay3
                4 -> R.id.viewDay4
                5 -> R.id.viewDay5
                6 -> R.id.viewDay6
                7 -> R.id.viewDay7
                else -> 0
            }

            binding.root.findViewById<View>(resourceId).visibility = View.VISIBLE
            binding.tvSunDate.text = "${viewModel.weekDays.value!![0]}"
            binding.tvMonDate.text = "${viewModel.weekDays.value!![1]}"
            binding.tvTueDate.text = "${viewModel.weekDays.value!![2]}"
            binding.tvWedDate.text = "${viewModel.weekDays.value!![3]}"
            binding.tvThuDate.text = "${viewModel.weekDays.value!![4]}"
            binding.tvFriDate.text = "${viewModel.weekDays.value!![5]}"
            binding.tvSatDate.text = "${viewModel.weekDays.value!![6]}"
            Log.d("Debugging", "resuming")
            Log.d("Debugging", viewModel.selectedDay.value.toString())
            Log.d("Debugging", SimpleDateFormat("dd/MM/yyyy").format(viewModel.selectedCal.time))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        lifecycleScope.launch {
            viewModel.allItems.postValue(itemDao.getItemsList())
            viewModel.groups.postValue(todoDao.getItemsList())
        }
        binding = FragmentCalendarBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        locationRemindManager = EditLocationRemindCalendarManager(requireContext(), binding.llLocationRemind, inflater, viewModel)
        calendarListAdapter = CalendarListRecyclerViewAdapter(viewModel)
        binding.rvCalendar.adapter = calendarListAdapter
        binding.rvCalendar.layoutManager = LinearLayoutManager(requireContext().applicationContext)
        binding.rvCalendar.scrollToPosition(0)
        binding.fabAddCalendar.setOnClickListener {
            binding.viewExit.visibility = View.VISIBLE
            binding.cvChooseGroup.visibility = View.VISIBLE
        }
        binding.btnDoneNew.setOnClickListener {
            if (viewModel.selectedNewGroupId.value != null) {
                val action = CalendarFragmentDirections.actionCalendarFragmentToAddItemFragment(group = viewModel.selectedNewGroupId.value!!, calendar = true, date = SimpleDateFormat("dd/MM/yyyy").format(viewModel.selectedCal.time))
                findNavController().navigate(action)
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
        viewModel.visibleEdit.observe(viewLifecycleOwner) {
            binding.cvEditItem.visibility = if (it) View.VISIBLE else View.GONE
            binding.viewExit.visibility = if (it) View.VISIBLE else View.GONE
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
        binding.viewExit.setOnClickListener {
            viewModel.visibleEdit.value = false
            binding.viewExit.visibility = View.GONE
            binding.cvChooseGroup.visibility = View.GONE
            viewModel.locationIds.value = listOf()
            locationRemindManager.clearOnFinish()
            viewModel.checkedEditLocation.value = false
        }
        binding.btnTimeDue.setOnClickListener {
            val timeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                viewModel.calDue.set(android.icu.util.Calendar.HOUR_OF_DAY, hour)
                viewModel.calDue.set(android.icu.util.Calendar.MINUTE, minute)
                viewModel.textEditDueTime.value = SimpleDateFormat("HH:mm").format(viewModel.calDue.time)
            }
            TimePickerDialog(requireContext(), android.R.style.Theme_Material_Dialog, timeSetListener, viewModel.calDue.get(
                android.icu.util.Calendar.HOUR_OF_DAY), viewModel.calDue.get(android.icu.util.Calendar.MINUTE), true).show()
        }
        binding.btnDayDue.setOnClickListener {
            val dateSetListener = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
                viewModel.calDue.set(android.icu.util.Calendar.DAY_OF_MONTH, day)
                viewModel.calDue.set(android.icu.util.Calendar.MONTH, month)
                viewModel.calDue.set(android.icu.util.Calendar.YEAR, year)
                viewModel.textEditDueDate.value = SimpleDateFormat("dd/MM/yyyy").format(viewModel.calDue.time)
            }
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                android.R.style.Theme_Material_Dialog,
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
                viewModel.calDate.set(android.icu.util.Calendar.DAY_OF_MONTH, day)
                viewModel.calDate.set(android.icu.util.Calendar.MONTH, month)
                viewModel.calDate.set(android.icu.util.Calendar.YEAR, year)
                viewModel.textEditItemDate.value = SimpleDateFormat("dd/MM/yyyy").format(viewModel.calDate.time)
            }
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                android.R.style.Theme_Material_Dialog,
                dateSetListener,
                2025,
                0,
                1
            )
            datePickerDialog.datePicker.minDate = viewModel.minDateInMillisStart.value!!
            datePickerDialog.show()
        }
        val startTimeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
            viewModel.calDate.set(android.icu.util.Calendar.HOUR_OF_DAY, hour)
            viewModel.calDate.set(android.icu.util.Calendar.MINUTE, minute)
            viewModel.textEditItemStart.value = SimpleDateFormat("HH:mm").format(viewModel.calDate.time)
            viewModel.calcMinEndTime(hour, minute, 0)
        }
        val endTimeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
            viewModel.calStartDate.set(android.icu.util.Calendar.HOUR_OF_DAY, hour)
            viewModel.calStartDate.set(android.icu.util.Calendar.MINUTE, minute)
            viewModel.textEditItemEnd.value = SimpleDateFormat("HH:mm").format(viewModel.calStartDate.time)
            viewModel.calcMinEndTime(hour, minute, 1)
        }
        binding.btnItemStart.setOnClickListener {
            TimePickerDialog(requireContext(), android.R.style.Theme_Material_Dialog, startTimeSetListener, viewModel.calDate.get(
                android.icu.util.Calendar.HOUR_OF_DAY), viewModel.calDate.get(android.icu.util.Calendar.MINUTE), true).show()
        }
        binding.btnItemEnd.setOnClickListener {
            TimePickerDialog(requireContext(), android.R.style.Theme_Material_Dialog, endTimeSetListener, viewModel.calStartDate.get(
                android.icu.util.Calendar.HOUR_OF_DAY), viewModel.calStartDate.get(android.icu.util.Calendar.MINUTE), true).show()
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
            binding.viewExit.visibility = View.GONE
        }
        binding.btnGroup.setOnClickListener {
            val titleList = viewModel.groups.value!!.map { it.title }.toTypedArray()
            val idList = viewModel.groups.value!!.map { it.id }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Choose Group")
                .setNegativeButton("Cancel", null)
                .setItems(titleList) { dialog, which ->
                    val selected = titleList[which]
                    val selectedId = idList[which]
                    viewModel.textEditParentGroup.value = selected
                    viewModel.selectedGroupId.value = selectedId
                    Log.d("Debugging", "Previous: ${viewModel.editingItem.value!!.group} New: $selectedId")
                    Log.d("Debugging", "Selected: $selected")
                }
                .show()
        }
        binding.btnGroupNew.setOnClickListener {
            val titleList = viewModel.groups.value!!.map { it.title }.toTypedArray()
            val idList = viewModel.groups.value!!.map { it.id }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Choose Parent Group")
                .setNegativeButton("Cancel", null)
                .setItems(titleList) { dialog, which ->
                    val selected = titleList[which]
                    val selectedId = idList[which]
                    viewModel.textParentGroup.value = selected
                    viewModel.selectedNewGroupId.value = selectedId
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
            viewModel.calStartDate.set(android.icu.util.Calendar.DAY_OF_MONTH, day)
            viewModel.calStartDate.set(android.icu.util.Calendar.MONTH, month)
            viewModel.calStartDate.set(android.icu.util.Calendar.YEAR, year)
            viewModel.textEditDateStart.value = SimpleDateFormat("dd/MM/yyyy").format(viewModel.calStartDate.time)
            viewModel.calcMinEndDate()
        }
        val dateEndSetListener = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
            viewModel.calEndDate.set(android.icu.util.Calendar.DAY_OF_MONTH, day)
            viewModel.calEndDate.set(android.icu.util.Calendar.MONTH, month)
            viewModel.calEndDate.set(android.icu.util.Calendar.YEAR, year)
            viewModel.textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(viewModel.calEndDate.time)
            viewModel.roundEndDate(year, month, day)
        }
        binding.btnStartDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                android.R.style.Theme_Material_Dialog,
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
                android.R.style.Theme_Material_Dialog,
                dateEndSetListener,
                2025,
                0,
                1
            )
            datePickerDialog.datePicker.minDate = viewModel.minDateInMillisEnd.value!!
            datePickerDialog.show()
        }
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                val swipedItem = calendarListAdapter.getItemAt(position)

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        calendarListAdapter.deleteItem(swipedItem)
                        if (!swipedItem.locationIds.isNullOrEmpty() && !swipedItem.locationIds.isNullOrBlank()) {
                            val locationIds = locationIdsJsonAdapter.fromJson(swipedItem.locationIds)
                            val removeList = mutableListOf<String>()
                            for (location in locationIds!!) {
                                removeList.add("${swipedItem.id} ${location[0]}")
                            }
                            geofencingClient.removeGeofences(removeList)
                        }
                    }
                    ItemTouchHelper.RIGHT -> {
//                        innerAdapter.notifyItemChanged(position)
                        calendarListAdapter.editItem(swipedItem)
                        Log.i("Debug", "Resetting swiped item at position: $position")
                        calendarListAdapter.notifyItemChanged(position)
//                        calendarListAdapter.resetSwiped(position)
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val deleteIcon = ContextCompat.getDrawable(recyclerView.context, R.drawable.baseline_delete_24)
                    val editIcon = ContextCompat.getDrawable(recyclerView.context, R.drawable.baseline_edit_24)
                    val deleteIconMargin = (itemView.height - deleteIcon!!.intrinsicHeight) / 2
                    val editIconMargin = (itemView.height - editIcon!!.intrinsicHeight) / 2

                    if (dX > 0) {
                        editIcon.setBounds(
                            itemView.left + 60,
                            itemView.top + editIconMargin,
                            itemView.left + 60 + editIcon.intrinsicWidth,
                            itemView.bottom - editIconMargin
                        )
                        editIcon.draw(c)
                    } else {
                        deleteIcon.setBounds(
                            itemView.right - 60 - deleteIcon.intrinsicWidth,
                            itemView.top + deleteIconMargin,
                            itemView.right - 60,
                            itemView.bottom - deleteIconMargin
                        )
                        deleteIcon.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.rvCalendar)

        viewModel.setCurrentDayInfo()
        val resourceId = when (viewModel.selectedDay.value) {
            1 -> R.id.viewDay1
            2 -> R.id.viewDay2
            3 -> R.id.viewDay3
            4 -> R.id.viewDay4
            5 -> R.id.viewDay5
            6 -> R.id.viewDay6
            7 -> R.id.viewDay7
            else -> 0
        }
        viewModel.getWeekDays()
        viewModel.allItems.observe(viewLifecycleOwner) { items ->
            if (items != null) {
                Log.d("Debugging", "items updated")
                viewModel.getSelectedDayItems()
            }
        }
        viewModel.showItems.observe(viewLifecycleOwner) {
            calendarListAdapter.setList(it)
        }

        if (args.date == "") {
            binding.root.findViewById<View>(resourceId).visibility = View.VISIBLE
            binding.tvSunDate.text = "${viewModel.weekDays.value!![0]}"
            binding.tvMonDate.text = "${viewModel.weekDays.value!![1]}"
            binding.tvTueDate.text = "${viewModel.weekDays.value!![2]}"
            binding.tvWedDate.text = "${viewModel.weekDays.value!![3]}"
            binding.tvThuDate.text = "${viewModel.weekDays.value!![4]}"
            binding.tvFriDate.text = "${viewModel.weekDays.value!![5]}"
            binding.tvSatDate.text = "${viewModel.weekDays.value!![6]}"
        }

        binding.clSun.setOnClickListener {
            viewModel.selectedDay.value = 1
            viewModel.selectedCal.set(Calendar.DAY_OF_WEEK, 1)
            viewModel.getSelectedDayItems()
            binding.root.findViewById<View>(R.id.viewDay1).visibility = View.VISIBLE
            binding.root.findViewById<View>(R.id.viewDay2).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay3).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay4).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay5).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay6).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay7).visibility = View.GONE
            viewModel.textMonthYear.value = SimpleDateFormat("MMMM yyyy").format(viewModel.selectedCal.time)
        }

        binding.clMon.setOnClickListener {
            viewModel.selectedDay.value = 2
            viewModel.selectedCal.set(Calendar.DAY_OF_WEEK, 2)
            viewModel.getSelectedDayItems()
            binding.root.findViewById<View>(R.id.viewDay1).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay2).visibility = View.VISIBLE
            binding.root.findViewById<View>(R.id.viewDay3).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay4).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay5).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay6).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay7).visibility = View.GONE
            viewModel.textMonthYear.value = SimpleDateFormat("MMMM yyyy").format(viewModel.selectedCal.time)
        }

        binding.clTue.setOnClickListener {
            viewModel.selectedDay.value = 3
            viewModel.selectedCal.set(Calendar.DAY_OF_WEEK, 3)
            viewModel.getSelectedDayItems()
            binding.root.findViewById<View>(R.id.viewDay1).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay2).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay3).visibility = View.VISIBLE
            binding.root.findViewById<View>(R.id.viewDay4).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay5).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay6).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay7).visibility = View.GONE
            viewModel.textMonthYear.value = SimpleDateFormat("MMMM yyyy").format(viewModel.selectedCal.time)
        }

        binding.clWed.setOnClickListener {
            viewModel.selectedDay.value = 4
            viewModel.selectedCal.set(Calendar.DAY_OF_WEEK, 4)
            viewModel.getSelectedDayItems()
            binding.root.findViewById<View>(R.id.viewDay1).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay2).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay3).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay4).visibility = View.VISIBLE
            binding.root.findViewById<View>(R.id.viewDay5).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay6).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay7).visibility = View.GONE
            viewModel.textMonthYear.value = SimpleDateFormat("MMMM yyyy").format(viewModel.selectedCal.time)
        }

        binding.clThu.setOnClickListener {
            viewModel.selectedDay.value = 5
            viewModel.selectedCal.set(Calendar.DAY_OF_WEEK, 5)
            viewModel.getSelectedDayItems()
            binding.root.findViewById<View>(R.id.viewDay1).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay2).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay3).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay4).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay5).visibility = View.VISIBLE
            binding.root.findViewById<View>(R.id.viewDay6).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay7).visibility = View.GONE
            viewModel.textMonthYear.value = SimpleDateFormat("MMMM yyyy").format(viewModel.selectedCal.time)
        }

        binding.clFri.setOnClickListener {
            viewModel.selectedDay.value = 6
            viewModel.selectedCal.set(Calendar.DAY_OF_WEEK, 6)
            viewModel.getSelectedDayItems()
            binding.root.findViewById<View>(R.id.viewDay1).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay2).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay3).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay4).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay5).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay6).visibility = View.VISIBLE
            binding.root.findViewById<View>(R.id.viewDay7).visibility = View.GONE
            viewModel.textMonthYear.value = SimpleDateFormat("MMMM yyyy").format(viewModel.selectedCal.time)
        }

        binding.clSat.setOnClickListener {
            viewModel.selectedDay.value = 7
            viewModel.selectedCal.set(Calendar.DAY_OF_WEEK, 7)
            viewModel.getSelectedDayItems()
            binding.root.findViewById<View>(R.id.viewDay1).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay2).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay3).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay4).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay5).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay6).visibility = View.GONE
            binding.root.findViewById<View>(R.id.viewDay7).visibility = View.VISIBLE
            viewModel.textMonthYear.value = SimpleDateFormat("MMMM yyyy").format(viewModel.selectedCal.time)
        }

        binding.btnWeekNext.setOnClickListener {
            viewModel.nextWeek()
            viewModel.getWeekDays()
            viewModel.getSelectedDayItems()
            binding.tvSunDate.text = "${viewModel.weekDays.value!![0]}"
            binding.tvMonDate.text = "${viewModel.weekDays.value!![1]}"
            binding.tvTueDate.text = "${viewModel.weekDays.value!![2]}"
            binding.tvWedDate.text = "${viewModel.weekDays.value!![3]}"
            binding.tvThuDate.text = "${viewModel.weekDays.value!![4]}"
            binding.tvFriDate.text = "${viewModel.weekDays.value!![5]}"
            binding.tvSatDate.text = "${viewModel.weekDays.value!![6]}"
        }

        binding.btnWeekPrev.setOnClickListener {
            viewModel.prevWeek()
            viewModel.getWeekDays()
            viewModel.getSelectedDayItems()
            binding.tvSunDate.text = "${viewModel.weekDays.value!![0]}"
            binding.tvMonDate.text = "${viewModel.weekDays.value!![1]}"
            binding.tvTueDate.text = "${viewModel.weekDays.value!![2]}"
            binding.tvWedDate.text = "${viewModel.weekDays.value!![3]}"
            binding.tvThuDate.text = "${viewModel.weekDays.value!![4]}"
            binding.tvFriDate.text = "${viewModel.weekDays.value!![5]}"
            binding.tvSatDate.text = "${viewModel.weekDays.value!![6]}"
        }

        return binding.root
    }
}