package com.example.todolist

import android.app.Application
import android.icu.util.Calendar
import android.util.Log
import android.view.View
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.todolist.db.GroupInfo
import com.example.todolist.db.ItemDao
import com.example.todolist.db.ItemInfo
import com.example.todolist.db.TodoDao
import com.example.todolist.workers.SingleNotificationWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.math.min

class AddItemFragmentViewModel(private val application: Application, private val itemDao: ItemDao, private val todoDao: TodoDao) : AndroidViewModel(application) {
//    TODO implement factory for todoDao and itemDao
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
    val clickableTimes = MutableLiveData<Boolean>()
    val parentGroup = MutableLiveData<Long>()
    val calStartDate = Calendar.getInstance()
    val calDate = Calendar.getInstance()
    val calEndDate = Calendar.getInstance()
    val calDue = Calendar.getInstance()
    private lateinit var workManager: WorkManager
    lateinit var items: LiveData<List<ItemInfo>>

    init {
        workManager = WorkManager.getInstance(application)
        minDateInMillisStart.value = calStartDate.timeInMillis
        calEndDate.add(Calendar.DAY_OF_YEAR, 1)
        minDateInMillisEnd.value = calEndDate.timeInMillis
        calDue.add(Calendar.MINUTE, 1)
        minDateInMillisDue.value = calDue.timeInMillis
        textName.value = ""
        textDescription.value = ""
        textDueTime.value = SimpleDateFormat("HH:mm").format(minDateInMillisDue.value)
        textDueDate.value = SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisDue.value)
        textItemDate.value = SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisStart.value)
        textDateStart.value = SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisStart.value)
        textDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisEnd.value)
        textItemStart.value = "00:00"
        textItemEnd.value = "00:00"
        textRemind.value = ""
        minEndTime.value = 0
        checkedDue.value = false
        checkedItemDate.value = false
        checkedItemStart.value = false
        checkedItemEnd.value = false
    }

    fun getGroupName(groupId: Long) {

    }

    fun calcMinEndDate() {
        if (checkedDaily.value == true) {
            minDateInMillisEnd.value = calStartDate.timeInMillis + 86400000
            if (calEndDate.timeInMillis < minDateInMillisEnd.value!!) {
                calEndDate.timeInMillis = minDateInMillisEnd.value!!
                textDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(calEndDate.time)
            }
        } else if (checkedWeekly.value == true) {
            minDateInMillisEnd.value = calStartDate.timeInMillis + 604800000
            if (calEndDate.timeInMillis < minDateInMillisEnd.value!!) {
                calEndDate.timeInMillis = minDateInMillisEnd.value!!
                textDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(calEndDate.time)
            }
        } else if (checkedMonthly.value == true) {
            val tempCal = Calendar.getInstance()
            tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse(textDateStart.value)
            tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse("${tempCal.get(Calendar.DAY_OF_MONTH)}/${tempCal.get(Calendar.MONTH) + 2}/${tempCal.get(Calendar.YEAR)}")
            minDateInMillisEnd.value = tempCal.timeInMillis
            if (calEndDate.timeInMillis < tempCal.timeInMillis) {
                calEndDate.timeInMillis = tempCal.timeInMillis
                textDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(calEndDate.time)
            }
        }
    }

    fun calcMinEndTime(hour: Int? = null, minute: Int? = null, source: Int) {
        if (checkedItemStart.value == true) {
            if (source == 1) {
                val tempCal = Calendar.getInstance()
                tempCal.time = SimpleDateFormat("HH:mm").parse(textItemStart.value)
                minEndTime.value = tempCal.get(Calendar.HOUR_OF_DAY) * 60 + tempCal.get(Calendar.MINUTE) + 1
                tempCal.set(Calendar.MINUTE, tempCal.get(Calendar.MINUTE) + 1)
                if (minEndTime.value!! > hour!! * 60 + minute!!) {
                    textItemEnd.value = SimpleDateFormat("HH:mm").format(tempCal.time)
                }
            } else if (source == 0) {
                minEndTime.value = hour!! * 60 + minute!! + 1
                val tempCal = Calendar.getInstance()
                tempCal.time = SimpleDateFormat("HH:mm").parse(textItemEnd.value)
                if (minEndTime.value!! > tempCal.get(Calendar.HOUR_OF_DAY) * 60 + tempCal.get(Calendar.MINUTE)) {
                    tempCal.time = SimpleDateFormat("HH:mm").parse("${hour}:${minute + 1}")
                    textItemEnd.value = SimpleDateFormat("HH:mm").format(tempCal.time)
                }
            } else {
                val tempCal = Calendar.getInstance()
                tempCal.time = SimpleDateFormat("HH:mm").parse(textItemStart.value)
                minEndTime.value = tempCal.get(Calendar.HOUR_OF_DAY) * 60 + tempCal.get(Calendar.MINUTE) + 1
                tempCal.set(Calendar.MINUTE, tempCal.get(Calendar.MINUTE) + 1)
                val tempCal2 = Calendar.getInstance()
                tempCal2.time = SimpleDateFormat("HH:mm").parse(textItemEnd.value)
                if (minEndTime.value!! > tempCal2.get(Calendar.HOUR_OF_DAY) * 60 + tempCal2.get(Calendar.MINUTE)) {
                    textItemEnd.value = SimpleDateFormat("HH:mm").format(tempCal.time)
                }
            }
        }
    }

    fun roundEndDate(year: Int, month: Int, day: Int) {
        if (checkedWeekly.value == true) {
            val tempCal = Calendar.getInstance()
            val tempCal2 = Calendar.getInstance()
            tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse("$day/$month/$year")
            tempCal.add(Calendar.MONTH, 1)
            if (checkedRangeStart.value == true) {
                tempCal2.time = SimpleDateFormat("dd/MM/yyyy").parse(textDateStart.value)
                while (tempCal.get(Calendar.DAY_OF_WEEK) != tempCal2.get(Calendar.DAY_OF_WEEK)) {
                    tempCal.add(Calendar.DAY_OF_MONTH, -1)
                }
                textDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(tempCal.time)
            } else {
                while (tempCal.get(Calendar.DAY_OF_WEEK) != tempCal2.get(Calendar.DAY_OF_WEEK)) {
                    tempCal.add(Calendar.DAY_OF_MONTH, -1)
                }
                textDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(tempCal.time)
            }
        } else if (checkedMonthly.value == true) {
            val tempCal = Calendar.getInstance()
            val tempCal2 = Calendar.getInstance()
            tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse("$day/$month/$year")
            tempCal.add(Calendar.MONTH, 1)
            if (checkedRangeStart.value == true) {
                tempCal2.time = SimpleDateFormat("dd/MM/yyyy").parse(textDateStart.value)
                while (tempCal.get(Calendar.DAY_OF_MONTH) != tempCal2.get(Calendar.DAY_OF_MONTH)) {
                    tempCal.add(Calendar.DAY_OF_MONTH, -1)
                }
                textDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(tempCal.time)
            } else {
                while (tempCal.get(Calendar.DAY_OF_MONTH) != tempCal2.get(Calendar.DAY_OF_MONTH)) {
                    tempCal.add(Calendar.DAY_OF_MONTH, -1)
                }
                textDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(tempCal.time)
            }
        }
    }

    fun checkAdd() : String {
        val tempCal = Calendar.getInstance()
        if (textName.value!! != "") {
            if (checkedDue.value == true) {
                if (!calDue.after(tempCal)) return "Due Date and Time Are Before Current Date and Time"
            }
            if (checkedItemDate.value == true) {
                if (checkedItemEnd.value == true && checkedItemStart.value == false) {
                    val tempCal2 = Calendar.getInstance()
                    tempCal2.time = calDate.time
                    tempCal2.set(Calendar.MINUTE, calStartDate.get(Calendar.MINUTE))
                    tempCal2.set(Calendar.HOUR_OF_DAY, calStartDate.get(Calendar.HOUR_OF_DAY))
                    if (!tempCal2.after(tempCal)) return "Date and End Time Are Before Current Date and Time"
                } else if (checkedItemStart.value == false && checkedItemEnd.value == false) {
                    val tempCal2 = Calendar.getInstance()
                    tempCal2.time = calDate.time
                    tempCal2.set(Calendar.MINUTE, 0)
                    tempCal2.set(Calendar.HOUR_OF_DAY, 0)
                    tempCal.set(Calendar.MINUTE, 0)
                    tempCal.set(Calendar.HOUR_OF_DAY, 0)
                    if (!tempCal2.after(tempCal)) return "Date Is Before Current Date"
                } else {
                    if (!calDate.after(tempCal)) return "Date and Time Are Before Current Date and Time"
                }
            }
            if (checkedDaily.value == true || checkedWeekly.value == true || checkedMonthly.value == true) {
                val tempCal2 = Calendar.getInstance()
                tempCal2.time = calStartDate.time
                tempCal2.set(Calendar.MINUTE, 0)
                tempCal2.set(Calendar.HOUR_OF_DAY, 0)
                tempCal.set(Calendar.MINUTE, 0)
                tempCal.set(Calendar.HOUR_OF_DAY, 0)
                if (tempCal.after(calStartDate)) return "Start Date Is Before Current Date"
            }
            if (checkedRange.value == true) {
                if (!textRemind.value!!.isDigitsOnly()) return "Invalid Reminder Time"
            }
            if (checkedRemind.value == true) {
                if (textRemind.value!!.isBlank() || textRemind.value == "") return "Reminder Time Cannot Be Blank"
                if (!textRemind.value!!.isDigitsOnly()) return "Reminder Time Is Not a Number"
            }
            return "Item added successfully"
        } else {
            return "Name is empty"
        }
    }

    fun remindVisibility(vis: Int): Int {
        var v1 = 0
        var v2 = 0
        if (checkedItemStart.value == false) v1 = 0 else v1 = 1
        if (vis == View.VISIBLE) v2 = 1 else v2 = 0
        if (min(v1, v2) == 0) return View.GONE else return View.VISIBLE
    }

    fun addItem() : String {
        val result = checkAdd()
        if (result == "Item added successfully") {
            var repeatType: Int?
            if (checkedDaily.value == true) {
                repeatType = 1
            } else if (checkedWeekly.value == true) {
                repeatType = 2
            } else if (checkedMonthly.value == true) {
                repeatType = 3
            } else {
                repeatType = null
            }
            Log.i("Debugging", checkedItemEnd.value.toString() + " Checking")
            Log.i("Debugging", (checkedItemEnd.value == true && checkedDaily.value == true || checkedWeekly.value == true || checkedMonthly.value == true || checkedItemDate.value == true).toString())
            viewModelScope.launch {
                val itemId = itemDao.insertItem(
                    ItemInfo(
                        0,
                        textName.value!!,
                        if (textDescription.value!!.isNotBlank()) textDescription.value!! else null,
                        parentGroup.value!!,
                        if (checkedDue.value == true) SimpleDateFormat("HH:mm").parse(textDueTime.value).time else null,
                        if (checkedDue.value == true) SimpleDateFormat("dd/MM/yyyy").parse(textDueDate.value).time else null,
                        if (checkedItemDate.value == true && checkedDaily.value != true && checkedWeekly.value != true && checkedMonthly.value != true) SimpleDateFormat("dd/MM/yyyy").parse(textItemDate.value).time else null,
                        if (checkedItemStart.value == true && (checkedDaily.value == true || checkedWeekly.value == true || checkedMonthly.value == true || checkedItemDate.value == true)) SimpleDateFormat("HH:mm").parse(textItemStart.value).time else null,
                        if (checkedItemEnd.value == true && (checkedDaily.value == true || checkedWeekly.value == true || checkedMonthly.value == true || checkedItemDate.value == true)) SimpleDateFormat("HH:mm").parse(textItemEnd.value).time else null,
                        if (checkedRangeStart.value == true && (checkedDaily.value == true || checkedWeekly.value == true || checkedMonthly.value == true)) SimpleDateFormat("dd/MM/yyyy").parse(textDateStart.value).time else null,
                        if (checkedRangeEnd.value == true && (checkedDaily.value == true || checkedWeekly.value == true || checkedMonthly.value == true)) SimpleDateFormat("dd/MM/yyyy").parse(textDateEnd.value).time else null,
                        repeatType,
                        false,
                        if (checkedRemind.value == true) textRemind.value!!.toInt() else null
                    )
                )
                if (checkedRemind.value == true) {
                    val groupName = todoDao.getGroupById(parentGroup.value!!).title
                    when (repeatType) {
                        null -> {
//                            january 1, 1970 00:00:00 GMT+00:00 TODO fix this problem
                            Log.i("Debug", "${SimpleDateFormat("dd/MM/yyyy").parse(textItemDate.value).time + SimpleDateFormat("HH:mm").parse(textItemStart.value).time - (textRemind.value!!.toInt() * 60000)}")
                            if (checkedItemDate.value == true) {
                                if (checkedItemStart.value == true) {
                                    val request =
                                        OneTimeWorkRequestBuilder<SingleNotificationWorker>()
                                            .setInputData(
                                                workDataOf(
                                                    SingleNotificationWorker.TODO_NAME to textName.value,
                                                    SingleNotificationWorker.GROUP_NAME to groupName,
                                                    SingleNotificationWorker.TODO_TIME to textItemStart.value,
                                                    SingleNotificationWorker.TODO_REMIND to textRemind.value,
                                                    SingleNotificationWorker.TODO_DESCRIPTION to textDescription.value,
                                                    SingleNotificationWorker.TODO_ID to itemId
                                                )
                                            )
                                            .setInitialDelay(
                                                SimpleDateFormat("dd/MM/yyyy").parse(textItemDate.value).time + SimpleDateFormat("HH:mm").parse(textItemStart.value).time - (textRemind.value!!.toInt() * 60000) - System.currentTimeMillis(),
                                                TimeUnit.MILLISECONDS
                                            )
                                            .build()
                                    workManager.enqueueUniqueWork(
                                        itemId.toString(),
                                        ExistingWorkPolicy.REPLACE,
                                        request
                                    )
                                }
                            }
                        }

                        1 -> {

                        }
                    }
                }
                textName.value = ""
                textDescription.value = ""
                checkedDue.value = false
                checkedItemDate.value = false
                checkedItemStart.value = false
                checkedItemEnd.value = false
                textDueTime.value = SimpleDateFormat("HH:mm").format(minDateInMillisDue.value)
                textDueDate.value = SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisDue.value)
                textItemDate.value = SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisStart.value)
                textDateStart.value = SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisStart.value)
                textDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisEnd.value)
                textItemStart.value = "00:00"
                textItemEnd.value = "00:00"
                // calEndDate //reset this one TODO
                // calEndDate.add(Calendar.DAY_OF_YEAR, 1)
            }
            return "Item added successfully"
        } else {
            return result
        }
    }
}