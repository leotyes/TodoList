package com.example.todolist

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkManager
import com.example.todolist.databinding.GroupListItemBinding
import com.example.todolist.db.GroupInfo
import com.example.todolist.db.ItemInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class GroupItemRecyclerViewAdapter(private val viewModel: HomeFragmentViewModel, private val checkedListener: (ItemInfo, Boolean) -> Unit): RecyclerView.Adapter<ItemViewHolder>() {
    private val itemsAdapter = ArrayList<ItemInfo>()
    private var checkedItems = HashSet<Long>()
    private var expandedView = false
    private var recyclerView: RecyclerView? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = GroupListItemBinding.inflate(layoutInflater, parent, false)
        return ItemViewHolder(itemBinding, viewModel)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    override fun getItemCount(): Int {
        return itemsAdapter.size
    }

    fun expand(expanded: Boolean) {
        expandedView = expanded
    }

    fun check(itemId: Long) {
        val position = itemsAdapter.indexOfFirst { it.id == itemId }
        if (checkedItems.contains(itemId)) {
            checkedItems.remove(itemId)
        } else {
            checkedItems.add(itemId)
        }
        recyclerView?.post {
            notifyItemChanged(position)
        }
    }

    fun getItemAt(position: Int): ItemInfo {
        return itemsAdapter[position]
    }

    fun deleteItem(item: ItemInfo) {
        viewModel.deleteItem(item)
    }

    fun editItem(item: ItemInfo) {
        viewModel.editItemAppear(item)
    }

    fun isChecked(itemId: Long): Boolean {
        if (itemsAdapter[(itemsAdapter.indexOfFirst { it.id == itemId })].checked == true) {
            checkedItems.add(itemId)
            return true
        }
        return checkedItems.contains(itemId)
    }

    fun setList(list: List<ItemInfo>) {
//        Log.i("Debug", list.toString())
        itemsAdapter.clear()
        itemsAdapter.addAll(list)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(itemsAdapter[position], checkedListener, expandedView,
            { itemId -> check(itemId) },
            { itemId -> isChecked(itemId) })
    }

    fun resetSwiped(position: Int) {
        recyclerView?.post {
            notifyItemChanged(position)
            Log.i("Debug", "hello")
        }
    }
}

class ItemViewHolder(val binding: GroupListItemBinding, val viewModel: HomeFragmentViewModel): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: ItemInfo, checkedListener: (ItemInfo, Boolean) -> Unit, expanded: Boolean, checkClickedListener: (itemId: Long) -> Unit, checkCheck: (itemId: Long) -> Boolean) {
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
                    if (item.rangeEnd == null) repeatRange = " Forever" else " Until ${SimpleDateFormat("dd/MM/yyyy").format(item.rangeEnd)}"
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
        if (expanded == true) {
            binding.tvDescription.visibility = View.VISIBLE
        } else {
            binding.tvDescription.visibility = View.GONE
        }
        binding.cbItemName.text = item.name
        binding.tvDue.text = if (item.dueDate == null) "No due date" else "Due at " + SimpleDateFormat("HH:mm").format(item.dueTime) + " " + SimpleDateFormat("dd/MM/yyyy").format(item.dueDate)
        binding.tvDescription.text = if (item.description == null) "No description" else item.description

        val checked = checkCheck(item.id)
        binding.cbItemName.setOnCheckedChangeListener(null)
        binding.cbItemName.isChecked = checked
        if (checked) {
            binding.cbItemName.paintFlags = binding.cbItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.cbItemName.paintFlags = binding.cbItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        binding.cbItemName.setOnCheckedChangeListener { _, checked ->
            Log.i("Debug", "${binding.cbItemName.isChecked} just say something bro anything")
            checkClickedListener(item.id)
            checkedListener(item, checked)
        }
        binding.tvRemind.text = if (item.remind != null) "Remind ${item.remind.toString()} minutes before" else "No reminder"
    }
}