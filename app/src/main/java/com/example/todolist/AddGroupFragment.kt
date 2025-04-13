package com.example.todolist

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.todolist.databinding.FragmentAddGroupBinding
import com.example.todolist.databinding.FragmentHomeBinding
import com.example.todolist.db.TodoDao
import com.example.todolist.db.TodoDatabase

class AddGroupFragment : Fragment() {
    private lateinit var binding: FragmentAddGroupBinding
    private lateinit var viewModel: AddGroupFragmentViewModel
    private lateinit var todoListRecyclerViewAdapter: TodoListRecyclerViewAdapter
    private lateinit var dao: TodoDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dao = TodoDatabase.getInstance(requireContext().applicationContext).todoDao
        //val factory = HomeViewModelFactory(dao)
        viewModel = ViewModelProvider(this).get(AddGroupFragmentViewModel()::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddGroupBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.btnDone.setOnClickListener {
            viewModel.addGroup(dao)
            findNavController().navigate(R.id.action_addGroupFragment_to_homeFragment)
        }
        return binding.root
    }
}