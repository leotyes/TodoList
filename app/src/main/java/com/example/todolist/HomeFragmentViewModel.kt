package com.example.todolist

import android.app.Application
import android.content.Context
import android.icu.util.Calendar
import android.util.Log
import android.util.TypedValue
import android.view.View
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
import java.text.SimpleDateFormat
import kotlin.math.min

class HomeFragmentViewModel(private val todoDao: TodoDao, private val itemDao: ItemDao, private val application: Application) : ViewModel() {
    val groups = todoDao.getItems()
    val items = itemDao.getItems()
    val textNewGroupName = MutableLiveData<String>()
    val editingGroup = MutableLiveData<Long>()
    val visibleEdit = MutableLiveData<Boolean>()
    val textEditName = MutableLiveData<String>()
    val textEditDescription = MutableLiveData<String>()
    val textEditDueTime = MutableLiveData<String>()
    val textEditDueDate = MutableLiveData<String>()
    val textEditItemDate = MutableLiveData<String>()
    val textEditItemStart = MutableLiveData<String>()
    val textEditItemEnd = MutableLiveData<String>()
    val textEditDateStart = MutableLiveData<String>()
    val textEditDateEnd = MutableLiveData<String>()
    val textEditRemind = MutableLiveData<String>()
    val checkedEditDue = MutableLiveData<Boolean>()
    val checkedEditItemDate = MutableLiveData<Boolean>()
    val checkedEditItemStart = MutableLiveData<Boolean>()
    val checkedEditItemEnd = MutableLiveData<Boolean>()
    val checkedEditRangeStart = MutableLiveData<Boolean>()
    val checkedEditRangeEnd = MutableLiveData<Boolean>()
    val checkedEditDaily = MutableLiveData<Boolean>()
    val checkedEditWeekly = MutableLiveData<Boolean>()
    val checkedEditMonthly = MutableLiveData<Boolean>()
    val checkedEditRemind = MutableLiveData<Boolean>()
    val minDateInMillisStart = MutableLiveData<Long>()
    val minDateInMillisEnd = MutableLiveData<Long>()
    val minDateInMillisDue = MutableLiveData<Long>()
    val minEndTime = MutableLiveData<Int>()
    val visibleEditRanges = MutableLiveData<Boolean>()
    val visibleEditTimes = MutableLiveData<Boolean>()
    val visibleEditRemind = MutableLiveData<Boolean>()
    val parentGroup = MutableLiveData<Long>()
    val calStartDate = Calendar.getInstance()
    val calDate = Calendar.getInstance()
    val calEndDate = Calendar.getInstance()
    val calDue = Calendar.getInstance()

    init {
        textNewGroupName.value = ""
        checkedEditDue.value = false
        checkedEditItemDate.value = false
        checkedEditItemStart.value = false
        checkedEditItemEnd.value = false
        visibleEditRanges.value = false
        visibleEditTimes.value = false
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

    fun editItemAppear(item: ItemInfo) {
        visibleEdit.value = true
//        val itemId = itemDao.insertItem(
//            ItemInfo(
//                0,
//                textName.value!!,
//                if (textDescription.value!!.isNotBlank()) textDescription.value!! else null,
//                parentGroup.value!!,
//                if (checkedDue.value == true) SimpleDateFormat("HH:mm").parse(textDueTime.value).time else null,
//                if (checkedDue.value == true) SimpleDateFormat("dd/MM/yyyy").parse(textDueDate.value).time else null,
//                if (checkedItemDate.value == true && checkedDaily.value != true && checkedWeekly.value != true && checkedMonthly.value != true) SimpleDateFormat("dd/MM/yyyy").parse(textItemDate.value).time else null,
//                if (checkedItemStart.value == true && (checkedDaily.value == true || checkedWeekly.value == true || checkedMonthly.value == true || checkedItemDate.value == true)) SimpleDateFormat("HH:mm").parse(textItemStart.value).time else null,
//                if (checkedItemEnd.value == true && (checkedDaily.value == true || checkedWeekly.value == true || checkedMonthly.value == true || checkedItemDate.value == true)) SimpleDateFormat("HH:mm").parse(textItemEnd.value).time else null,
//                if (checkedRangeStart.value == true && (checkedDaily.value == true || checkedWeekly.value == true || checkedMonthly.value == true)) SimpleDateFormat("dd/MM/yyyy").parse(textDateStart.value).time else null,
//                if (checkedRangeEnd.value == true && (checkedDaily.value == true || checkedWeekly.value == true || checkedMonthly.value == true)) SimpleDateFormat("dd/MM/yyyy").parse(textDateEnd.value).time else null,
//                repeatType,
//                false,
//                if (checkedRemind.value == true) textRemind.value!!.toInt() else null
//            )
        minDateInMillisStart.value = calStartDate.timeInMillis
        textEditName.value = item.name
        textEditDescription.value = if (item.description != null) item.description else ""
        visibleEditTimes.value = false
        if (item.dueTime != null) {
            checkedEditDue.value = true
        } else {
            checkedEditDue.value = false
        }
        if (item.date != null) {
            checkedEditItemDate.value = true
            visibleEditTimes.value = true
        } else {
            checkedEditItemDate.value = false
        }
        if (item.timeStart != null) {
            checkedEditItemStart.value = true
        } else {
            checkedEditItemStart.value = false
        }
        if (item.timeEnd != null) {
            checkedEditItemEnd.value = true
        } else {
            checkedEditItemEnd.value = false
        }
        if (item.rangeStart != null) {
            checkedEditRangeStart.value = true
            visibleEditTimes.value = true
        } else {
            checkedEditRangeStart.value = false
        }
        if (item.rangeEnd != null) {
            checkedEditRangeEnd.value = true
            visibleEditTimes.value = true
        } else {
            checkedEditRangeEnd.value = false
        }
        textEditDueTime.value = if (item.dueTime != null) SimpleDateFormat("HH:mm").format(item.dueTime) else SimpleDateFormat("HH:mm").format(minDateInMillisStart.value)
        textEditDueDate.value = if (item.dueDate != null) SimpleDateFormat("dd/MM/yyyy").format(item.dueDate) else SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisStart.value)
        textEditItemDate.value = if (item.date != null) SimpleDateFormat("dd/MM/yyyy").format(item.date) else SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisStart.value)
        textEditItemStart.value = if (item.timeStart != null) SimpleDateFormat("HH:mm").format(item.timeStart) else "00:00"
        textEditItemEnd.value = if (item.timeEnd != null) SimpleDateFormat("HH:mm").format(item.timeEnd) else "00:00"
        if (item.repeatType == 1) {
            checkedEditDaily.value = true
            visibleEditRanges.value = true
        } else if (item.repeatType == 2) {
            checkedEditWeekly.value = true
            visibleEditRanges.value = true
        } else if (item.repeatType == 3) {
            checkedEditMonthly.value = true
            visibleEditRanges.value = true
        } else {
            visibleEditRanges.value = false
            checkedEditDaily.value = false
            checkedEditWeekly.value = false
            checkedEditMonthly.value = false
        }
        textEditDateStart.value = if (item.rangeStart != null) SimpleDateFormat("dd/MM/yyyy").format(item.rangeStart) else SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisStart.value)
        textEditDateEnd.value = if (item.rangeEnd != null) SimpleDateFormat("dd/MM/yyyy").format(item.rangeEnd) else ""
        if (item.rangeEnd == null) {
            Log.i("Debug", "Range end not there")
            calcMinEndDate()
        }
        // TODO update all info to match the item
        if (checkedEditItemStart.value == true) {
            visibleEditRemind.value = true
        } else {
            visibleEditRemind.value = false
        }
    }

    fun remindVisibility(vis: Int): Boolean {
        var v1 = 0
        var v2 = 0
        if (checkedEditItemStart.value != true) v1 = 0 else v1 = 1
        if (vis == View.VISIBLE) v2 = 1 else v2 = 0
        Log.i("Debug", "min: ${min(v1, v2)} and ${checkedEditItemStart.value}")
        if (min(v1, v2) == 0) return false else return true
    }

    fun editItemFinish() {
        visibleEdit.value = false
    }

    fun calcMinEndTime(hour: Int? = null, minute: Int? = null, source: Int) {
        if (checkedEditItemStart.value == true) {
            if (source == 1) {
                val tempCal = Calendar.getInstance()
                tempCal.time = SimpleDateFormat("HH:mm").parse(textEditItemStart.value)
                minEndTime.value = tempCal.get(Calendar.HOUR_OF_DAY) * 60 + tempCal.get(Calendar.MINUTE) + 1
                tempCal.set(Calendar.MINUTE, tempCal.get(Calendar.MINUTE) + 1)
                if (minEndTime.value!! > hour!! * 60 + minute!!) {
                    textEditItemEnd.value = SimpleDateFormat("HH:mm").format(tempCal.time)
                }
            } else if (source == 0) {
                minEndTime.value = hour!! * 60 + minute!! + 1
                val tempCal = Calendar.getInstance()
                tempCal.time = SimpleDateFormat("HH:mm").parse(textEditItemEnd.value)
                if (minEndTime.value!! > tempCal.get(Calendar.HOUR_OF_DAY) * 60 + tempCal.get(Calendar.MINUTE)) {
                    tempCal.time = SimpleDateFormat("HH:mm").parse("${hour}:${minute + 1}")
                    textEditItemEnd.value = SimpleDateFormat("HH:mm").format(tempCal.time)
                }
            } else {
                val tempCal = Calendar.getInstance()
                tempCal.time = SimpleDateFormat("HH:mm").parse(textEditItemStart.value)
                minEndTime.value = tempCal.get(Calendar.HOUR_OF_DAY) * 60 + tempCal.get(Calendar.MINUTE) + 1
                tempCal.set(Calendar.MINUTE, tempCal.get(Calendar.MINUTE) + 1)
                val tempCal2 = Calendar.getInstance()
                tempCal2.time = SimpleDateFormat("HH:mm").parse(textEditItemEnd.value)
                if (minEndTime.value!! > tempCal2.get(Calendar.HOUR_OF_DAY) * 60 + tempCal2.get(Calendar.MINUTE)) {
                    textEditItemEnd.value = SimpleDateFormat("HH:mm").format(tempCal.time)
                }
            }
        }
    }

    fun calcMinEndDate() {
        if (checkedEditDaily.value == true) {
            minDateInMillisEnd.value = calStartDate.timeInMillis + 86400000
            if (calEndDate.timeInMillis < minDateInMillisEnd.value!!) {
                calEndDate.timeInMillis = minDateInMillisEnd.value!!
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(calEndDate.time)
            }
        } else if (checkedEditWeekly.value == true) {
            minDateInMillisEnd.value = calStartDate.timeInMillis + 604800000
            if (calEndDate.timeInMillis < minDateInMillisEnd.value!!) {
                calEndDate.timeInMillis = minDateInMillisEnd.value!!
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(calEndDate.time)
            }
        } else if (checkedEditMonthly.value == true) {
            val tempCal = Calendar.getInstance()
            tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditDateStart.value)
            tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse("${tempCal.get(Calendar.DAY_OF_MONTH)}/${tempCal.get(Calendar.MONTH) + 2}/${tempCal.get(Calendar.YEAR)}")
            minDateInMillisEnd.value = tempCal.timeInMillis
            if (calEndDate.timeInMillis < tempCal.timeInMillis) {
                calEndDate.timeInMillis = tempCal.timeInMillis
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(calEndDate.time)
            }
        }
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

    fun roundEndDate(year: Int, month: Int, day: Int) {
        if (checkedEditWeekly.value == true) {
            val tempCal = Calendar.getInstance()
            val tempCal2 = Calendar.getInstance()
            tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse("$day/$month/$year")
            tempCal.add(Calendar.MONTH, 1)
            if (checkedEditRangeStart.value == true) {
                tempCal2.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditDateStart.value)
                while (tempCal.get(Calendar.DAY_OF_WEEK) != tempCal2.get(Calendar.DAY_OF_WEEK)) {
                    tempCal.add(Calendar.DAY_OF_MONTH, -1)
                }
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(tempCal.time)
            } else {
                while (tempCal.get(Calendar.DAY_OF_WEEK) != tempCal2.get(Calendar.DAY_OF_WEEK)) {
                    tempCal.add(Calendar.DAY_OF_MONTH, -1)
                }
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(tempCal.time)
            }
        } else if (checkedEditMonthly.value == true) {
            val tempCal = Calendar.getInstance()
            val tempCal2 = Calendar.getInstance()
            tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse("$day/$month/$year")
            tempCal.add(Calendar.MONTH, 1)
            if (checkedEditRangeStart.value == true) {
                tempCal2.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditDateStart.value)
                while (tempCal.get(Calendar.DAY_OF_MONTH) != tempCal2.get(Calendar.DAY_OF_MONTH)) {
                    tempCal.add(Calendar.DAY_OF_MONTH, -1)
                }
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(tempCal.time)
            } else {
                while (tempCal.get(Calendar.DAY_OF_MONTH) != tempCal2.get(Calendar.DAY_OF_MONTH)) {
                    tempCal.add(Calendar.DAY_OF_MONTH, -1)
                }
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(tempCal.time)
            }
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
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }

    fun getGrouped(parentId: Long): LiveData<List<ItemInfo>> {
        return itemDao.getGroupedItems(parentId)
    }
}