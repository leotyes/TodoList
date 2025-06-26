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
interface ItemDao {
    @Insert
    suspend fun insertItem(item: ItemInfo): Long

    @Update
    suspend fun editItem(item: ItemInfo)

    @Delete
    suspend fun deleteItem(item: ItemInfo)

    @Query("SELECT * FROM item_data_table")
    fun getItems(): LiveData<List<ItemInfo>>

    @Query("SELECT * FROM item_data_table WHERE todo_group = :parentGroup")
    fun getGroupedItems(parentGroup: Long): LiveData<List<ItemInfo>>
}