package com.example.petcare.ui.screens.delegation

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
import com.example.petcare.data.local.entity.DelegationEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.databinding.FragmentDelegationHistoryBinding
import com.example.petcare.ui.viewmodel.DelegationViewModel
import com.example.petcare.ui.viewmodel.PetViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class DelegationHistoryFragment : Fragment() {

    private val viewModel: DelegationViewModel by viewModels()
    private val petViewModel: PetViewModel by viewModels()
    private var binding: FragmentDelegationHistoryBinding? = null
    private var adapter: DelegationAdapter? = null

    private var latestHistory: List<DelegationEntity> = emptyList()
    private var latestPets: List<PetEntity> = emptyList()
    private var hasLoadedOnce = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDelegationHistoryBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnAddDelegation.setOnClickListener {
            (requireActivity() as? DelegationActivity)?.pushFragment(DelegationFormFragment.newInstance(null, null))
        }

        val adapter = DelegationAdapter(onDeleteClick = { delegation -> confirmDelete(delegation) })
        this.adapter = adapter
        binding.rvDelegations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDelegations.adapter = adapter

        binding.progressDelegations.isVisible = true

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.history.collect { history -> latestHistory = history; hasLoadedOnce = true; render() } }
                launch { petViewModel.pets.collect { pets -> latestPets = pets; render() } }
            }
        }
    }

    private fun render() {
        val binding = binding ?: return
        val adapter = adapter ?: return

        binding.progressDelegations.isVisible = !hasLoadedOnce
        if (!hasLoadedOnce) {
            binding.rvDelegations.isVisible = false
            binding.emptyDelegationsContainer.isVisible = false
            return
        }

        if (latestHistory.isEmpty()) {
            binding.rvDelegations.isVisible = false
            binding.emptyDelegationsContainer.isVisible = true
        } else {
            binding.emptyDelegationsContainer.isVisible = false
            binding.rvDelegations.isVisible = true
            val petNames = latestPets.associate { it.id to it.name }
            adapter.submitList(latestHistory, petNames)
        }
    }

    private fun confirmDelete(delegation: DelegationEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_delegation_dialog_title)
            .setMessage(R.string.delete_delegation_dialog_message)
            .setPositiveButton(R.string.action_delete) { dialog, _ ->
                viewModel.deleteDelegation(delegation)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        binding = null
    }
}
