package com.example.todolist

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.GroupListItemBinding
import com.example.todolist.db.GroupInfo
import com.example.todolist.db.ItemInfo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class GroupItemRecyclerViewAdapter(private val viewModel: HomeFragmentViewModel, private val checkedListener: (ItemInfo, Boolean) -> Unit): RecyclerView.Adapter<ItemViewHolder>() {
    private val itemsAdapter = ArrayList<ItemInfo>()
    private var expandedView = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = GroupListItemBinding.inflate(layoutInflater, parent, false)
        return ItemViewHolder(itemBinding, viewModel)
    }

    override fun getItemCount(): Int {
        return itemsAdapter.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(itemsAdapter[position], checkedListener, expandedView)
    }

    fun expand(expanded: Boolean) {
        expandedView = expanded
    }

    fun setList(list: List<ItemInfo>) {
        Log.i("Debug", list.toString())
        itemsAdapter.clear()
        itemsAdapter.addAll(list)
    }
}

class ItemViewHolder(val binding: GroupListItemBinding, val viewModel: HomeFragmentViewModel): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: ItemInfo, checkedListener: (ItemInfo, Boolean) -> Unit, expanded: Boolean) {
        var repeatType = ""
        var repeatRange = ""
        var repeatTime = ""
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
            binding.btnEdit.visibility = View.VISIBLE
            binding.btnDelete.visibility = View.VISIBLE
        } else {
            binding.tvDescription.visibility = View.GONE
            binding.btnEdit.visibility = View.GONE
            binding.btnDelete.visibility = View.GONE
        }
        binding.cbItemName.text = item.name
        binding.tvDue.text = if (item.dueDate == null) "No due date" else "Due at " + SimpleDateFormat("HH:mm").format(item.dueTime) + " " + SimpleDateFormat("dd/MM/yyyy").format(item.dueDate)
        binding.tvDescription.text = if (item.description == null) "No description" else item.description
        binding.cbItemName.setOnClickListener {
            if (binding.cbItemName.isChecked) {
                binding.cbItemName.paintFlags = binding.cbItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                checkedListener(item, true)
            } else {
                binding.cbItemName.paintFlags = binding.cbItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                checkedListener(item, false)
            }
        }
        binding.tvRemind.text =
        // add remind change
    }
}