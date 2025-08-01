package com.example.todolist

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.todolist.databinding.FragmentAddItemBinding
import com.example.todolist.db.ItemDao
import com.example.todolist.db.TodoDao
import com.example.todolist.db.TodoDatabase
import java.text.SimpleDateFormat
import kotlin.math.min

class AddItemFragment : Fragment() {
    private lateinit var binding: FragmentAddItemBinding
    private lateinit var itemDao: ItemDao
    private lateinit var todoDao: TodoDao
    private lateinit var viewModel: AddItemFragmentViewModel
    private val args: AddItemFragmentArgs by navArgs()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemDao = TodoDatabase.getInstance(requireContext().applicationContext).itemDao
        todoDao = TodoDatabase.getInstance(requireContext().applicationContext).todoDao
        val factory = AddItemViewModelFactory(requireActivity().application, itemDao, todoDao)
        viewModel = ViewModelProvider(this, factory).get(AddItemFragmentViewModel(requireActivity().application, itemDao, todoDao)::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddItemBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        viewModel.parentGroup.value = args.group
        binding.lifecycleOwner = viewLifecycleOwner
        binding.btnTimeDue.setOnClickListener {
            val timeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                viewModel.calDue.set(Calendar.HOUR_OF_DAY, hour)
                viewModel.calDue.set(Calendar.MINUTE, minute)
                viewModel.textDueTime.value = SimpleDateFormat("HH:mm").format(viewModel.calDue.time)
            }
            TimePickerDialog(requireContext(), android.R.style.Theme_Material_Dialog, timeSetListener, viewModel.calDue.get(Calendar.HOUR_OF_DAY), viewModel.calDue.get(Calendar.MINUTE), true).show()
        }
        binding.btnDayDue.setOnClickListener {
            val dateSetListener = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
                viewModel.calDue.set(Calendar.DAY_OF_MONTH, day)
                viewModel.calDue.set(Calendar.MONTH, month)
                viewModel.calDue.set(Calendar.YEAR, year)
                viewModel.textDueDate.value = SimpleDateFormat("dd/MM/yyyy").format(viewModel.calDue.time)
            }
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                dateSetListener,
                2025,
                0,
                1
            )
            datePickerDialog.datePicker.minDate = viewModel.minDateInMillisDue.value!!
            datePickerDialog.show()
        }
        binding.btnItemDay.setOnClickListener {
            val dateSetListener = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
                viewModel.calDate.set(Calendar.DAY_OF_MONTH, day)
                viewModel.calDate.set(Calendar.MONTH, month)
                viewModel.calDate.set(Calendar.YEAR, year)
                viewModel.textItemDate.value = SimpleDateFormat("dd/MM/yyyy").format(viewModel.calDate.time)
            }
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                dateSetListener,
                2025,
                0,
                1
            )
            datePickerDialog.datePicker.minDate = viewModel.minDateInMillisStart.value!!
            datePickerDialog.show()
        }
        val startTimeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
            viewModel.calDate.set(Calendar.HOUR_OF_DAY, hour)
            viewModel.calDate.set(Calendar.MINUTE, minute)
            viewModel.textItemStart.value = SimpleDateFormat("HH:mm").format(viewModel.calDate.time)
            viewModel.calcMinEndTime(hour, minute, 0)
        }
        val endTimeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
            viewModel.calStartDate.set(Calendar.HOUR_OF_DAY, hour)
            viewModel.calStartDate.set(Calendar.MINUTE, minute)
            viewModel.textItemEnd.value = SimpleDateFormat("HH:mm").format(viewModel.calStartDate.time)
            viewModel.calcMinEndTime(hour, minute, 1)
        }
        binding.btnItemStart.setOnClickListener {
            TimePickerDialog(requireContext(), android.R.style.Theme_Material_Dialog, startTimeSetListener, viewModel.calDate.get(Calendar.HOUR_OF_DAY), viewModel.calDate.get(Calendar.MINUTE), true).show()
        }
        binding.btnItemEnd.setOnClickListener {
            TimePickerDialog(requireContext(), android.R.style.Theme_Material_Dialog, endTimeSetListener, viewModel.calStartDate.get(Calendar.HOUR_OF_DAY), viewModel.calStartDate.get(Calendar.MINUTE), true).show()
        }
        binding.cbDate.setOnClickListener {
            if (viewModel.checkedItemDate.value == false) {
                binding.llStartTime.visibility = View.GONE
                binding.llEndTime.visibility = View.GONE
                binding.llRemind.visibility = viewModel.remindVisibility(binding.llStartTime.visibility)
            } else {
                binding.llStartTime.visibility = View.VISIBLE
                binding.llEndTime.visibility = View.VISIBLE
                binding.llRemind.visibility = viewModel.remindVisibility(binding.llStartTime.visibility)
            }
        }
        binding.btnItemDone.setOnClickListener {
            val result = viewModel.addItem()
            if (result == "Item added successfully") {
                findNavController().navigate(R.id.action_addItemFragment_to_homeFragment)
            }
            Toast.makeText(requireContext(), result, Toast.LENGTH_LONG).show()
        }
        binding.cbDaily.setOnClickListener {
            if (viewModel.checkedDaily.value == true) {
                viewModel.checkedWeekly.value = false
                viewModel.checkedMonthly.value = false
                viewModel.checkedItemDate.value = false
                binding.llDate.visibility = View.GONE
                binding.llRangeEnd.visibility = View.VISIBLE
                binding.llRangeStart.visibility = View.VISIBLE
                binding.llStartTime.visibility = View.VISIBLE
                binding.llEndTime.visibility = View.VISIBLE
                binding.llRemind.visibility = viewModel.remindVisibility(binding.llStartTime.visibility)
                viewModel.calcMinEndDate()
            } else {
                binding.llDate.visibility = View.VISIBLE
                binding.llRangeEnd.visibility = View.GONE
                binding.llRangeStart.visibility = View.GONE
                if (viewModel.checkedItemDate.value == true) {
                    binding.llStartTime.visibility = View.VISIBLE
                    binding.llEndTime.visibility = View.VISIBLE
                } else {
                    binding.llStartTime.visibility = View.GONE
                    binding.llEndTime.visibility = View.GONE
                }
                binding.llRemind.visibility = viewModel.remindVisibility(binding.llStartTime.visibility)
            }
        }
        binding.cbWeekly.setOnClickListener {
            if (viewModel.checkedWeekly.value == true) {
                viewModel.checkedDaily.value = false
                viewModel.checkedMonthly.value = false
                viewModel.checkedItemDate.value = false
                binding.llDate.visibility = View.GONE
                binding.llRangeEnd.visibility = View.VISIBLE
                binding.llRangeStart.visibility = View.VISIBLE
                binding.llStartTime.visibility = View.VISIBLE
                binding.llEndTime.visibility = View.VISIBLE
                binding.llRemind.visibility = viewModel.remindVisibility(binding.llStartTime.visibility)
                viewModel.calcMinEndDate()
            } else {
                binding.llDate.visibility = View.VISIBLE
                binding.llRangeEnd.visibility = View.GONE
                binding.llRangeStart.visibility = View.GONE
                if (viewModel.checkedItemDate.value == true) {
                    binding.llStartTime.visibility = View.VISIBLE
                    binding.llEndTime.visibility = View.VISIBLE
                } else {
                    binding.llStartTime.visibility = View.GONE
                    binding.llEndTime.visibility = View.GONE
                }
                binding.llRemind.visibility = viewModel.remindVisibility(binding.llStartTime.visibility)
            }
        }
        binding.cbMonthly.setOnClickListener {
            if (viewModel.checkedMonthly.value == true) {
                viewModel.checkedWeekly.value = false
                viewModel.checkedDaily.value = false
                viewModel.checkedItemDate.value = false
                binding.llDate.visibility = View.GONE
                binding.llRangeEnd.visibility = View.VISIBLE
                binding.llRangeStart.visibility = View.VISIBLE
                binding.llStartTime.visibility = View.VISIBLE
                binding.llEndTime.visibility = View.VISIBLE
                binding.llRemind.visibility = viewModel.remindVisibility(binding.llStartTime.visibility)
                viewModel.calcMinEndDate()
            } else {
                binding.llDate.visibility = View.VISIBLE
                binding.llRangeEnd.visibility = View.GONE
                binding.llRangeStart.visibility = View.GONE
                if (viewModel.checkedItemDate.value == true) {
                    binding.llStartTime.visibility = View.VISIBLE
                    binding.llEndTime.visibility = View.VISIBLE
                } else {
                    binding.llStartTime.visibility = View.GONE
                    binding.llEndTime.visibility = View.GONE
                }
                binding.llRemind.visibility = viewModel.remindVisibility(binding.llStartTime.visibility)
            }
        }
        binding.cbStartTime.setOnClickListener {
            if (viewModel.checkedItemStart.value == true) {
                binding.llRemind.visibility = viewModel.remindVisibility(binding.llStartTime.visibility)
                viewModel.calcMinEndTime(source = 2)
            } else {
                binding.llRemind.visibility = viewModel.remindVisibility(binding.llStartTime.visibility)
            }
        }
        val dateStartSetListener = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
            viewModel.calStartDate.set(Calendar.DAY_OF_MONTH, day)
            viewModel.calStartDate.set(Calendar.MONTH, month)
            viewModel.calStartDate.set(Calendar.YEAR, year)
            viewModel.textDateStart.value = SimpleDateFormat("dd/MM/yyyy").format(viewModel.calStartDate.time)
            viewModel.calcMinEndDate()
        }
        val dateEndSetListener = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
            viewModel.calEndDate.set(Calendar.DAY_OF_MONTH, day)
            viewModel.calEndDate.set(Calendar.MONTH, month)
            viewModel.calEndDate.set(Calendar.YEAR, year)
            viewModel.textDateEnd.value = SimpleDateFormat("dd/MM/yyyy").format(viewModel.calEndDate.time)
            viewModel.roundEndDate(year, month, day)
        }
        binding.btnStartDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                dateStartSetListener,
                2025,
                0,
                1
            )
            datePickerDialog.datePicker.minDate = viewModel.minDateInMillisStart.value!!
            datePickerDialog.show()
        }
        binding.btnEndDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                dateEndSetListener,
                2025,
                0,
                1
            )
            datePickerDialog.datePicker.minDate = viewModel.minDateInMillisEnd.value!!
            datePickerDialog.show()
        }
        return binding.root
    }
}
