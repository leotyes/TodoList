package com.example.todolist.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Time
import java.util.Date

@Entity(tableName = "item_data_table")
data class ItemInfo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "todo_id")
    var id: Long,
    @ColumnInfo(name = "todo_title")
    var name: String,
    @ColumnInfo(name = "todo_description")
    var description: String?,
    @ColumnInfo(name = "todo_group")
    var group: Long,
    @ColumnInfo(name = "todo_due_time")
    var dueTime: Long?,
    @ColumnInfo(name = "todo_due_date")
    var dueDate: Long?,
    @ColumnInfo(name = "todo_date")
    var date: Long?,
    @ColumnInfo(name = "todo_time_start")
    var timeStart: Long?,
    @ColumnInfo(name = "todo_time_end")
    var timeEnd: Long?,
    @ColumnInfo(name = "todo_range_start")
    var rangeStart: Long?,
    @ColumnInfo(name = "todo_range_end")
    var rangeEnd: Long?,
    @ColumnInfo(name = "todo_repeat")
    var repeatType: Int?,
    @ColumnInfo(name = "todo_checked")
    var checked: Boolean,
    @ColumnInfo(name = "todo_remind")
    var remind: Int?,
    @ColumnInfo(name = "todo_location_ids")
    var locationIds: String?,
)

@Entity(tableName = "todo_data_table")
data class GroupInfo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "group_id")
    var id: Long,
    @ColumnInfo(name = "group_title")
    var title: String,
    @ColumnInfo(name = "group_colour")
    var colour: Int
)