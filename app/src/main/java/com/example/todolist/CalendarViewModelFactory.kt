package com.example.todolist

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.todolist.db.ItemDao
import com.example.todolist.db.TodoDao

class CalendarViewModelFactory(private val application: Application, private val itemDao: ItemDao, private val todoDao: TodoDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarFragmentViewModel::class.java)) {
            return CalendarFragmentViewModel(application, itemDao, todoDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}