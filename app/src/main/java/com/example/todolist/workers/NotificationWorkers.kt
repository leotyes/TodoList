package com.example.todolist.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.todolist.R
// TODO work on other notifs
class DailyNotificationWorker(private val appContext: Context, private val workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val group = workerParams.inputData.getString(TODO_ITEM)
        return Result.success()
    }

    companion object {
        const val TODO_ITEM = "TODO_ITEM"
    }
}

class WeeklyNotificationWorker(private val appContext: Context, private val workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val group = workerParams.inputData.getString(TODO_ITEM)
        return Result.success()
    }

    companion object {
        const val TODO_ITEM = "TODO_ITEM"
    }
}

class MonthlyNotificationWorker(private val appContext: Context, private val workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val group = workerParams.inputData.getString(TODO_ITEM)
        return Result.success()
    }

    companion object {
        const val TODO_ITEM = "TODO_ITEM"
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
                    notify(1, builder.build())
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