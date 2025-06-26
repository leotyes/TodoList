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

class TodoListRecyclerViewAdapter(private val viewModel: HomeFragmentViewModel, private val editListener: (GroupInfo) -> Unit, private val deleteListener: (GroupInfo) -> Unit, private val addListener: (GroupInfo) -> Unit, private val itemCheckedListener: (ItemInfo, Boolean) -> Unit,): RecyclerView.Adapter<TodoViewHolder>() {
    private val groupsAdapter = ArrayList<GroupInfo>()
    private var expandedGroups = HashSet<Long>()
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

    fun expand(groupId: Long) {
        val position = groupsAdapter.indexOfFirst { it.id == groupId }
        Log.i("Debug", "${groupsAdapter[position]}")
        if (expandedGroups.contains(groupId)) {
            expandedGroups.remove(groupId)
        } else {
            expandedGroups.add(groupId)
        }
        notifyItemChanged(position)
    }

    fun isExpanded(groupId: Long): Boolean {
        return expandedGroups.contains(groupId)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.binding.rvItems.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.bind(groupsAdapter[position], editListener, deleteListener, addListener, itemCheckedListener,
            { groupId -> expand(groupId) },
            { groupId -> isExpanded(groupId) })
    }

    fun setList(list: List<GroupInfo>) {
        Log.i("Debug", list.toString())
        groupsAdapter.clear()
        groupsAdapter.addAll(list)
    }
}

class TodoViewHolder(val binding: TodoListItemBinding, val viewModel: HomeFragmentViewModel): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: GroupInfo, editListener: (GroupInfo) -> Unit, deleteListener: (GroupInfo) -> Unit, addListener: (GroupInfo) -> Unit, itemCheckedListener: (ItemInfo, Boolean) -> Unit, expandClickedListener: (groupId: Long) -> Unit, expandCheck: (groupId: Long) -> Boolean) {
        val innerAdapter = GroupItemRecyclerViewAdapter(viewModel, itemCheckedListener)
        binding.tvTitle.text = item.title
        binding.btnArrow.setOnClickListener {
            expandClickedListener(item.id)
        }
        val expanded = expandCheck(item.id)
        if (!expanded) {
            binding.btnAdd.visibility = View.GONE
            binding.btnEditName.visibility = View.GONE
            binding.btnDeleteGroup.visibility = View.GONE
            binding.btnArrow.setImageResource(R.drawable.baseline_keyboard_arrow_down_24)
            innerAdapter.expand(false)
            innerAdapter.notifyDataSetChanged()
        } else {
            binding.btnAdd.visibility = View.VISIBLE
            binding.btnEditName.visibility = View.VISIBLE
            binding.btnDeleteGroup.visibility = View.VISIBLE
            binding.btnArrow.setImageResource(R.drawable.baseline_keyboard_arrow_up_24)
            innerAdapter.expand(true)
            innerAdapter.notifyDataSetChanged()
        }
        binding.btnDeleteGroup.setOnClickListener {
//            we must delete all the items in it TODO
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
        binding.rvItems.adapter = innerAdapter
        viewModel.getGrouped(item.id).observeForever {
            innerAdapter.setList(it)
            innerAdapter.notifyDataSetChanged()
        }
    }
}