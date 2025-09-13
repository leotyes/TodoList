package com.example.todolist

import android.app.Application
import androidx.lifecycle.ViewModel
import com.example.todolist.db.ItemDao
import com.example.todolist.db.TodoDao

class CalendarFragmentViewModel(private val application: Application, private val itemDao: ItemDao, private val todoDao: TodoDao) : ViewModel() {
}