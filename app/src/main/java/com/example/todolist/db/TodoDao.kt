package com.example.todolist.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.todolist.db.GroupInfo
import com.example.todolist.db.ItemInfo

@Dao
interface TodoDao {
    @Insert
    suspend fun insertItem(item: GroupInfo)

    @Update
    suspend fun editItem(item: GroupInfo)

    @Delete
    suspend fun deleteItem(item: GroupInfo)

    @Query("SELECT * FROM todo_data_table WHERE group_id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: Long): GroupInfo

    @Query("SELECT * FROM todo_data_table")
    fun getItems(): LiveData<List<GroupInfo>>
}