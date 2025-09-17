package com.example.todolist

import android.app.Application
import android.util.Log
import android.view.View
import androidx.core.text.isDigitsOnly
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bumptech.glide.Glide.init
import com.example.todolist.DataStoreManager.dataStore
import com.example.todolist.db.GroupInfo
import com.example.todolist.db.ItemDao
import com.example.todolist.db.ItemInfo
import com.example.todolist.db.TodoDao
import com.example.todolist.workers.DailyNotificationWorker
import com.example.todolist.workers.MonthlyNotificationWorker
import com.example.todolist.workers.SingleNotificationWorker
import com.example.todolist.workers.WeeklyNotificationWorker
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.maps.model.LatLng
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.min

class CalendarFragmentViewModel(private val application: Application, private val itemDao: ItemDao, private val todoDao: TodoDao) : ViewModel() {
    val groups = MutableLiveData<List<GroupInfo>>()
    val allItems = MutableLiveData<List<ItemInfo>>()
    val showItems = MutableLiveData<List<ItemInfo>>()
    val selectedDay = MutableLiveData<Int>()
    val groupColours = MutableLiveData<Map<Long, Int>>()
    val selectedCal = Calendar.getInstance()
    val weekDays = MutableLiveData<List<Int>>()
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
    val textEditParentGroup = MutableLiveData<String>()
    val textParentGroup = MutableLiveData<String>()
    val textMonthYear = MutableLiveData<String>()
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
    val checkedEditLocation = MutableLiveData<Boolean>()
    val minDateInMillisStart = MutableLiveData<Long>()
    val minDateInMillisEnd = MutableLiveData<Long>()
    val minDateInMillisDue = MutableLiveData<Long>()
    val minEndTime = MutableLiveData<Int>()
    val calStartDate = android.icu.util.Calendar.getInstance()
    val calDate = android.icu.util.Calendar.getInstance()
    val calEndDate = android.icu.util.Calendar.getInstance()
    val calDue = android.icu.util.Calendar.getInstance()
    val editingItem = MutableLiveData<ItemInfo?>()
    val selectedGroupId = MutableLiveData<Long>()
    val selectedNewGroupId = MutableLiveData<Long>()
    val visibleEditRanges = MutableLiveData<Boolean>()
    val visibleEditTimes = MutableLiveData<Boolean>()
    val visibleEditRemind = MutableLiveData<Boolean>()
    val locationRemindManager = MutableLiveData<EditLocationRemindCalendarManager>()
    val locationIds = MutableLiveData<List<List<Any>>>()
    val toastText = MutableLiveData<String>()
    val visibleLocationHint = MutableLiveData<Boolean>()
    private val dataStore = application.dataStore
    private val moshi = Moshi.Builder().build()
    private val locationIdsJsonAdapter = moshi.adapter<List<List<Any>>>(Types.newParameterizedType(List::class.java, Types.newParameterizedType(List::class.java, Any::class.java)))
    private var workManager: WorkManager

    init {
        workManager = WorkManager.getInstance(application)
        locationIds.value = listOf()
        checkedEditDue.value = false
        checkedEditItemDate.value = false
        checkedEditItemStart.value = false
        checkedEditItemEnd.value = false
        visibleEditRanges.value = false
        visibleEditTimes.value = false
        minDateInMillisStart.value = calStartDate.timeInMillis
        minDateInMillisEnd.value = calEndDate.timeInMillis
        minDateInMillisDue.value = calDue.timeInMillis
        minEndTime.value = 0
        checkedEditLocation.value = false
    }

    fun deleteItem(item: ItemInfo) {
        viewModelScope.launch {
            Log.i("Debug", "Deleting item: ${item.name} with id: ${item.id}")
            workManager.cancelUniqueWork(item.id.toString())
            itemDao.deleteItem(item)
            allItems.postValue(itemDao.getItemsList())
            groups.postValue(todoDao.getItemsList())
        }
    }

    fun editItemAppear(item: ItemInfo) {
        selectedGroupId.value = item.group
        editingItem.value = item
        visibleEdit.value = true
        minDateInMillisStart.value = calStartDate.timeInMillis
        textEditName.value = item.name
        textEditDescription.value = if (item.description != null) item.description else ""
        viewModelScope.launch {
            textEditParentGroup.value = todoDao.getGroupById(item.group).title
        }
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
        val tempCal = android.icu.util.Calendar.getInstance()
        tempCal.time = SimpleDateFormat("HH:mm").parse(textEditDueTime.value)
        calDue.set(android.icu.util.Calendar.HOUR_OF_DAY, tempCal.get(android.icu.util.Calendar.HOUR_OF_DAY))
        calDue.set(android.icu.util.Calendar.MINUTE, tempCal.get(android.icu.util.Calendar.MINUTE))
        tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditDueDate.value)
        calDue.set(android.icu.util.Calendar.DAY_OF_MONTH, tempCal.get(android.icu.util.Calendar.DAY_OF_MONTH))
        calDue.set(android.icu.util.Calendar.MONTH, tempCal.get(android.icu.util.Calendar.MONTH))
        calDue.set(android.icu.util.Calendar.YEAR, tempCal.get(android.icu.util.Calendar.YEAR))
        tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditItemDate.value)
        calDate.set(android.icu.util.Calendar.DAY_OF_MONTH, tempCal.get(android.icu.util.Calendar.DAY_OF_MONTH))
        calDate.set(android.icu.util.Calendar.MONTH, tempCal.get(android.icu.util.Calendar.MONTH))
        calDate.set(android.icu.util.Calendar.YEAR, tempCal.get(android.icu.util.Calendar.YEAR))
        tempCal.time = SimpleDateFormat("HH:mm").parse(textEditItemStart.value)
        calDate.set(android.icu.util.Calendar.HOUR_OF_DAY, tempCal.get(android.icu.util.Calendar.HOUR_OF_DAY))
        calDate.set(android.icu.util.Calendar.MINUTE, tempCal.get(android.icu.util.Calendar.MINUTE))
        if (item.repeatType == 1) {
            checkedEditDaily.value = true
            visibleEditRanges.value = true
            visibleEditTimes.value = true
        } else if (item.repeatType == 2) {
            checkedEditWeekly.value = true
            visibleEditRanges.value = true
            visibleEditTimes.value = true
        } else if (item.repeatType == 3) {
            checkedEditMonthly.value = true
            visibleEditRanges.value = true
            visibleEditTimes.value = true
        } else {
            visibleEditRanges.value = false
            checkedEditDaily.value = false
            checkedEditWeekly.value = false
            checkedEditMonthly.value = false
            tempCal.time = SimpleDateFormat("HH:mm").parse(textEditItemEnd.value)
            calStartDate.set(android.icu.util.Calendar.HOUR_OF_DAY, tempCal.get(android.icu.util.Calendar.HOUR_OF_DAY))
            calStartDate.set(android.icu.util.Calendar.MINUTE, tempCal.get(android.icu.util.Calendar.MINUTE))
        }
        textEditDateStart.value = if (item.rangeStart != null) SimpleDateFormat("dd/MM/yyyy").format(item.rangeStart) else SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisStart.value)
        textEditDateEnd.value = if (item.rangeEnd != null) SimpleDateFormat("dd/MM/yyyy").format(item.rangeEnd) else ""
        tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditDateStart.value)
        calStartDate.set(android.icu.util.Calendar.DAY_OF_MONTH, tempCal.get(android.icu.util.Calendar.DAY_OF_MONTH))
        calStartDate.set(android.icu.util.Calendar.MONTH, tempCal.get(android.icu.util.Calendar.MONTH))
        calStartDate.set(android.icu.util.Calendar.YEAR, tempCal.get(android.icu.util.Calendar.YEAR))
        if (item.rangeEnd == null) {
            Log.i("Debug", "Range end not there")
            calcMinEndDate()
        }
        if (checkedEditItemStart.value == true) {
            visibleEditRemind.value = true
        } else {
            visibleEditRemind.value = false
        }
        checkedEditRemind.value = if (item.remind != null) true else false
        if (item.remind != null) {
            textEditRemind.value = item.remind.toString()
        }
        checkedEditLocation.value = if (item.locationIds != null) true else false
        if (item.locationIds != null) {
            val savedLocations = locationIdsJsonAdapter.fromJson(item.locationIds)
            for (location in savedLocations!!) {
                locationRemindManager.value!!.addSavedItem(location[0] as String, (location[1] as Double).toInt())
            }
            Log.d("Debugging", "$locationIds")
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

    fun checkEdit(): String {
        val tempCal = android.icu.util.Calendar.getInstance()
        if (textEditName.value!! != "") {
            if (checkedEditDue.value == true) {
                if (!calDue.after(tempCal)) return "Due Date and Time Are Before Current Date and Time"
            }
            if (checkedEditItemDate.value == true) {
                if (checkedEditItemEnd.value == true && checkedEditItemStart.value == false) {
                    val tempCal2 = android.icu.util.Calendar.getInstance()
                    tempCal2.time = calDate.time
                    tempCal2.set(android.icu.util.Calendar.MINUTE, calStartDate.get(android.icu.util.Calendar.MINUTE))
                    tempCal2.set(android.icu.util.Calendar.HOUR_OF_DAY, calStartDate.get(android.icu.util.Calendar.HOUR_OF_DAY))
                    if (!tempCal2.after(tempCal)) return "Date and End Time Are Before Current Date and Time"
                } else if (checkedEditItemStart.value == false && checkedEditItemEnd.value == false) {
                    val tempCal2 = android.icu.util.Calendar.getInstance()
                    tempCal2.time = calDate.time
                    tempCal2.set(android.icu.util.Calendar.MINUTE, 0)
                    tempCal2.set(android.icu.util.Calendar.HOUR_OF_DAY, 0)
                    tempCal.set(android.icu.util.Calendar.MINUTE, 0)
                    tempCal.set(android.icu.util.Calendar.HOUR_OF_DAY, 0)
                    if (!tempCal2.after(tempCal)) return "Date Is Before Current Date"
                } else {
                    if (!calDate.after(tempCal)) return "Date and Time Are Before Current Date and Time"
                }
            }
            if (checkedEditDaily.value == true || checkedEditWeekly.value == true || checkedEditMonthly.value == true) {
                val tempCal2 = android.icu.util.Calendar.getInstance()
                tempCal2.time = calStartDate.time
                tempCal2.set(android.icu.util.Calendar.MINUTE, 0)
                tempCal2.set(android.icu.util.Calendar.HOUR_OF_DAY, 0)
                tempCal.set(android.icu.util.Calendar.MINUTE, 0)
                tempCal.set(android.icu.util.Calendar.HOUR_OF_DAY, 0)
                if (tempCal.after(calStartDate) && checkedEditRangeStart.value == true) return "Start Date Is Before Current Date"
            }
            if (checkedEditRemind.value == true) {
                if (textEditRemind.value!!.isBlank() || textEditRemind.value == "") return "Reminder Time Cannot Be Blank"
                if (!textEditRemind.value!!.isDigitsOnly()) return "Reminder Time Is Not a Number"
            }
            return "Item edited successfully"
        } else {
            return "Name is empty"
        }
    }

    fun reinitializeGeofencing(): GeofencingRequest {
        val geofenceList: MutableList<Geofence> = mutableListOf()
        for (location in locationIds.value!!) {
            geofenceList.add(
                Geofence.Builder()
                    .setRequestId(editingItem.value!!.id.toString() + " " + location[0] as String)
                    .setCircularRegion(
                        (location[2] as LatLng).latitude,
                        (location[2] as LatLng).longitude,
                        ((location[1] as Int) * 1000).toFloat()
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                    .setLoiteringDelay(10000)
                    .build()
            )
        }
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL or GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()
        locationIds.value = listOf()
        locationRemindManager.value!!.clearOnFinish()
        checkedEditLocation.value = false
        return geofencingRequest
    }

    fun editItemFinish(): String {
        val result = checkEdit()
        visibleEdit.value = false
        if (result == "Item edited successfully") {
            var repeatType: Int?
            if (checkedEditDaily.value == true) {
                repeatType = 1
            } else if (checkedEditWeekly.value == true) {
                repeatType = 2
            } else if (checkedEditMonthly.value == true) {
                repeatType = 3
            } else {
                repeatType = null
            }
            workManager.cancelUniqueWork(editingItem.value!!.id.toString())
            viewModelScope.launch {
                Log.d("Debugging", "${editingItem.value!!.group}")
                val itemId = itemDao.editItem(
                    ItemInfo(
                        editingItem.value!!.id,
                        textEditName.value!!,
                        if (textEditDescription.value!!.isNotBlank()) textEditDescription.value!! else null,
                        selectedGroupId.value!!,
                        if (checkedEditDue.value == true) SimpleDateFormat("HH:mm").parse(textEditDueTime.value).time else null,
                        if (checkedEditDue.value == true) SimpleDateFormat("dd/MM/yyyy").parse(textEditDueDate.value).time else null,
                        if (checkedEditItemDate.value == true && checkedEditDaily.value != true && checkedEditWeekly.value != true && checkedEditMonthly.value != true) SimpleDateFormat("dd/MM/yyyy").parse(textEditItemDate.value).time else null,
                        if (checkedEditItemStart.value == true && (checkedEditDaily.value == true || checkedEditWeekly.value == true || checkedEditMonthly.value == true || checkedEditItemDate.value == true)) SimpleDateFormat("HH:mm").parse(textEditItemStart.value).time else null,
                        if (checkedEditItemEnd.value == true && (checkedEditDaily.value == true || checkedEditWeekly.value == true || checkedEditMonthly.value == true || checkedEditItemDate.value == true)) SimpleDateFormat("HH:mm").parse(textEditItemEnd.value).time else null,
                        if (checkedEditRangeStart.value == true && (checkedEditDaily.value == true || checkedEditWeekly.value == true || checkedEditMonthly.value == true)) SimpleDateFormat("dd/MM/yyyy").parse(textEditDateStart.value).time else null,
                        if (checkedEditRangeEnd.value == true && (checkedEditDaily.value == true || checkedEditWeekly.value == true || checkedEditMonthly.value == true)) SimpleDateFormat("dd/MM/yyyy").parse(textEditDateEnd.value).time else null,
                        repeatType,
                        false,
                        if (checkedEditRemind.value == true) textEditRemind.value!!.toInt() else null,
                        if (checkedEditLocation.value == true && !locationIds.value!!.isEmpty()) locationIdsJsonAdapter.toJson(locationIds.value) else null
                    )
                )
                if (checkedEditRemind.value == true) {
                    val groupName = todoDao.getGroupById(editingItem.value!!.group).title
                    when (repeatType) {
                        null -> {
                            val calendar = android.icu.util.Calendar.getInstance()
                            val timeCalendar = android.icu.util.Calendar.getInstance()
                            calendar.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditItemDate.value)
                            timeCalendar.time = SimpleDateFormat("HH:mm").parse(textEditItemStart.value)
                            calendar.set(
                                android.icu.util.Calendar.HOUR_OF_DAY, timeCalendar.get(
                                    android.icu.util.Calendar.HOUR_OF_DAY))
                            calendar.set(android.icu.util.Calendar.MINUTE, timeCalendar.get(android.icu.util.Calendar.MINUTE))
                            if (checkedEditItemDate.value == true) {
                                if (checkedEditItemStart.value == true) {
                                    val request =
                                        OneTimeWorkRequestBuilder<SingleNotificationWorker>()
                                            .setInputData(
                                                workDataOf(
                                                    SingleNotificationWorker.TODO_NAME to textEditName.value,
                                                    SingleNotificationWorker.GROUP_NAME to groupName,
                                                    SingleNotificationWorker.TODO_TIME to textEditItemStart.value,
                                                    SingleNotificationWorker.TODO_REMIND to textEditRemind.value,
                                                    SingleNotificationWorker.TODO_DESCRIPTION to textEditDescription.value,
                                                    SingleNotificationWorker.TODO_ID to editingItem.value!!.id.toString()
                                                )
                                            )
                                            .setInitialDelay(
                                                calendar.timeInMillis - ((textEditRemind.value!!.toInt()) * 60000) - System.currentTimeMillis(),
                                                TimeUnit.MILLISECONDS
                                            )
                                            .build()
                                    workManager.enqueueUniqueWork(
                                        editingItem.value!!.id.toString(),
                                        ExistingWorkPolicy.REPLACE,
                                        request
                                    )
                                }
                            }
                        }
                        1 -> {
                            val startCalendar = android.icu.util.Calendar.getInstance()
                            val endCalendar = android.icu.util.Calendar.getInstance()
                            val timeCalendar = android.icu.util.Calendar.getInstance()
                            startCalendar.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditDateStart.value)
                            timeCalendar.time = SimpleDateFormat("HH:mm").parse(textEditItemStart.value)
                            startCalendar.set(
                                android.icu.util.Calendar.HOUR_OF_DAY,
                                timeCalendar.get(android.icu.util.Calendar.HOUR_OF_DAY)
                            )
                            startCalendar.set(
                                android.icu.util.Calendar.MINUTE, timeCalendar.get(
                                    android.icu.util.Calendar.MINUTE))
                            if (checkedEditRangeStart.value == true) {
                                if (checkedEditItemStart.value == true) {
                                    var numRepeats = -1L
                                    if (checkedEditRangeEnd.value == true) {
                                        endCalendar.time =
                                            SimpleDateFormat("dd/MM/yyyy").parse(textEditDateEnd.value)
                                        endCalendar.set(
                                            android.icu.util.Calendar.HOUR_OF_DAY,
                                            timeCalendar.get(android.icu.util.Calendar.HOUR_OF_DAY)
                                        )
                                        endCalendar.set(
                                            android.icu.util.Calendar.MINUTE,
                                            timeCalendar.get(android.icu.util.Calendar.MINUTE)
                                        )
                                        numRepeats =
                                            (endCalendar.timeInMillis - startCalendar.timeInMillis) / 86400000 + 1
                                        Log.i("Debug", "$numRepeats")
                                    }
                                    dataStore.edit {
                                        it[longPreferencesKey(editingItem.value!!.id.toString().toString())] = numRepeats
                                    }
                                    var delay = startCalendar.timeInMillis - (textEditRemind.value!!.toInt() * 60000) - System.currentTimeMillis()
                                    if (delay < 0) {
                                        startCalendar.add(android.icu.util.Calendar.DAY_OF_YEAR, 7)
                                        delay = startCalendar.timeInMillis - (textEditRemind.value!!.toInt() * 60000) - System.currentTimeMillis()
                                        dataStore.edit {
                                            it[longPreferencesKey(editingItem.value!!.id.toString().toString())] = numRepeats - 1
                                        }
                                    }
                                    val request =
                                        PeriodicWorkRequestBuilder<DailyNotificationWorker>(24, TimeUnit.HOURS)
                                            .setInputData(
                                                workDataOf(
                                                    DailyNotificationWorker.TODO_NAME to textEditName.value,
                                                    DailyNotificationWorker.GROUP_NAME to groupName,
                                                    DailyNotificationWorker.TODO_TIME to textEditItemStart.value,
                                                    DailyNotificationWorker.TODO_REMIND to textEditRemind.value,
                                                    DailyNotificationWorker.TODO_DESCRIPTION to textEditDescription.value,
                                                    DailyNotificationWorker.TODO_ID to editingItem.value!!.id.toString()
                                                )
                                            )
                                            .setInitialDelay(
                                                delay,
                                                TimeUnit.MILLISECONDS
                                            )
                                            .build()
                                    workManager.enqueueUniquePeriodicWork(
                                        editingItem.value!!.id.toString(),
                                        ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                                        request
                                    )
                                }
                            }
                        }
                        2 -> {
                            val startCalendar = android.icu.util.Calendar.getInstance()
                            val endCalendar = android.icu.util.Calendar.getInstance()
                            val timeCalendar = android.icu.util.Calendar.getInstance()
                            startCalendar.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditDateStart.value)
                            timeCalendar.time = SimpleDateFormat("HH:mm").parse(textEditItemStart.value)
                            startCalendar.set(
                                android.icu.util.Calendar.HOUR_OF_DAY, timeCalendar.get(
                                    android.icu.util.Calendar.HOUR_OF_DAY))
                            startCalendar.set(
                                android.icu.util.Calendar.MINUTE, timeCalendar.get(
                                    android.icu.util.Calendar.MINUTE))
                            if (checkedEditRangeStart.value == true) {
                                if (checkedEditItemStart.value == true) {
                                    var numRepeats = -1L
                                    if (checkedEditRangeEnd.value == true) {
                                        endCalendar.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditDateEnd.value)
                                        endCalendar.set(
                                            android.icu.util.Calendar.HOUR_OF_DAY, timeCalendar.get(
                                                android.icu.util.Calendar.HOUR_OF_DAY))
                                        endCalendar.set(
                                            android.icu.util.Calendar.MINUTE, timeCalendar.get(
                                                android.icu.util.Calendar.MINUTE))
                                        numRepeats = (endCalendar.timeInMillis - startCalendar.timeInMillis) / 86400000 + 1
                                        Log.i("Debug", "$numRepeats")
                                    }
                                    dataStore.edit {
                                        it[longPreferencesKey(editingItem.value!!.id.toString())] = numRepeats
                                    }
                                    var delay = startCalendar.timeInMillis - (textEditRemind.value!!.toInt() * 60000) - System.currentTimeMillis()
                                    if (delay < 0) {
                                        startCalendar.add(android.icu.util.Calendar.DAY_OF_YEAR, 7)
                                        delay = startCalendar.timeInMillis - (textEditRemind.value!!.toInt() * 60000) - System.currentTimeMillis()
                                        dataStore.edit {
                                            it[longPreferencesKey(editingItem.value!!.id.toString())] = numRepeats - 1
                                        }
                                    }
                                    val request =
                                        PeriodicWorkRequestBuilder<WeeklyNotificationWorker>(7, TimeUnit.DAYS)
                                            .setInputData(
                                                workDataOf(
                                                    WeeklyNotificationWorker.TODO_NAME to textEditName.value,
                                                    WeeklyNotificationWorker.GROUP_NAME to groupName,
                                                    WeeklyNotificationWorker.TODO_TIME to textEditItemStart.value,
                                                    WeeklyNotificationWorker.TODO_DAY to intToDay(startCalendar.get(
                                                        android.icu.util.Calendar.DAY_OF_WEEK)),
                                                    WeeklyNotificationWorker.TODO_REMIND to textEditRemind.value,
                                                    WeeklyNotificationWorker.TODO_DESCRIPTION to textEditDescription.value,
                                                    WeeklyNotificationWorker.TODO_ID to editingItem.value!!.id.toString()
                                                )
                                            )
                                            .setInitialDelay(
                                                delay,
                                                TimeUnit.MILLISECONDS
                                            )
                                            .build()
                                    workManager.enqueueUniquePeriodicWork(
                                        editingItem.value!!.id.toString(),
                                        ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                                        request
                                    )
                                }
                            }
                        }
                        3 -> {
                            val startCalendar = android.icu.util.Calendar.getInstance()
                            val endCalendar = android.icu.util.Calendar.getInstance()
                            val timeCalendar = android.icu.util.Calendar.getInstance()
                            startCalendar.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditDateStart.value)
                            timeCalendar.time = SimpleDateFormat("HH:mm").parse(textEditItemStart.value)
                            startCalendar.set(
                                android.icu.util.Calendar.HOUR_OF_DAY, timeCalendar.get(
                                    android.icu.util.Calendar.HOUR_OF_DAY))
                            startCalendar.set(
                                android.icu.util.Calendar.MINUTE, timeCalendar.get(
                                    android.icu.util.Calendar.MINUTE))
                            if (checkedEditRangeStart.value == true) {
                                if (checkedEditItemStart.value == true) {
                                    var numRepeats = -1L
                                    if (checkedEditRangeEnd.value == true) {
                                        endCalendar.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditDateEnd.value)
                                        endCalendar.set(
                                            android.icu.util.Calendar.HOUR_OF_DAY, timeCalendar.get(
                                                android.icu.util.Calendar.HOUR_OF_DAY))
                                        endCalendar.set(
                                            android.icu.util.Calendar.MINUTE, timeCalendar.get(
                                                android.icu.util.Calendar.MINUTE))
                                        numRepeats = (endCalendar.get(android.icu.util.Calendar.YEAR) * 12 + endCalendar.get(
                                            android.icu.util.Calendar.MONTH) - (startCalendar.get(
                                            android.icu.util.Calendar.YEAR) * 12 + startCalendar.get(
                                            android.icu.util.Calendar.MONTH)) + 1).toLong()
                                        Log.i("Debug", "$numRepeats")
                                    }
                                    dataStore.edit {
                                        it[longPreferencesKey(editingItem.value!!.id.toString())] = numRepeats
                                    }
                                    var delay = startCalendar.timeInMillis - (textEditRemind.value!!.toInt() * 60000) - System.currentTimeMillis()
                                    if (delay < 0) {
                                        startCalendar.add(android.icu.util.Calendar.DAY_OF_YEAR, 7)
                                        delay = startCalendar.timeInMillis - (textEditRemind.value!!.toInt() * 60000) - System.currentTimeMillis()
                                        dataStore.edit {
                                            it[longPreferencesKey(editingItem.value!!.id.toString())] = numRepeats - 1
                                        }
                                    }
                                    val request =
                                        OneTimeWorkRequestBuilder<MonthlyNotificationWorker>()
                                            .setInputData(
                                                workDataOf(
                                                    MonthlyNotificationWorker.TODO_NAME to textEditName.value,
                                                    MonthlyNotificationWorker.GROUP_NAME to groupName,
                                                    MonthlyNotificationWorker.TODO_TIME to textEditItemStart.value,
                                                    MonthlyNotificationWorker.TODO_DAY to orderSuffix(startCalendar.get(
                                                        android.icu.util.Calendar.DAY_OF_MONTH)),
                                                    MonthlyNotificationWorker.TODO_REMIND to textEditRemind.value,
                                                    MonthlyNotificationWorker.TODO_DESCRIPTION to textEditDescription.value,
                                                    MonthlyNotificationWorker.TODO_ID to editingItem.value!!.id.toString()
                                                )
                                            )
                                            .setInitialDelay(
                                                delay,
                                                TimeUnit.MILLISECONDS
                                            )
                                            .build()
                                    workManager.enqueueUniqueWork(
                                        editingItem.value!!.id.toString(),
                                        ExistingWorkPolicy.REPLACE,
                                        request
                                    )
                                }
                            }
                        }
                    }
                }
                textEditName.value = ""
                textEditDescription.value = ""
                checkedEditDue.value = false
                checkedEditItemDate.value = false
                checkedEditItemStart.value = false
                checkedEditItemEnd.value = false
                textEditDueTime.value = SimpleDateFormat("HH:mm").format(minDateInMillisDue.value)
                textEditDueDate.value = SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisDue.value)
                textEditItemDate.value = SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisStart.value)
                textEditDateStart.value = SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisStart.value)
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisEnd.value)
                textEditItemStart.value = "00:00"
                textEditItemEnd.value = "00:00"
                allItems.postValue(itemDao.getItemsList())
                groups.postValue(todoDao.getItemsList())
            }
        }
        return result
    }

    fun intToDay(day: Int): String {
        return when (day) {
            android.icu.util.Calendar.SUNDAY -> "Sunday"
            android.icu.util.Calendar.MONDAY -> "Monday"
            android.icu.util.Calendar.TUESDAY -> "Tuesday"
            android.icu.util.Calendar.WEDNESDAY -> "Wednesday"
            android.icu.util.Calendar.THURSDAY -> "Thursday"
            android.icu.util.Calendar.FRIDAY -> "Friday"
            android.icu.util.Calendar.SATURDAY -> "Saturday"
            else -> "Error"
        }
    }

    fun orderSuffix(num: Int): String {
        return when (num) {
            1 -> "1st"
            2 -> "2nd"
            3 -> "3rd"
            else -> "${num}th"
        }
    }

    fun setCurrentDayInfo() {
        selectedCal.set(Calendar.HOUR_OF_DAY, 0)
        selectedCal.set(Calendar.MINUTE, 0)
        selectedCal.set(Calendar.SECOND, 0)
        selectedCal.set(Calendar.MILLISECOND, 0)
        selectedDay.value = selectedCal.get(Calendar.DAY_OF_WEEK)
    }

    fun updateDay() {
        selectedDay.value
    }

    fun getSelectedDayItems() {
        textMonthYear.value = SimpleDateFormat("MMMM yyyy").format(selectedCal.time)
        viewModelScope.launch {
            val colourMap = mutableMapOf<Long, Int>()
            val tempShowList = mutableListOf<ItemInfo>()
            val tempCal = Calendar.getInstance()
            Log.d("Debugging", "all items: ${allItems.value}")
            if (allItems.value != null) {
                Log.d("Debugging", "not null")
                for (item in allItems.value!!) {
                    if (item.date != null) {
                        tempCal.timeInMillis = item.date!!
                        if (tempCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR) && tempCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR)) {
                            Log.d("Debugging", "hihihiohio")
                            tempShowList.add(item)
                        }
                    } else if (item.repeatType != null) {
                        val tempStartCal = Calendar.getInstance()
                        val tempEndCal = Calendar.getInstance()
                        if (item.rangeStart != null) {
                            tempStartCal.timeInMillis = item.rangeStart!!
                        }
                        if (item.rangeEnd != null) {
                            tempEndCal.timeInMillis = item.rangeEnd!!
                        }
                        if (item.rangeStart != null) {
                            if (selectedCal.before(tempStartCal)) {
                                continue
                            }
                        }
                        if (item.rangeEnd != null) {
                            if (selectedCal.after(tempEndCal)) {
                                continue
                            }
                        }
                        when (item.repeatType) {
                            1 -> {
                                tempShowList.add(item)
                                Log.d("Debugging", "daily added")
                            }
                            2 -> {
                                val dayOfWeek = tempStartCal.get(Calendar.DAY_OF_WEEK)

                                if (dayOfWeek == selectedCal.get(Calendar.DAY_OF_WEEK)) {
                                    tempShowList.add(item)
                                    Log.d("Debugging", "weekly added")
                                }
                            }
                            3 -> {
                                val dayOfMonth = tempStartCal.get(Calendar.DAY_OF_MONTH)

                                if (dayOfMonth == selectedCal.get(Calendar.DAY_OF_MONTH)) {
                                    tempShowList.add(item)
                                    Log.d("Debugging", "monthly added")
                                }
                            }
                        }
                    }
                }
            }
            for (item in tempShowList) {
                colourMap[item.group] = todoDao.getGroupById(item.group).colour
            }
            groupColours.postValue(colourMap)
            showItems.postValue(tempShowList)
            Log.d("Debugging", "show list: $tempShowList")
            Log.d("Debugging", "${showItems.value}")
        }
    }

    fun roundEndDate(year: Int, month: Int, day: Int) {
        if (checkedEditWeekly.value == true) {
            val tempCal = android.icu.util.Calendar.getInstance()
            val tempCal2 = android.icu.util.Calendar.getInstance()
            tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse("$day/$month/$year")
            tempCal.add(android.icu.util.Calendar.MONTH, 1)
            if (checkedEditRangeStart.value == true) {
                tempCal2.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditDateStart.value)
                while (tempCal.get(android.icu.util.Calendar.DAY_OF_WEEK) != tempCal2.get(android.icu.util.Calendar.DAY_OF_WEEK)) {
                    tempCal.add(android.icu.util.Calendar.DAY_OF_MONTH, -1)
                }
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(tempCal.time)
            } else {
                while (tempCal.get(android.icu.util.Calendar.DAY_OF_WEEK) != tempCal2.get(android.icu.util.Calendar.DAY_OF_WEEK)) {
                    tempCal.add(android.icu.util.Calendar.DAY_OF_MONTH, -1)
                }
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(tempCal.time)
            }
        } else if (checkedEditMonthly.value == true) {
            val tempCal = android.icu.util.Calendar.getInstance()
            val tempCal2 = android.icu.util.Calendar.getInstance()
            tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse("$day/$month/$year")
            tempCal.add(android.icu.util.Calendar.MONTH, 1)
            if (checkedEditRangeStart.value == true) {
                tempCal2.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditDateStart.value)
                while (tempCal.get(android.icu.util.Calendar.DAY_OF_MONTH) != tempCal2.get(android.icu.util.Calendar.DAY_OF_MONTH)) {
                    tempCal.add(android.icu.util.Calendar.DAY_OF_MONTH, -1)
                }
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(tempCal.time)
            } else {
                while (tempCal.get(android.icu.util.Calendar.DAY_OF_MONTH) != tempCal2.get(android.icu.util.Calendar.DAY_OF_MONTH)) {
                    tempCal.add(android.icu.util.Calendar.DAY_OF_MONTH, -1)
                }
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(tempCal.time)
            }
        }
    }

    fun calcMinEndTime(hour: Int? = null, minute: Int? = null, source: Int) {
        if (checkedEditItemStart.value == true) {
            if (source == 1) {
                val tempCal = android.icu.util.Calendar.getInstance()
                tempCal.time = SimpleDateFormat("HH:mm").parse(textEditItemStart.value)
                minEndTime.value = tempCal.get(android.icu.util.Calendar.HOUR_OF_DAY) * 60 + tempCal.get(
                    android.icu.util.Calendar.MINUTE) + 1
                tempCal.set(android.icu.util.Calendar.MINUTE, tempCal.get(android.icu.util.Calendar.MINUTE) + 1)
                if (minEndTime.value!! > hour!! * 60 + minute!!) {
                    textEditItemEnd.value = SimpleDateFormat("HH:mm").format(tempCal.time)
                }
            } else if (source == 0) {
                minEndTime.value = hour!! * 60 + minute!! + 1
                val tempCal = android.icu.util.Calendar.getInstance()
                tempCal.time = SimpleDateFormat("HH:mm").parse(textEditItemEnd.value)
                if (minEndTime.value!! > tempCal.get(android.icu.util.Calendar.HOUR_OF_DAY) * 60 + tempCal.get(
                        android.icu.util.Calendar.MINUTE)) {
                    tempCal.time = SimpleDateFormat("HH:mm").parse("${hour}:${minute + 1}")
                    textEditItemEnd.value = SimpleDateFormat("HH:mm").format(tempCal.time)
                }
            } else {
                val tempCal = android.icu.util.Calendar.getInstance()
                tempCal.time = SimpleDateFormat("HH:mm").parse(textEditItemStart.value)
                minEndTime.value = tempCal.get(android.icu.util.Calendar.HOUR_OF_DAY) * 60 + tempCal.get(
                    android.icu.util.Calendar.MINUTE) + 1
                tempCal.set(android.icu.util.Calendar.MINUTE, tempCal.get(android.icu.util.Calendar.MINUTE) + 1)
                val tempCal2 = android.icu.util.Calendar.getInstance()
                tempCal2.time = SimpleDateFormat("HH:mm").parse(textEditItemEnd.value)
                if (minEndTime.value!! > tempCal2.get(android.icu.util.Calendar.HOUR_OF_DAY) * 60 + tempCal2.get(
                        android.icu.util.Calendar.MINUTE)) {
                    textEditItemEnd.value = SimpleDateFormat("HH:mm").format(tempCal.time)
                }
            }
        }
    }

    fun calcMinEndDate() {
        if (checkedEditDaily.value == true) {
            Log.d("Debugging", "why doesn't it wokr")
            minDateInMillisEnd.value = calStartDate.timeInMillis + 86400000
            Log.d("Debugging", "${SimpleDateFormat("dd/MM/yyyy").format(calEndDate.time)}")
            Log.d("Debugging", "${SimpleDateFormat("dd/MM/yyyy").format(minDateInMillisEnd.value)}")
            if (calEndDate.timeInMillis < minDateInMillisEnd.value!!) {
                Log.d("Debugging", "very weird")
                calEndDate.timeInMillis = minDateInMillisEnd.value!!
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(calEndDate.time)
            } else {
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(calEndDate.time)
            }
        } else if (checkedEditWeekly.value == true) {
            minDateInMillisEnd.value = calStartDate.timeInMillis + 604800000
            if (calEndDate.timeInMillis < minDateInMillisEnd.value!!) {
                calEndDate.timeInMillis = minDateInMillisEnd.value!!
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(calEndDate.time)
            }
        } else if (checkedEditMonthly.value == true) {
            val tempCal = android.icu.util.Calendar.getInstance()
            tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse(textEditDateStart.value)
            tempCal.time = SimpleDateFormat("dd/MM/yyyy").parse("${tempCal.get(android.icu.util.Calendar.DAY_OF_MONTH)}/${tempCal.get(
                android.icu.util.Calendar.MONTH) + 2}/${tempCal.get(android.icu.util.Calendar.YEAR)}")
            minDateInMillisEnd.value = tempCal.timeInMillis
            if (calEndDate.timeInMillis < tempCal.timeInMillis) {
                calEndDate.timeInMillis = tempCal.timeInMillis
                textEditDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(calEndDate.time)
            }
        }
    }

    fun itemChecked(itemId: Long, checked: Boolean) {
        viewModelScope.launch {
            val item = itemDao.getItem(itemId)

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
                    item.remind,
                    item.locationIds
                )
            )
        }
    }

    fun getWeekDays() {
        val tempCal = selectedCal.clone() as Calendar
        tempCal.add(Calendar.DAY_OF_MONTH, -(tempCal.get(Calendar.DAY_OF_WEEK) - 1))
        val days = mutableListOf<Int>()
        for (i in 0 until 7) {
            days.add(tempCal.get(Calendar.DAY_OF_MONTH))
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        weekDays.value = days
    }

    fun nextWeek() {
        selectedCal.add(Calendar.DAY_OF_MONTH, 7)
        textMonthYear.value = SimpleDateFormat("MMMM yyyy").format(selectedCal.time)
    }

    fun prevWeek() {
        selectedCal.add(Calendar.DAY_OF_MONTH, -7)
        textMonthYear.value = SimpleDateFormat("MMMM yyyy").format(selectedCal.time)
    }
}