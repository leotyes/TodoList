package com.example.todolist

import android.app.Application
import android.content.Context
import android.icu.util.Calendar
import android.util.Log
import android.view.View
import androidx.core.text.isDigitsOnly
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
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
import com.example.todolist.db.GroupInfo
import com.example.todolist.db.ItemDao
import com.example.todolist.db.ItemInfo
import com.example.todolist.db.TodoDao
import com.example.todolist.workers.DailyNotificationWorker
import com.example.todolist.workers.SingleNotificationWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.math.min
import com.example.todolist.DataStoreManager.dataStore
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.work.BackoffPolicy
import com.example.todolist.workers.MonthlyNotificationWorker
import com.example.todolist.workers.WeeklyNotificationWorker
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.time.DayOfWeek
import kotlin.time.Duration

class AddItemFragmentViewModel(private val application: Application, private val itemDao: ItemDao, private val todoDao: TodoDao) : AndroidViewModel(application) {
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
    val checkedLocation = MutableLiveData<Boolean>()
    val visibleLocationHint = MutableLiveData<Boolean>()
    val minDateInMillisStart = MutableLiveData<Long>()
    val minDateInMillisEnd = MutableLiveData<Long>()
    val minDateInMillisDue = MutableLiveData<Long>()
    val minEndTime = MutableLiveData<Int>()
    val parentGroup = MutableLiveData<Long>()
    val toastText = MutableLiveData<String>()
    val calStartDate = Calendar.getInstance()
    val calDate = Calendar.getInstance()
    val calEndDate = Calendar.getInstance()
    val calDue = Calendar.getInstance()
    val locationIds = MutableLiveData<List<List<Any>>>()
    private val dataStore = application.dataStore
    private lateinit var workManager: WorkManager
    lateinit var items: LiveData<List<ItemInfo>>
    private val moshi = Moshi.Builder().build()
    private val locationIdsJsonAdapter = moshi.adapter<List<List<Any>>>(Types.newParameterizedType(List::class.java, Types.newParameterizedType(List::class.java, Any::class.java)))

    init {
        workManager = WorkManager.getInstance(application)
        toastText.value = ""
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
        checkedLocation.value = false
        visibleLocationHint.value = true
        locationIds.value = listOf()
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

    fun intToDay(day: Int): String {
        return when (day) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
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

    fun initializeGeofencing(itemId: Long): GeofencingRequest {
        Log.d("Debugging", itemId.toString())
        val geofenceList: MutableList<Geofence> = mutableListOf()
        for (location in locationIds.value!!) {
            geofenceList.add(Geofence.Builder()
                .setRequestId(itemId.toString() + " " + location[0] as String)
                .setCircularRegion(
                    (location[2] as LatLng).latitude,
                    (location[2] as LatLng).longitude,
                    ((location[1] as Int) * 1000).toFloat()
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_ENTER)
                .setLoiteringDelay(60000)
                .build()
            )
        }
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL or GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()
        locationIds.value = listOf()
        checkedLocation.value = false
        return geofencingRequest
    }

    suspend fun addItem(): Pair<String, Long> {
        val result = checkAdd()
        if (result == "Item added successfully") {
            val locationIdsJson = locationIdsJsonAdapter.toJson(locationIds.value)
            var repeatType: Int?
            var returnItemId: Long = -1
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
//            viewModelScope.launch {
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
                    if (checkedRemind.value == true) textRemind.value!!.toInt() else null,
                    if (checkedLocation.value == true && !locationIds.value!!.isEmpty()) locationIdsJson else null
                )
            )
            returnItemId = itemId
            if (checkedRemind.value == true) {
                val groupName = todoDao.getGroupById(parentGroup.value!!).title
                when (repeatType) {
                    null -> {
                        val calendar = Calendar.getInstance()
                        val timeCalendar = Calendar.getInstance()
                        calendar.time = SimpleDateFormat("dd/MM/yyyy").parse(textItemDate.value)
                        timeCalendar.time = SimpleDateFormat("HH:mm").parse(textItemStart.value)
                        calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                        calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
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
                                            calendar.timeInMillis - ((textRemind.value!!.toInt()) * 60000) - System.currentTimeMillis(),
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
                        val startCalendar = Calendar.getInstance()
                        val endCalendar = Calendar.getInstance()
                        val timeCalendar = Calendar.getInstance()
                        startCalendar.time = SimpleDateFormat("dd/MM/yyyy").parse(textDateStart.value)
                        timeCalendar.time = SimpleDateFormat("HH:mm").parse(textItemStart.value)
                        startCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            timeCalendar.get(Calendar.HOUR_OF_DAY)
                        )
                        startCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                        if (checkedRangeStart.value == true) {
                            if (checkedItemStart.value == true) {
                                var numRepeats = -1L
                                if (checkedRangeEnd.value == true) {
                                    endCalendar.time =
                                        SimpleDateFormat("dd/MM/yyyy").parse(textDateEnd.value)
                                    endCalendar.set(
                                        Calendar.HOUR_OF_DAY,
                                        timeCalendar.get(Calendar.HOUR_OF_DAY)
                                    )
                                    endCalendar.set(
                                        Calendar.MINUTE,
                                        timeCalendar.get(Calendar.MINUTE)
                                    )
                                    numRepeats =
                                        (endCalendar.timeInMillis - startCalendar.timeInMillis) / 86400000 + 1
                                    Log.i("Debug", "$numRepeats")
                                }
                                dataStore.edit {
                                    it[longPreferencesKey(itemId.toString())] = numRepeats
                                }
                                var delay = startCalendar.timeInMillis - (textRemind.value!!.toInt() * 60000) - System.currentTimeMillis()
                                if (delay < 0) {
                                    startCalendar.add(Calendar.DAY_OF_YEAR, 7)
                                    delay = startCalendar.timeInMillis - (textRemind.value!!.toInt() * 60000) - System.currentTimeMillis()
                                    dataStore.edit {
                                        it[longPreferencesKey(itemId.toString())] = numRepeats - 1
                                    }
                                }
                                val request =
                                    PeriodicWorkRequestBuilder<DailyNotificationWorker>(24, TimeUnit.HOURS)
                                        .setInputData(
                                            workDataOf(
                                                DailyNotificationWorker.TODO_NAME to textName.value,
                                                DailyNotificationWorker.GROUP_NAME to groupName,
                                                DailyNotificationWorker.TODO_TIME to textItemStart.value,
                                                DailyNotificationWorker.TODO_REMIND to textRemind.value,
                                                DailyNotificationWorker.TODO_DESCRIPTION to textDescription.value,
                                                DailyNotificationWorker.TODO_ID to itemId
                                            )
                                        )
                                        .setInitialDelay(
                                            delay,
                                            TimeUnit.MILLISECONDS
                                        )
                                        .build()
                                workManager.enqueueUniquePeriodicWork(
                                    itemId.toString(),
                                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                                    request
                                )
                            }
                        }
                    }
                    2 -> {
                        val startCalendar = Calendar.getInstance()
                        val endCalendar = Calendar.getInstance()
                        val timeCalendar = Calendar.getInstance()
                        startCalendar.time = SimpleDateFormat("dd/MM/yyyy").parse(textDateStart.value)
                        timeCalendar.time = SimpleDateFormat("HH:mm").parse(textItemStart.value)
                        startCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                        startCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                        if (checkedRangeStart.value == true) {
                            if (checkedItemStart.value == true) {
                                var numRepeats = -1L
                                if (checkedRangeEnd.value == true) {
                                    endCalendar.time = SimpleDateFormat("dd/MM/yyyy").parse(textDateEnd.value)
                                    endCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                                    endCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                                    numRepeats = (endCalendar.timeInMillis - startCalendar.timeInMillis) / 86400000 + 1
                                    Log.i("Debug", "$numRepeats")
                                }
                                dataStore.edit {
                                    it[longPreferencesKey(itemId.toString())] = numRepeats
                                }
                                var delay = startCalendar.timeInMillis - (textRemind.value!!.toInt() * 60000) - System.currentTimeMillis()
                                if (delay < 0) {
                                    startCalendar.add(Calendar.DAY_OF_YEAR, 7)
                                    delay = startCalendar.timeInMillis - (textRemind.value!!.toInt() * 60000) - System.currentTimeMillis()
                                    dataStore.edit {
                                        it[longPreferencesKey(itemId.toString())] = numRepeats - 1
                                    }
                                }
                                val request =
                                    PeriodicWorkRequestBuilder<WeeklyNotificationWorker>(7, TimeUnit.DAYS)
                                        .setInputData(
                                            workDataOf(
                                                WeeklyNotificationWorker.TODO_NAME to textName.value,
                                                WeeklyNotificationWorker.GROUP_NAME to groupName,
                                                WeeklyNotificationWorker.TODO_TIME to textItemStart.value,
                                                WeeklyNotificationWorker.TODO_DAY to intToDay(startCalendar.get(Calendar.DAY_OF_WEEK)),
                                                WeeklyNotificationWorker.TODO_REMIND to textRemind.value,
                                                WeeklyNotificationWorker.TODO_DESCRIPTION to textDescription.value,
                                                WeeklyNotificationWorker.TODO_ID to itemId
                                            )
                                        )
                                        .setInitialDelay(
                                            delay,
                                            TimeUnit.MILLISECONDS
                                        )
                                        .build()
                                workManager.enqueueUniquePeriodicWork(
                                    itemId.toString(),
                                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                                    request
                                )
                            }
                        }
                    }
                    3 -> {
                        val startCalendar = Calendar.getInstance()
                        val endCalendar = Calendar.getInstance()
                        val timeCalendar = Calendar.getInstance()
                        startCalendar.time = SimpleDateFormat("dd/MM/yyyy").parse(textDateStart.value)
                        timeCalendar.time = SimpleDateFormat("HH:mm").parse(textItemStart.value)
                        startCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                        startCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                        if (checkedRangeStart.value == true) {
                            if (checkedItemStart.value == true) {
                                var numRepeats = -1L
                                if (checkedRangeEnd.value == true) {
                                    endCalendar.time = SimpleDateFormat("dd/MM/yyyy").parse(textDateEnd.value)
                                    endCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                                    endCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                                    numRepeats = (endCalendar.get(Calendar.YEAR) * 12 + endCalendar.get(Calendar.MONTH) - (startCalendar.get(Calendar.YEAR) * 12 + startCalendar.get(Calendar.MONTH)) + 1).toLong()
                                    Log.i("Debug", "$numRepeats")
                                }
                                dataStore.edit {
                                    it[longPreferencesKey(itemId.toString())] = numRepeats
                                }
                                var delay = startCalendar.timeInMillis - (textRemind.value!!.toInt() * 60000) - System.currentTimeMillis()
                                if (delay < 0) {
                                    startCalendar.add(Calendar.DAY_OF_YEAR, 7)
                                    delay = startCalendar.timeInMillis - (textRemind.value!!.toInt() * 60000) - System.currentTimeMillis()
                                    dataStore.edit {
                                        it[longPreferencesKey(itemId.toString())] = numRepeats - 1
                                    }
                                }
                                val request =
                                    OneTimeWorkRequestBuilder<MonthlyNotificationWorker>()
                                        .setInputData(
                                            workDataOf(
                                                MonthlyNotificationWorker.TODO_NAME to textName.value,
                                                MonthlyNotificationWorker.GROUP_NAME to groupName,
                                                MonthlyNotificationWorker.TODO_TIME to textItemStart.value,
                                                MonthlyNotificationWorker.TODO_DAY to orderSuffix(startCalendar.get(Calendar.DAY_OF_MONTH)),
                                                MonthlyNotificationWorker.TODO_REMIND to textRemind.value,
                                                MonthlyNotificationWorker.TODO_DESCRIPTION to textDescription.value,
                                                MonthlyNotificationWorker.TODO_ID to itemId
                                            )
                                        )
                                        .setInitialDelay(
                                            delay,
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
//            }
            return Pair(result, returnItemId)
        }
        return Pair(result, -1)
    }
}