package com.example.todolist

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.todolist.databinding.FragmentCalendarBinding
import com.example.todolist.db.ItemDao
import com.example.todolist.db.TodoDao
import com.example.todolist.db.TodoDatabase

class CalendarFragment : Fragment() {
    private lateinit var binding: FragmentCalendarBinding
    private lateinit var itemDao: ItemDao
    private lateinit var todoDao: TodoDao
    private lateinit var viewModel: CalendarFragmentViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemDao = TodoDatabase.getInstance(requireContext().applicationContext).itemDao
        todoDao = TodoDatabase.getInstance(requireContext().applicationContext).todoDao
        val factory = CalendarViewModelFactory(requireActivity().application, itemDao, todoDao)
        viewModel = ViewModelProvider(this, factory).get(CalendarFragmentViewModel(requireActivity().application, itemDao, todoDao)::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCalendarBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        return binding.root
    }
}