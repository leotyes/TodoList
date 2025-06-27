package com.example.todolist

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.db.GroupInfo
import com.example.todolist.db.TodoDao
import kotlinx.coroutines.launch

class AddGroupFragmentViewModel : ViewModel() {
//    TODO implement viewmodel factory
    val textName = MutableLiveData<String>()
    private val _groupResult = MutableLiveData<String>()
    val groupResult: LiveData<String> = _groupResult

    init {
        textName.value = ""
    }

    fun addGroup(dao: TodoDao) {
        viewModelScope.launch {
            if (textName.value!!.isNotBlank()) {
                _groupResult.value = "Group created successfully"
                dao.insertItem(GroupInfo(0, textName.value!!))
                textName.value = ""
            } else {
                _groupResult.value = "Group name cannot be blank, group creation failed"
            }
        }
    }
}