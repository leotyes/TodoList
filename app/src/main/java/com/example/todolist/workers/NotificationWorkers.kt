package com.example.todolist.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.icu.lang.UCharacter.GraphemeClusterBreak.L
import android.icu.util.Calendar
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.todolist.DataStoreManager.dataStore
import com.example.todolist.R
import com.example.todolist.workers.DailyNotificationWorker.Companion
import com.example.todolist.workers.DaySummaryNotificationWorker.Companion.TODO_ITEM
import com.example.todolist.workers.SingleNotificationWorker.Companion.GROUP_NAME
import com.example.todolist.workers.SingleNotificationWorker.Companion.TODO_DESCRIPTION
import com.example.todolist.workers.SingleNotificationWorker.Companion.TODO_ID
import com.example.todolist.workers.SingleNotificationWorker.Companion.TODO_NAME
import com.example.todolist.workers.SingleNotificationWorker.Companion.TODO_REMIND
import com.example.todolist.workers.SingleNotificationWorker.Companion.TODO_TIME
import kotlinx.coroutines.flow.first
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class DailyNotificationWorker(private val appContext: Context, private val workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val itemName = workerParams.inputData.getString(TODO_NAME)
        val groupName = workerParams.inputData.getString(GROUP_NAME)
        val itemTime = workerParams.inputData.getString(TODO_TIME)
        val itemRemind = workerParams.inputData.getString(TODO_REMIND)
        val itemDescription = workerParams.inputData.getString(TODO_DESCRIPTION)
        val itemId = workerParams.inputData.getLong(TODO_ID, -1)
        val itemCurrent = appContext.dataStore.data.first()[longPreferencesKey(itemId.toString())]
        if (itemCurrent == 0L) {
            WorkManager.getInstance(appContext).cancelWorkById(id)
            return Result.success()
        } else {
            appContext.dataStore.edit {
                it[longPreferencesKey(itemId.toString())] = it[longPreferencesKey(itemId.toString())]!! - 1
            }
        }
        if (itemId != -1L) {
            val builder = NotificationCompat.Builder(appContext, "notification_channel")
                .setSmallIcon(R.drawable.baseline_keyboard_arrow_up_24)
                .setContentTitle("$itemName at $itemTime every day")
                .setContentText("$itemRemind minutes before")
                .setStyle(NotificationCompat.BigTextStyle()
                    .setBigContentTitle("$itemName at $itemTime in group $groupName")
                    .bigText(itemDescription))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            with(NotificationManagerCompat.from(appContext)) {
                if (ActivityCompat.checkSelfPermission(
                        appContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(itemId.toInt(), builder.build())
                    return@with
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val TODO_NAME = "TODO_NAME"
        const val GROUP_NAME = "GROUP_NAME"
        const val TODO_TIME = "TODO_TIME"
        const val TODO_REMIND = "TODO_REMIND"
        const val TODO_DESCRIPTION = "TODO_DESCRIPTION"
        const val TODO_ID = "TODO_ID"
    }
}

class WeeklyNotificationWorker(private val appContext: Context, private val workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val itemName = workerParams.inputData.getString(TODO_NAME)
        val groupName = workerParams.inputData.getString(GROUP_NAME)
        val itemTime = workerParams.inputData.getString(TODO_TIME)
        val itemDay = workerParams.inputData.getString(TODO_DAY)
        val itemRemind = workerParams.inputData.getString(TODO_REMIND)
        val itemDescription = workerParams.inputData.getString(TODO_DESCRIPTION)
        val itemId = workerParams.inputData.getLong(TODO_ID, -1)
        val itemCurrent = appContext.dataStore.data.first()[longPreferencesKey(itemId.toString())]
        if (itemCurrent == 0L) {
            WorkManager.getInstance(appContext).cancelWorkById(id)
            return Result.success()
        } else {
            appContext.dataStore.edit {
                it[longPreferencesKey(itemId.toString())] = it[longPreferencesKey(itemId.toString())]!! - 1
            }
        }
        if (itemId != -1L) {
            val builder = NotificationCompat.Builder(appContext, "notification_channel")
                .setSmallIcon(R.drawable.baseline_keyboard_arrow_up_24)
                .setContentTitle("$itemName at $itemTime every $itemDay")
                .setContentText("$itemRemind minutes before")
                .setStyle(NotificationCompat.BigTextStyle()
                    .setBigContentTitle("$itemName at $itemTime every $itemDay in group $groupName")
                    .bigText(itemDescription))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            with(NotificationManagerCompat.from(appContext)) {
                if (ActivityCompat.checkSelfPermission(
                        appContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(itemId.toInt(), builder.build())
                    return@with
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val TODO_NAME = "TODO_NAME"
        const val GROUP_NAME = "GROUP_NAME"
        const val TODO_TIME = "TODO_TIME"
        const val TODO_DAY = "TODO_DAY"
        const val TODO_REMIND = "TODO_REMIND"
        const val TODO_DESCRIPTION = "TODO_DESCRIPTION"
        const val TODO_ID = "TODO_ID"
    }
}

class MonthlyNotificationWorker(private val appContext: Context, private val workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val workManager = WorkManager.getInstance(appContext)
        val itemName = workerParams.inputData.getString(TODO_NAME)
        val groupName = workerParams.inputData.getString(GROUP_NAME)
        val itemTime = workerParams.inputData.getString(TODO_TIME)
        val itemDay = workerParams.inputData.getString(TODO_DAY)
        val itemRemind = workerParams.inputData.getString(TODO_REMIND)
        val itemDescription = workerParams.inputData.getString(TODO_DESCRIPTION)
        val itemId = workerParams.inputData.getLong(TODO_ID, -1)
        val itemCurrent = appContext.dataStore.data.first()[longPreferencesKey(itemId.toString())]
        if (itemCurrent == 0L) {
            WorkManager.getInstance(appContext).cancelWorkById(id)
            return Result.success()
        } else {
            if (itemId != -1L) {
                val builder = NotificationCompat.Builder(appContext, "notification_channel")
                    .setSmallIcon(R.drawable.baseline_keyboard_arrow_up_24)
                    .setContentTitle("$itemName at $itemTime on the $itemDay of every month")
                    .setContentText("$itemRemind minutes before")
                    .setStyle(NotificationCompat.BigTextStyle()
                        .setBigContentTitle("$itemName at $itemTime on the $itemDay of every month in group $groupName")
                        .bigText(itemDescription))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                with(NotificationManagerCompat.from(appContext)) {
                    if (ActivityCompat.checkSelfPermission(
                            appContext,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        notify(itemId.toInt(), builder.build())
                        return@with
                    }
                }
            }
            appContext.dataStore.edit {
                it[longPreferencesKey(itemId.toString())] = it[longPreferencesKey(itemId.toString())]!! - 1
            }
            val finalCalendar = Calendar.getInstance()
            val timeCalendar = Calendar.getInstance()
            finalCalendar.time = SimpleDateFormat("dd/MM/yyyy").parse(itemDay)
            timeCalendar.time = SimpleDateFormat("HH:mm").parse(itemTime)
            finalCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            finalCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
            finalCalendar.add(Calendar.MONTH, 1)
            val dayNumber = itemDay!!.replace("[^0-9]".toRegex(), "").toInt()
            if (finalCalendar.get(Calendar.DAY_OF_MONTH) != dayNumber) {
                finalCalendar.set(Calendar.DAY_OF_MONTH, finalCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            val request =
                OneTimeWorkRequestBuilder<MonthlyNotificationWorker>()
                    .setInputData(
                        workDataOf(
                            TODO_NAME to itemName,
                            GROUP_NAME to groupName,
                            TODO_TIME to itemTime,
                            TODO_DAY to itemDay,
                            TODO_REMIND to itemRemind,
                            TODO_DESCRIPTION to itemDescription,
                            TODO_ID to itemId
                        )
                    )
                    .setInitialDelay(
                        finalCalendar.timeInMillis - (itemRemind!!.toInt() * 60000) - System.currentTimeMillis(),
                        TimeUnit.MILLISECONDS
                    )
                    .build()
            workManager.enqueueUniqueWork(
                itemId.toString(),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
        return Result.success()
    }

    companion object {
        const val TODO_NAME = "TODO_NAME"
        const val GROUP_NAME = "GROUP_NAME"
        const val TODO_TIME = "TODO_TIME"
        const val TODO_DAY = "TODO_DAY"
        const val TODO_REMIND = "TODO_REMIND"
        const val TODO_DESCRIPTION = "TODO_DESCRIPTION"
        const val TODO_ID = "TODO_ID"
    }
}

class SingleNotificationWorker(private val appContext: Context, private val workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val itemName = workerParams.inputData.getString(TODO_NAME)
        val groupName = workerParams.inputData.getString(GROUP_NAME)
        val itemTime = workerParams.inputData.getString(TODO_TIME)
        val itemRemind = workerParams.inputData.getString(TODO_REMIND)
        val itemDescription = workerParams.inputData.getString(TODO_DESCRIPTION)
        val itemId = workerParams.inputData.getLong(TODO_ID, -1)
        if (itemId != -1L) {
            val builder = NotificationCompat.Builder(appContext, "notification_channel")
                .setSmallIcon(R.drawable.baseline_keyboard_arrow_up_24)
                .setContentTitle("$itemName at $itemTime")
                .setContentText("$itemRemind minutes before")
                .setStyle(NotificationCompat.BigTextStyle()
                    .setBigContentTitle("$itemName at $itemTime in group $groupName")
                    .bigText(itemDescription))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            with(NotificationManagerCompat.from(appContext)) {
                if (ActivityCompat.checkSelfPermission(
                        appContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(itemId.toInt(), builder.build())
                    return@with
                }
            }
        }

        return Result.success()
    }

    companion object {
        const val TODO_NAME = "TODO_NAME"
        const val GROUP_NAME = "GROUP_NAME"
        const val TODO_TIME = "TODO_TIME"
        const val TODO_REMIND = "TODO_REMIND"
        const val TODO_DESCRIPTION = "TODO_DESCRIPTION"
        const val TODO_ID = "TODO_ID"
    }
}

class DaySummaryNotificationWorker(private val appContext: Context, private val workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val group = workerParams.inputData.getString(TODO_ITEM)
        return Result.success()
    }

    companion object {
        const val TODO_ITEM = "TODO_ITEM"
    }
}