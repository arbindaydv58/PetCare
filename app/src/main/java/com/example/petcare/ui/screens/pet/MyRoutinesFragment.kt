package com.example.petcare.ui.screens.pet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petcare.R
import com.example.petcare.data.local.entity.CareRoutineEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.databinding.FragmentRoutinesBinding
import com.example.petcare.ui.viewmodel.CareRoutineViewModel
import com.example.petcare.ui.viewmodel.PetViewModel
import kotlinx.coroutines.launch

class MyRoutinesFragment : Fragment() {

    private val viewModel: CareRoutineViewModel by viewModels()
    private val petViewModel: PetViewModel by viewModels()
    private var binding: FragmentRoutinesBinding? = null
    private var adapter: RoutineAdapter? = null

    private var latestRoutines: List<CareRoutineEntity> = emptyList()
    private var latestPets: List<PetEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRoutinesBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        val adapter = RoutineAdapter(
            onRoutineClick = { routine -> (requireActivity() as? RoutinesActivity)?.pushFragment(RoutineDetailsFragment.newInstance(routine.id)) },
            onEditClick = { routine -> (requireActivity() as? RoutinesActivity)?.pushFragment(RoutineFormFragment.newInstance(routine.id, null)) }
        )
        this.adapter = adapter
        binding.rvRoutines.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRoutines.adapter = adapter

        binding.btnAddRoutine.setOnClickListener {
            (requireActivity() as? RoutinesActivity)?.pushFragment(RoutineFormFragment.newInstance(null, null))
        }

        val empty = binding.emptyRoutines
        empty.emptyStateTitle.text = getString(R.string.no_routines_title)
        empty.emptyStateDescription.text = getString(R.string.no_routines_description)
        empty.emptyStateAction.text = getString(R.string.create_routine_action)
        empty.emptyStateAction.setOnClickListener {
            (requireActivity() as? RoutinesActivity)?.pushFragment(RoutineFormFragment.newInstance(null, null))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.routines.collect { routines -> latestRoutines = routines; render() } }
                launch { petViewModel.pets.collect { pets -> latestPets = pets; render() } }
            }
        }
    }

    private fun render() {
        val binding = binding ?: return
        if (latestRoutines.isEmpty()) {
            binding.rvRoutines.isVisible = false
            binding.emptyRoutines.root.isVisible = true
        } else {
            binding.emptyRoutines.root.isVisible = false
            binding.rvRoutines.isVisible = true
            val petNames = latestPets.associate { it.id to it.name }
            adapter?.submitList(latestRoutines, petNames)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        binding = null
    }
}
