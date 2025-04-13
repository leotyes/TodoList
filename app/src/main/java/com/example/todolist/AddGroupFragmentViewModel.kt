package com.example.todolist

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.db.GroupInfo
import com.example.todolist.db.TodoDao
import kotlinx.coroutines.launch

class AddGroupFragmentViewModel : ViewModel() {
    val textName = MutableLiveData<String>()

    init {
        textName.value = ""
    }

    fun addGroup(dao: TodoDao) {
        viewModelScope.launch {
            if (textName.value!!.isNotBlank()) {
                dao.insertItem(GroupInfo(0, textName.value!!))
                textName.value = ""
            }
            //TODO Make a toast if empty
        }
    }
}