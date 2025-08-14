package com.example.todolist

import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
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
        if (viewModel.expandedGroup.value == groupId) {
            viewModel.expandedGroup.value = -1L
        } else {
            val prevPosition = groupsAdapter.indexOfFirst { it.id == viewModel.expandedGroup.value }
            viewModel.expandedGroup.value = groupId
            if (viewModel.expandedGroup.value != -1L) {
                notifyItemChanged(prevPosition)
            }
        }
        notifyItemChanged(position)
    }

    fun isExpanded(groupId: Long): Boolean {
        return (groupId == viewModel.expandedGroup.value)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.binding.rvItems.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.bind(groupsAdapter[position], editListener, deleteListener, addListener, itemCheckedListener,
            { groupId -> expand(groupId) },
            { groupId -> isExpanded(groupId) },
            { groupId -> getPosition(groupId) })
    }

    fun setList(list: List<GroupInfo>) {
        Log.i("Debug", list.toString())
        groupsAdapter.clear()
        groupsAdapter.addAll(list)
    }

    fun getPosition(groupId: Long): Int {
        return groupsAdapter.indexOfFirst { it.id == groupId }
    }
}

class TodoViewHolder(val binding: TodoListItemBinding, val viewModel: HomeFragmentViewModel): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: GroupInfo, editListener: (GroupInfo) -> Unit, deleteListener: (GroupInfo) -> Unit, addListener: (GroupInfo) -> Unit, itemCheckedListener: (ItemInfo, Boolean) -> Unit, expandClickedListener: (groupId: Long) -> Unit, expandCheck: (groupId: Long) -> Boolean, getGroupPosition: (groupId: Long) -> Int) {
        val innerAdapter = GroupItemRecyclerViewAdapter(viewModel, itemCheckedListener)
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun isLongPressDragEnabled(): Boolean {
                return if (expandCheck(item.id)) {
                    true
                } else {
                    false
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

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return true
            }

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                return if (expandCheck(item.id)) {
                    super.getSwipeDirs(recyclerView, viewHolder)
                } else {
                    0
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val swipedItem = innerAdapter.getItemAt(position)
                Log.i("Debug", "Swiped item: ${swipedItem.id} in group: ${item.id} at position: $position")

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        innerAdapter.deleteItem(swipedItem)
                    }
                    ItemTouchHelper.RIGHT -> {
//                        innerAdapter.notifyItemChanged(position)
                        innerAdapter.editItem(swipedItem)
                        Log.i("Debug", "Resetting swiped item at position: $position")
                        innerAdapter.resetSwiped(position)
                        expandClickedListener(item.id)
                        expandClickedListener(item.id)
                    }
                }
            }
        }
        )
        itemTouchHelper.attachToRecyclerView(binding.rvItems)
        binding.tvTitle.text = item.title
        binding.btnArrow.setOnClickListener {
            expandClickedListener(item.id)
        }
        val expanded = expandCheck(item.id)
        if (!expanded) {
            Log.i("Debug", "Collapsed")
            binding.btnAdd.visibility = View.GONE
            binding.btnEditName.visibility = View.GONE
            binding.btnDeleteGroup.visibility = View.GONE
            binding.btnArrow.setImageResource(R.drawable.baseline_keyboard_arrow_down_24)
            innerAdapter.expand(false)
            innerAdapter.notifyDataSetChanged()
        } else {
            Log.i("Debug", "Expanded")
            binding.btnAdd.visibility = View.VISIBLE
            binding.btnEditName.visibility = View.VISIBLE
            binding.btnDeleteGroup.visibility = View.VISIBLE
            binding.btnArrow.setImageResource(R.drawable.baseline_keyboard_arrow_up_24)
            innerAdapter.expand(true)
            innerAdapter.notifyDataSetChanged()
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
        binding.rvItems.adapter = innerAdapter
        viewModel.getGrouped(item.id).observeForever {
            innerAdapter.setList(it)
            innerAdapter.notifyDataSetChanged()
        }
    }
}