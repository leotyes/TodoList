package com.example.todolist.db

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Database
import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.example.todolist.db.GroupInfo

@Database(entities = [GroupInfo::class, ItemInfo::class], version = 1, exportSchema = false)
abstract class TodoDatabase: RoomDatabase()/*, TodoDao, ItemDao*/ {
    abstract val todoDao: TodoDao
    abstract val itemDao: ItemDao
    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null
        fun getInstance(context: Context): TodoDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        TodoDatabase::class.java,
                        "todo_data_database"
                    ).build()
                }
                return instance
            }
        }
    }

//    override fun clearAllTables() {
//        TODO("Not yet implemented")
//    }
//
//    override fun createInvalidationTracker(): InvalidationTracker {
//        TODO("Not yet implemented")
//    }
//
//    override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun insertItem(item: GroupInfo) {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun editItem(item: GroupInfo) {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun deleteItem(item: GroupInfo) {
//        TODO("Not yet implemented")
//    }
//
//    override fun getItems(): LiveData<List<GroupInfo>> {
//        TODO("Not yet implemented")
//    }
}