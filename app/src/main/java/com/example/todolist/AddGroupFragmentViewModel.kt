package com.example.todolist

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.db.GroupInfo
import com.example.todolist.db.ItemDao
import com.example.todolist.db.TodoDao
import kotlinx.coroutines.launch

class AddGroupFragmentViewModel(private val todoDao: TodoDao, private val itemDao: ItemDao, private val application: Application) : ViewModel() {
    val textName = MutableLiveData<String>()
    private val _groupResult = MutableLiveData<String>()
    val groupResult: LiveData<String> = _groupResult

    init {
        textName.value = ""
    }

    fun addGroup() {
        viewModelScope.launch {
            if (textName.value!!.isNotBlank()) {
                _groupResult.value = "Group created successfully"
                todoDao.insertItem(GroupInfo(0, textName.value!!))
                textName.value = ""
            } else {
                _groupResult.value = "Group name cannot be blank, group creation failed"
            }
        }
    }
}