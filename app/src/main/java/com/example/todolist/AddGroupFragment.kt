package com.example.todolist

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.todolist.databinding.FragmentAddGroupBinding
import com.example.todolist.databinding.FragmentHomeBinding
import com.example.todolist.db.ItemDao
import com.example.todolist.db.TodoDao
import com.example.todolist.db.TodoDatabase
import top.defaults.colorpicker.ColorPickerPopup
import androidx.core.graphics.toColorInt

class AddGroupFragment : Fragment() {
    private lateinit var binding: FragmentAddGroupBinding
    private lateinit var viewModel: AddGroupFragmentViewModel
    private lateinit var todoDao: TodoDao
    private lateinit var itemDao: ItemDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        todoDao = TodoDatabase.getInstance(requireContext().applicationContext).todoDao
        itemDao = TodoDatabase.getInstance(requireContext().applicationContext).itemDao
        val factory = AddGroupViewModelFactory(todoDao, itemDao, requireActivity().application)
        viewModel = ViewModelProvider(this, factory).get(AddGroupFragmentViewModel(todoDao, itemDao, requireActivity().application)::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddGroupBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.btnDone.setOnClickListener {
            viewModel.addGroup()
            viewModel.selectedColour.value = "#C6E99F".toColorInt()
        }
        viewModel.groupResult.observe(viewLifecycleOwner) { result ->
            Toast.makeText(requireContext(), result, Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_addGroupFragment_to_homeFragment)
        }
        binding.btnColour.setOnClickListener {
            ColorPickerPopup.Builder(requireContext())
                .initialColor("#C6E99F".toColorInt())
                .enableBrightness(true)
                .enableAlpha(false)
                .okTitle("Choose")
                .cancelTitle("Cancel")
                .showIndicator(false)
                .showValue(false)
                .build()
                .show(it, object : ColorPickerPopup.ColorPickerObserver() {
                    override fun onColorPicked(color: Int) {
                        viewModel.selectedColour.value = color
                        binding.btnColour.backgroundTintList = ColorStateList.valueOf(color)
                    }
                })
        }
        return binding.root
    }
}