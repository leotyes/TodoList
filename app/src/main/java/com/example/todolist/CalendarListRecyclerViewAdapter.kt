package com.example.todolist

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.CalendarListItemBinding
import com.example.todolist.db.ItemInfo
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.text.SimpleDateFormat

class CalendarListRecyclerViewAdapter(private val viewModel: CalendarFragmentViewModel) : RecyclerView.Adapter<CalendarViewHolder>() {
    private val calendarAdapter = ArrayList<ItemInfo>()
    private lateinit var parentContext: Context
    private var checkedItems = HashSet<Long>()
    private val moshi = Moshi.Builder().build()
    private val locationIdsJsonAdapter = moshi.adapter<List<List<Any>>>(Types.newParameterizedType(List::class.java, Types.newParameterizedType(List::class.java, Any::class.java)))
    private lateinit var geofencingClient: GeofencingClient

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = CalendarListItemBinding.inflate(layoutInflater, parent, false)
        parentContext = parent.context
        geofencingClient = LocationServices.getGeofencingClient(parentContext)
        return CalendarViewHolder(itemBinding, viewModel)
    }

    override fun getItemCount(): Int {
        return calendarAdapter.size
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        Log.d("Debugging", "binding view holder at position $position")
        holder.bind(calendarAdapter[position], locationIdsJsonAdapter, geofencingClient, { itemId -> isChecked(itemId) }, { itemId -> check(itemId) })
    }

    fun getItemAt(position: Int): ItemInfo {
        return calendarAdapter[position]
    }

    fun setList(items: List<ItemInfo>) {
        calendarAdapter.clear()
        calendarAdapter.addAll(items)
        Log.d("Debugging", "setting list: $items")
        notifyDataSetChanged()
    }

    fun deleteItem(item: ItemInfo) {
        viewModel.deleteItem(item)
    }

    fun editItem(item: ItemInfo) {
        viewModel.editItemAppear(item)
    }

//    fun resetSwiped(position: Int) {
//        notifyItemChanged(position)
//    }

    fun isChecked(itemId: Long): Boolean {
        if (calendarAdapter[(calendarAdapter.indexOfFirst { it.id == itemId })].checked == true) {
            checkedItems.add(itemId)
            return true
        }
        return checkedItems.contains(itemId)
    }

    fun check(itemId: Long) {
        val position = calendarAdapter.indexOfFirst { it.id == itemId }
        if (checkedItems.contains(itemId)) {
            checkedItems.remove(itemId)
        } else {
            checkedItems.add(itemId)
        }
        notifyItemChanged(position)
        viewModel.itemChecked(itemId, checkedItems.contains(itemId))
    }
}

class CalendarViewHolder(val binding: CalendarListItemBinding, val viewModel: CalendarFragmentViewModel) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: ItemInfo, locationIdsJsonAdapter: JsonAdapter<List<List<Any>>>, geofencingClient: GeofencingClient, isChecked: (itemId: Long) -> Boolean, check: (itemId: Long) -> Unit) {
        var repeatType = ""
        var repeatRange = ""
        var repeatTime = ""
        if (item.locationIds != null) {
            val moshi = Moshi.Builder().build()
            val locationIdsJsonAdapter = moshi.adapter<List<List<Any>>>(Types.newParameterizedType(List::class.java, Types.newParameterizedType(List::class.java, Any::class.java)))
            val locationIds = locationIdsJsonAdapter.fromJson(item.locationIds!!)
            binding.tvLocation.text = "${locationIds!!.size} location${if (locationIds!!.size > 1) "s" else ""}"
        } else {
            binding.tvLocation.text = "No locations"
        }
        if (item.date == null) {
            if (item.repeatType != null) {
                if (item.repeatType == 1) {
                    repeatType = "Repeats Daily"
                } else if (item.repeatType == 2) {
                    repeatType = "Repeats Weekly"
                } else {
                    repeatType = "Repeats Monthly"
                }
                if (item.rangeStart == null) {
                    if (item.rangeEnd == null) repeatRange = " Forever" else repeatRange = " Until ${SimpleDateFormat("dd/MM/yyyy").format(item.rangeEnd)}"
                } else if (item.rangeEnd == null) {
                    repeatRange = " Forever Starting On ${SimpleDateFormat("dd/MM/yyyy").format(item.rangeStart)}"
                } else {
                    repeatRange = " From ${SimpleDateFormat("dd/MM/yyyy").format(item.rangeStart)} to ${SimpleDateFormat("dd/MM/yyyy").format(item.rangeEnd)}"
                }
                if (item.timeStart == null) {
                    if (item.timeEnd != null) repeatTime = " Ends at " + SimpleDateFormat("HH:mm").format(item.timeEnd)
                } else if (item.timeEnd == null) {
                    repeatTime = " Starts at " + SimpleDateFormat("HH:mm").format(item.timeStart)
                } else {
                    repeatTime = " " + SimpleDateFormat("HH:mm").format(item.timeStart) + "-" + SimpleDateFormat("HH:mm").format(item.timeEnd)
                }
                binding.tvTime.text = "$repeatType$repeatRange$repeatTime"
            } else {
                binding.tvTime.text = "No time"
            }
        } else if (item.timeStart == null) {
            if (item.timeEnd == null) binding.tvTime.text = SimpleDateFormat("dd/MM/yyyy").format(item.date) else binding.tvTime.text = "Ends at " + SimpleDateFormat("HH:mm").format(item.timeEnd) + " " + SimpleDateFormat("dd/MM/yyyy").format(item.date)
        } else if (item.timeEnd == null) {
            if (item.timeStart == null) binding.tvTime.text = SimpleDateFormat("dd/MM/yyyy").format(item.date) else binding.tvTime.text = "Starts at " + SimpleDateFormat("HH:mm").format(item.timeStart) + " " + SimpleDateFormat("dd/MM/yyyy").format(item.date)
        } else {
            binding.tvTime.text = SimpleDateFormat("HH:mm").format(item.timeStart) + "-" + SimpleDateFormat("HH:mm").format(item.timeEnd) + " " + SimpleDateFormat("dd/MM/yyyy").format(item.date)
        }
        binding.cbItemName.text = item.name
        binding.tvDue.text = if (item.dueDate == null) "No due date" else "Due at " + SimpleDateFormat("HH:mm").format(item.dueTime) + " " + SimpleDateFormat("dd/MM/yyyy").format(item.dueDate)
        binding.tvDescription.text = if (item.description == null) "No description" else item.description

        val checked = isChecked(item.id)
        binding.cbItemName.setOnCheckedChangeListener(null)
        binding.cbItemName.isChecked = checked
        if (checked) {
            binding.cbItemName.paintFlags = binding.cbItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.cbItemName.paintFlags = binding.cbItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        binding.cbItemName.setOnCheckedChangeListener { _, checked ->
            Log.i("Debug", "${binding.cbItemName.isChecked} just say something bro anything")
            check(item.id)
        }
        binding.tvRemind.text = if (item.remind != null) "Remind ${item.remind.toString()} minutes before" else "No reminder"
        binding.viewGroupCalendar.backgroundTintList = ColorStateList.valueOf(viewModel.groupColours.value!![item.group]!!)
    }
}