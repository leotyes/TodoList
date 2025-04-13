package com.example.todolist

import android.content.Context
import android.util.Log
import android.util.TypedValue
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.db.GroupInfo
import com.example.todolist.db.ItemDao
import com.example.todolist.db.ItemInfo
import com.example.todolist.db.TodoDao
import kotlinx.coroutines.launch

class HomeFragmentViewModel(private val todoDao: TodoDao, private val itemDao: ItemDao) : ViewModel() {
    val groups = todoDao.getItems()
    val items = itemDao.getItems()
    val textNewGroupName = MutableLiveData<String>()
    var selectedGroup: GroupInfo? = null
    var selectedItem: ItemInfo? = null
    val inspectTitle = MutableLiveData<String>()
    val inspectDescription = MutableLiveData<String>()
    val inspectDue = MutableLiveData<String>()
    val inspectRange = MutableLiveData<String>()
    val inspectRemind = MutableLiveData<String>()

    init {
        textNewGroupName.value = ""
    }

    fun groupClicked(selected: GroupInfo, visibility: Int) {
        Log.i("Debugging", visibility.toString())
        if (selectedGroup == null) {
            Log.i("Debugging", "ViewModel got it")
            selectedGroup = selected
        } else if (visibility == 0) {
            Log.i("Debugging", "back to null")
            selectedGroup = null
        }
    }

    fun addGroup() {

    }

    fun addItem(parentGroup: GroupInfo) {

    }

    fun editClicked() {

    }

    fun doneNameClicked() {
        /* TODO Make toast if blank */
        if (textNewGroupName.value!!.isNotBlank()) {
            viewModelScope.launch {
                todoDao.editItem(
                    GroupInfo(
                        selectedGroup!!.id,
                        textNewGroupName.value!!,
                    )
                )
            }
            textNewGroupName.value = ""
        }
    }

    fun itemClicked() {

    }

    fun itemChecked(item: ItemInfo, checked: Boolean) {
        viewModelScope.launch {
            itemDao.editItem(
                ItemInfo(
                    item.id,
                    item.name,
                    item.description,
                    item.group,
                    item.dueTime,
                    item.dueDate,
                    item.date,
                    item.timeStart,
                    item.timeEnd,
                    item.rangeStart,
                    item.rangeEnd,
                    item.repeatType,
                    checked,
                    item.remind
                )
            )
        }
    }

    fun deleteGroup(item: GroupInfo) {
        selectedGroup = null
        viewModelScope.launch {
            todoDao.deleteItem(item)
        }
    }

    fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }

    fun getGrouped(parentId: Int): LiveData<List<ItemInfo>> {
        return itemDao.getGroupedItems(parentId)
    }
}