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
import com.example.petcare.databinding.FragmentPetsBinding
import com.example.petcare.ui.viewmodel.PetUiState
import com.example.petcare.ui.viewmodel.PetViewModel
import kotlinx.coroutines.launch

class MyPetsFragment : Fragment() {

    private val viewModel: PetViewModel by viewModels()
    private var binding: FragmentPetsBinding? = null
    private var adapter: PetAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPetsBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        val adapter = PetAdapter(onPetClick = { pet ->
            (requireActivity() as? PetsActivity)?.pushFragment(PetDetailsFragment.newInstance(pet.id))
        })
        this.adapter = adapter
        binding.rvPets.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPets.adapter = adapter

        binding.btnAddPet.setOnClickListener {
            (requireActivity() as? PetsActivity)?.pushFragment(PetFormFragment.newInstance(null))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.petUiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: PetUiState) {
        val binding = binding ?: return

        binding.petsLoading.isVisible = state.isLoading && state.pets.isEmpty()

        if (!state.isLoading && state.pets.isEmpty()) {
            binding.rvPets.isVisible = false
            binding.emptyPets.root.isVisible = true
            bindEmptyState(state)
        } else if (state.pets.isNotEmpty()) {
            binding.emptyPets.root.isVisible = false
            binding.rvPets.isVisible = true
            adapter?.submitList(state.pets)
        }
    }

    private fun bindEmptyState(state: PetUiState) {
        val binding = binding ?: return
        val empty = binding.emptyPets
        empty.emptyStateTitle.text = getString(R.string.home_no_pets_title)
        empty.emptyStateDescription.text = getString(R.string.home_no_pets_description)
        empty.emptyStateAction.text = getString(R.string.add_pet_content_description)
        empty.emptyStateAction.setOnClickListener {
            (requireActivity() as? PetsActivity)?.pushFragment(PetFormFragment.newInstance(null))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        binding = null
    }
}
