package com.example.todolist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        binding.lifecycleOwner = viewLifecycleOwner
        binding.rvGroups.layoutManager = LinearLayoutManager(requireContext().applicationContext)
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            if (granted) {
                Toast.makeText(requireContext(), "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Notification permission denied, notifications will not work", Toast.LENGTH_SHORT).show()
            }
        }
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }

        }
        todoListRecyclerViewAdapter = TodoListRecyclerViewAdapter(viewModel,
            {
                selectedGroup: GroupInfo -> viewModel.editClicked(selectedGroup.id)
                binding.view.visibility = View.VISIBLE
                binding.cvEditName.visibility = View.VISIBLE
            },
            {
                selectedGroup: GroupInfo -> viewModel.deleteGroup(selectedGroup)
                binding.fabAdd.visibility = View.VISIBLE
            },
            {
                selectedGroup: GroupInfo -> viewModel.addItem(selectedGroup)
//                Log.i("Debugging", selectedGroup.toString())
                val action = HomeFragmentDirections.actionHomeFragmentToAddItemFragment(selectedGroup.id)
                findNavController().navigate(action)
            },
            {
                item, checked -> viewModel.itemChecked(item, checked)
            },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("notification_channel", "NotifChannel", importance).apply {
                description = "Notifications Channel"
            }
            val notificationManager: NotificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        binding.rvGroups.adapter = todoListRecyclerViewAdapter
        binding.rvGroups.scrollToPosition(0)
        binding.btnDoneName.setOnClickListener {
            Toast.makeText(requireContext(), viewModel.doneNameClicked(), Toast.LENGTH_LONG).show()
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
        return binding.root
    }
}