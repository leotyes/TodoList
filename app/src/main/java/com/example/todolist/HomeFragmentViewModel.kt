package com.example.todolist

import android.app.Application
import android.content.Context
import android.icu.util.Calendar
import android.util.Log
import android.util.TypedValue
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.todolist.db.GroupInfo
import com.example.todolist.db.ItemDao
import com.example.todolist.db.ItemInfo
import com.example.todolist.db.TodoDao
import kotlinx.coroutines.launch

class HomeFragmentViewModel(private val todoDao: TodoDao, private val itemDao: ItemDao, private val application: Application) : ViewModel() {
    val groups = todoDao.getItems()
    val items = itemDao.getItems()
    val textNewGroupName = MutableLiveData<String>()
    val editingGroup = MutableLiveData<Long>()
    val textName = MutableLiveData<String>()
    val textDescription = MutableLiveData<String>()
    val textDueTime = MutableLiveData<String>()
    val textDueDate = MutableLiveData<String>()
    val textItemDate = MutableLiveData<String>()
    val textItemStart = MutableLiveData<String>()
    val textItemEnd = MutableLiveData<String>()
    val textDateStart = MutableLiveData<String>()
    val textDateEnd = MutableLiveData<String>()
    val textRemind = MutableLiveData<String>()
    val checkedDue = MutableLiveData<Boolean>()
    val checkedItemDate = MutableLiveData<Boolean>()
    val checkedItemStart = MutableLiveData<Boolean>()
    val checkedItemEnd = MutableLiveData<Boolean>()
    val checkedRangeStart = MutableLiveData<Boolean>()
    val checkedRangeEnd = MutableLiveData<Boolean>()
    val checkedDaily = MutableLiveData<Boolean>()
    val checkedWeekly = MutableLiveData<Boolean>()
    val checkedMonthly = MutableLiveData<Boolean>()
    val checkedRemind = MutableLiveData<Boolean>()
    val minDateInMillisStart = MutableLiveData<Long>()
    val minDateInMillisEnd = MutableLiveData<Long>()
    val minDateInMillisDue = MutableLiveData<Long>()
    val minEndTime = MutableLiveData<Int>()
    val checkedRange = MutableLiveData<Boolean>()
    val parentGroup = MutableLiveData<Long>()
    val calStartDate = Calendar.getInstance()
    val calDate = Calendar.getInstance()
    val calEndDate = Calendar.getInstance()
    val calDue = Calendar.getInstance()

    init {
        textNewGroupName.value = ""
    }

    fun addGroup() {

    }

    fun addItem(parentGroup: GroupInfo) {

    }

    fun editClicked(groupId: Long) {
        editingGroup.value = groupId
    }

    fun doneNameClicked(): String {
        if (textNewGroupName.value!!.isNotBlank()) {
            viewModelScope.launch {
                todoDao.editItem(
                    GroupInfo(
                        editingGroup.value!!,
                        textNewGroupName.value!!,
                    )
                )
            }
            textNewGroupName.value = ""
            return "Group name updated"
        } else {
            return "Group name cannot be blank"
        }
    }

    fun itemClicked() {

    }

    fun deleteItem(item: ItemInfo) {
        viewModelScope.launch {
            WorkManager.getInstance(application).cancelUniqueWork(item.id.toString())
            itemDao.deleteItem(item)
        }
    }

    fun editItem(item: ItemInfo) {

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
        itemDao.getGroupedItems(item.id).observeForever {
            viewModelScope.launch {
                if (it != null) {
                    for (itemDelete in it) {
                        WorkManager.getInstance(application).cancelUniqueWork(itemDelete.id.toString())
                    }
                    itemDao.deleteGroupedItems(item.id)
                    todoDao.deleteItem(item)
                }
            }
        }
    }

    fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }

    fun getGrouped(parentId: Long): LiveData<List<ItemInfo>> {
        return itemDao.getGroupedItems(parentId)
    }
}