package com.example.todolist

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.todolist.db.ItemDao
import com.example.todolist.db.TodoDao

class HomeViewModelFactory(
    private val todoDao: TodoDao,
    private val itemDao: ItemDao,
    private val application: Application
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeFragmentViewModel::class.java)) {
            return HomeFragmentViewModel(todoDao, itemDao, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}