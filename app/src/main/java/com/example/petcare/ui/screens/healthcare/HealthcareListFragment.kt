package com.example.petcare.ui.screens.healthcare

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
import com.example.petcare.data.local.entity.HealthcareRecordEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.databinding.FragmentHealthcareListBinding
import com.example.petcare.databinding.ItemHealthcareCategoryCardBinding
import com.example.petcare.ui.viewmodel.HealthcareRecordViewModel
import com.example.petcare.ui.viewmodel.PetViewModel
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class HealthcareListFragment : Fragment() {

    private val viewModel: HealthcareRecordViewModel by viewModels()
    private val petViewModel: PetViewModel by viewModels()
    private var binding: FragmentHealthcareListBinding? = null
    private var adapter: HealthcareAdapter? = null

    private var petId: Long? = null
    private var selectedFilter: String = FILTER_ALL
    private var latestRecords: List<HealthcareRecordEntity> = emptyList()
    private var latestPets: List<PetEntity> = emptyList()
    private var hasLoadedOnce = false

    private val recordTypes = listOf(
        FILTER_ALL, "VETERINARY_APPOINTMENT", "VACCINATION", "MEDICATION",
        "MEDICAL_CONDITION", "ALLERGY", "HEALTH_CHECK", "TREATMENT", "OTHER"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentHealthcareListBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        petId = arguments?.getLong(ARG_PET_ID, -1L)?.takeIf { it > 0 }

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnAddHealthcare.isVisible = petId != null
        binding.btnAddHealthcare.setOnClickListener { openAddRecord() }
        binding.btnEmptyAddRecord.setOnClickListener { openAddRecord() }

        setupCategoryCards(binding)
        setupFilterChips(binding)

        val adapter = HealthcareAdapter(onRecordClick = { record -> openDetails(record.id) })
        this.adapter = adapter
        binding.rvHealthcare.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHealthcare.adapter = adapter

        binding.progressHealthcare.isVisible = true

        val recordsFlow = petId?.let { viewModel.observeRecordsForPet(it) } ?: viewModel.records

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { recordsFlow.collect { records -> latestRecords = records; hasLoadedOnce = true; render() } }
                launch { petViewModel.pets.collect { pets -> latestPets = pets; renderSubtitle(); render() } }
            }
        }
    }

    private fun setupCategoryCards(binding: FragmentHealthcareListBinding) {
        bindCategoryCard(binding.categoryCardVax, R.drawable.ic_vaccines, R.string.category_vax_label, R.color.soft_green)
        bindCategoryCard(binding.categoryCardMeds, R.drawable.ic_medical, R.string.category_meds_label, R.color.soft_blue)
        bindCategoryCard(binding.categoryCardVisits, R.drawable.ic_calendar, R.string.category_visits_label, R.color.soft_yellow)
        bindCategoryCard(binding.categoryCardAllergy, R.drawable.ic_warning, R.string.category_allergy_label, R.color.soft_lavender)
    }

    private fun bindCategoryCard(card: ItemHealthcareCategoryCardBinding, iconRes: Int, labelRes: Int, colorRes: Int) {
        card.categoryCardIcon.setImageResource(iconRes)
        card.categoryCardLabel.text = getString(labelRes)
        card.categoryCardIconContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(
            androidx.core.content.ContextCompat.getColor(requireContext(), colorRes)
        )
    }

    private fun setupFilterChips(binding: FragmentHealthcareListBinding) {
        binding.healthcareFilterChips.removeAllViews()
        val chipContext = android.view.ContextThemeWrapper(requireContext(), R.style.Widget_PetCare_Chip_Filter)
        recordTypes.forEach { type ->
            val chip = Chip(chipContext).apply {
                text = if (type == FILTER_ALL) getString(R.string.healthcare_filter_all) else humanizeHealthcareType(type)
                tag = type
                isCheckable = true
                isChecked = type == selectedFilter
                setOnClickListener {
                    selectedFilter = type
                    for (i in 0 until binding.healthcareFilterChips.childCount) {
                        val c = binding.healthcareFilterChips.getChildAt(i) as Chip
                        c.isChecked = c.tag == selectedFilter
                    }
                    render()
                }
            }
            binding.healthcareFilterChips.addView(chip)
        }
    }

    private fun renderSubtitle() {
        val binding = binding ?: return
        val pet = petId?.let { id -> latestPets.find { it.id == id } }
        binding.healthcareSubtitle.text = if (pet != null) {
            getString(R.string.healthcare_subtitle_pet_format, pet.name)
        } else {
            getString(R.string.healthcare_subtitle_general)
        }
    }

    private fun render() {
        val binding = binding ?: return
        val adapter = adapter ?: return

        binding.progressHealthcare.isVisible = !hasLoadedOnce
        if (!hasLoadedOnce) {
            binding.rvHealthcare.isVisible = false
            binding.tvEmptyHealthcare.isVisible = false
            return
        }

        val filtered = if (selectedFilter == FILTER_ALL) latestRecords else latestRecords.filter { it.type == selectedFilter }

        if (filtered.isEmpty()) {
            binding.rvHealthcare.isVisible = false
            binding.tvEmptyHealthcare.isVisible = true
            binding.btnEmptyAddRecord.isVisible = petId != null
        } else {
            binding.tvEmptyHealthcare.isVisible = false
            binding.rvHealthcare.isVisible = true
            val petNames = latestPets.associate { it.id to it.name }
            adapter.submitList(filtered, petNames)
        }
    }

    private fun openAddRecord() {
        val id = petId ?: return
        (requireActivity() as? HealthcareActivity)?.pushFragment(HealthcareFormFragment.newInstance(recordId = null, presetPetId = id))
    }

    private fun openDetails(recordId: Long) {
        (requireActivity() as? HealthcareActivity)?.pushFragment(HealthcareDetailsFragment.newInstance(recordId))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        binding = null
    }

    companion object {
        private const val FILTER_ALL = "ALL"
        private const val ARG_PET_ID = "pet_id"

        fun newInstance(petId: Long?): HealthcareListFragment = HealthcareListFragment().apply {
            arguments = Bundle().apply { putLong(ARG_PET_ID, petId ?: -1L) }
        }
    }
}
