package com.example.todolist

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todolist.databinding.FragmentHomeBinding
import com.example.todolist.db.GroupInfo
import com.example.todolist.db.ItemDao
import com.example.todolist.db.ItemInfo
import com.example.todolist.db.TodoDao
import com.example.todolist.db.TodoDatabase
import kotlin.math.abs

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: HomeFragmentViewModel
    private lateinit var todoListRecyclerViewAdapter: TodoListRecyclerViewAdapter
    private lateinit var todoDao: TodoDao
    private lateinit var itemDao: ItemDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        todoDao = TodoDatabase.getInstance(requireContext().applicationContext).todoDao
        itemDao = TodoDatabase.getInstance(requireContext().applicationContext).itemDao
        val factory = HomeViewModelFactory(todoDao, itemDao)
        viewModel = ViewModelProvider(this, factory).get(HomeFragmentViewModel(todoDao, itemDao)::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        Log.i("Debugging", viewModel.selectedGroup.toString())
        binding.lifecycleOwner = viewLifecycleOwner
        binding.rvGroups.layoutManager = LinearLayoutManager(requireContext().applicationContext)
        todoListRecyclerViewAdapter = TodoListRecyclerViewAdapter(viewModel,
            {
                selectedGroup: GroupInfo, visibility: Int -> viewModel.groupClicked(selectedGroup, visibility)
                Log.i("Debugging", visibility.toString())
                binding.fabAdd.visibility = visibility
                //binding.view.elevation = viewModel.dpToPx(requireContext(), 10f)
                //binding.rvGroups.elevation = viewModel.dpToPx(requireContext(), 9f)
            },
            {
                selectedGroup: GroupInfo -> viewModel.editClicked()
                binding.view.visibility = View.VISIBLE
                binding.cvEditName.visibility = View.VISIBLE
                Log.i("Debugging", "edit")
            },
            {
                selectedGroup: GroupInfo -> viewModel.deleteGroup(selectedGroup)
                binding.fabAdd.visibility = View.VISIBLE
            },
            {
                selectedGroup: GroupInfo -> viewModel.addItem(selectedGroup)
                Log.i("Debugging", selectedGroup.toString())
                val action = HomeFragmentDirections.actionHomeFragmentToAddItemFragment(viewModel.selectedGroup!!.id)
                findNavController().navigate(action)
            },
            {
                item, checked -> viewModel.itemChecked(item, checked)
            },
        )
        binding.rvGroups.adapter = todoListRecyclerViewAdapter
        binding.rvGroups.scrollToPosition(0)
        binding.btnDoneName.setOnClickListener {
            viewModel.doneNameClicked()
            binding.cvEditName.visibility = View.GONE
            binding.view.visibility = View.GONE
        }
        viewModel.groups.observe(viewLifecycleOwner, {
            todoListRecyclerViewAdapter.setList(it)
            todoListRecyclerViewAdapter.notifyDataSetChanged()
        })
        binding.fabAdd.setOnClickListener {
            viewModel.addGroup()
            findNavController().navigate(R.id.action_homeFragment_to_addGroupFragment)
        }
        // Inflate the layout for this fragment
        return binding.root
    }
}