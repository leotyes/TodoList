package com.example.todolist

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import com.example.todolist.databinding.ActivityMainBinding
import com.example.todolist.databinding.GroupListItemBinding
import com.example.todolist.databinding.TodoListItemBinding
import com.example.todolist.db.GroupInfo
import com.example.todolist.db.ItemInfo

class TodoListRecyclerViewAdapter(private val viewModel: HomeFragmentViewModel, private val clickListener: (GroupInfo, Int) -> Unit, private val editListener: (GroupInfo) -> Unit, private val deleteListener: (GroupInfo) -> Unit, private val addListener: (GroupInfo) -> Unit, private val itemCheckedListener: (ItemInfo, Boolean) -> Unit,): RecyclerView.Adapter<TodoViewHolder>() {
    private val groupsAdapter = ArrayList<GroupInfo>()
    private lateinit var parentContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = TodoListItemBinding.inflate(layoutInflater, parent, false)
        parentContext = parent.context
        return TodoViewHolder(itemBinding, viewModel)
    }

    override fun getItemCount(): Int {
        return groupsAdapter.size
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.binding.rvItems.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.bind(groupsAdapter[position], clickListener, editListener, deleteListener, addListener, itemCheckedListener)
        //holder.binding.root.elevation = viewModel.dpToPx(parentContext, 9f)
        //holder.binding.root.cardElevation = viewModel.dpToPx(parentContext, 9f)
    }

    fun setList(list: List<GroupInfo>) {
        Log.i("Debug", list.toString())
        groupsAdapter.clear()
        groupsAdapter.addAll(list)
    }
}

class TodoViewHolder(val binding: TodoListItemBinding, val viewModel: HomeFragmentViewModel): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: GroupInfo, clickListener: (GroupInfo, Int) -> Unit, editListener: (GroupInfo) -> Unit, deleteListener: (GroupInfo) -> Unit, addListener: (GroupInfo) -> Unit, itemCheckedListener: (ItemInfo, Boolean) -> Unit) {
        val innerAdapter = GroupItemRecyclerViewAdapter(viewModel, itemCheckedListener)
        binding.tvTitle.text = item.title
        binding.btnArrow.setOnClickListener {
            if (/*viewModel.selectedGroup == item ||*/ viewModel.selectedGroup == null) {
                clickListener(item, binding.btnAdd.visibility)
                if (binding.btnAdd.visibility == View.GONE) {
                    Log.i("Debugging", viewModel.selectedGroup.toString())
                    //binding.root.elevation = viewModel.dpToPx(itemView.context, 11f)
                    //binding.root.cardElevation = viewModel.dpToPx(itemView.context, 11f)
                    binding.btnAdd.visibility = View.VISIBLE
                    binding.btnEditName.visibility = View.VISIBLE
                    binding.btnDeleteGroup.visibility = View.VISIBLE
                    innerAdapter.expand(true)
                    innerAdapter.notifyDataSetChanged()
                    binding.btnArrow.setImageResource(R.drawable.baseline_keyboard_arrow_up_24)
                }
            } else if (binding.btnAdd.visibility == View.VISIBLE) {
                clickListener(item, binding.btnAdd.visibility)
                binding.btnAdd.visibility = View.GONE
                binding.btnEditName.visibility = View.GONE
                binding.btnDeleteGroup.visibility = View.GONE
                innerAdapter.expand(false)
                innerAdapter.notifyDataSetChanged()
                binding.btnArrow.setImageResource(R.drawable.baseline_keyboard_arrow_down_24)
                //viewModel.selectedGroup = null
            }
        }
        binding.btnDeleteGroup.setOnClickListener {
            deleteListener(item)
            binding.btnAdd.visibility = View.GONE
            binding.btnEditName.visibility = View.GONE
            binding.btnDeleteGroup.visibility = View.GONE
            binding.btnArrow.setImageResource(R.drawable.baseline_keyboard_arrow_down_24)
        }
        binding.btnEditName.setOnClickListener {
            editListener(item)
        }
        binding.btnAdd.setOnClickListener {
            addListener(item)
        }
        if (viewModel.selectedGroup == item) {
            binding.btnAdd.visibility = View.VISIBLE
            binding.btnEditName.visibility = View.VISIBLE
            binding.btnDeleteGroup.visibility = View.VISIBLE
            binding.btnArrow.setImageResource(R.drawable.baseline_keyboard_arrow_up_24)
        }
        binding.rvItems.adapter = innerAdapter
        viewModel.getGrouped(item.id).observeForever {
            innerAdapter.setList(it)
            innerAdapter.notifyDataSetChanged()
        }
    }
}