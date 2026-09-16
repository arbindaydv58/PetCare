package com.example.petcare.ui.screens.home

import android.content.Intent
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
import androidx.viewpager2.widget.ViewPager2
import com.example.petcare.R
import com.example.petcare.data.local.entity.HealthcareRecordEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.databinding.FragmentHomeBinding
import com.example.petcare.databinding.ItemHomeCareTaskBinding
import com.example.petcare.databinding.ItemHomeCareToolBinding
import com.example.petcare.databinding.ItemHomeRecentActivityBinding
import com.example.petcare.ui.viewmodel.CareTaskUiModel
import com.example.petcare.ui.viewmodel.CareTaskViewModel
import com.example.petcare.ui.viewmodel.ExpenseUiState
import com.example.petcare.ui.viewmodel.ExpenseViewModel
import com.example.petcare.ui.viewmodel.HealthcareRecordViewModel
import com.example.petcare.ui.viewmodel.PetViewModel
import com.example.petcare.ui.viewmodel.ageInYears
import com.example.petcare.ui.screens.checklist.TasksActivity
import com.example.petcare.ui.screens.expenses.ExpensesActivity
import com.example.petcare.ui.screens.healthcare.HealthcareActivity
import com.example.petcare.ui.screens.healthcare.healthcareColorFor
import com.example.petcare.ui.screens.healthcare.healthcareIconFor
import com.example.petcare.ui.screens.healthcare.humanizeHealthcareType
import com.example.petcare.ui.screens.pet.PetsActivity
import com.example.petcare.ui.screens.pet.RoutinesActivity
import com.example.petcare.ui.screens.profile.ProfileActivity
import com.example.petcare.ui.screens.settings.NotificationSettingsActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private val petViewModel: PetViewModel by viewModels()
    private val taskViewModel: CareTaskViewModel by viewModels()
    private val expenseViewModel: ExpenseViewModel by viewModels()
    private val healthcareViewModel: HealthcareRecordViewModel by viewModels()

    private var binding: FragmentHomeBinding? = null
    private var petPagerAdapter: PetPagerAdapter? = null

    private enum class PetCategoryFilter { ALL, DOGS, CATS }

    private var selectedCategory = PetCategoryFilter.ALL
    private var allPets: List<PetEntity> = emptyList()
    private var filteredPets: List<PetEntity> = emptyList()
    private var currentPage = 0
    private var nextCheckupByPetId: Map<Long, Long?> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentHomeBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        val userName = FirebaseAuth.getInstance().currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: getString(R.string.home_default_user_name)
        binding.homeGreeting.text = getString(R.string.home_greeting_format, userName)
        binding.homeAvatarInitial.text = userName.take(1).uppercase()

        binding.homeNotificationButton.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationSettingsActivity::class.java))
        }
        binding.homeAvatar.setOnClickListener { openProfile() }
        binding.homeAddPet.setOnClickListener { openPets() }

        setupPetPager(binding)
        setupCategoryChips(binding)
        setupFinanceSnippet(binding)
        setupBottomNavigation(binding)
        setupCareTools(binding)

        binding.homeSeeAllTasks.setOnClickListener {
            openTasks()
        }
        binding.homeSeeAllActivity.setOnClickListener {
            openHealthcare()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { petViewModel.pets.collect { pets -> onPetsChanged(pets) } }
                launch { taskViewModel.checklistState.collect { state -> renderTasks(state.tasks, state.completedCount, state.totalCount) } }
                launch { expenseViewModel.uiState.collect { state -> renderFinance(state) } }
                launch { healthcareViewModel.records.collect { records -> renderRecentActivity(records) } }
                launch {
                    healthcareViewModel.upcomingRecords.collect { records ->
                        nextCheckupByPetId = records
                            .groupBy { it.petId }
                            .mapValues { (_, petRecords) -> petRecords.minOf { it.nextDueDate ?: it.date } }
                        petPagerAdapter?.submitList(filteredPets, nextCheckupByPetId)
                    }
                }
            }
        }
    }

    private fun currentSelectedPet(): PetEntity? =
        filteredPets.getOrNull(currentPage.coerceIn(0, (filteredPets.size - 1).coerceAtLeast(0)))

    private fun openHealthcareAddRecord() {
        val petId = currentSelectedPet()?.id
        if (petId != null) {
            startActivity(
                Intent(requireContext(), HealthcareActivity::class.java).apply {
                    putExtra(HealthcareActivity.EXTRA_NEW_RECORD_PET_ID, petId)
                }
            )
        } else {
            openPets()
        }
    }

    private fun setupCareTools(binding: FragmentHomeBinding) {
        bindCareTool(
            binding.toolBookVet, R.drawable.ic_stethoscope, R.color.soft_green, R.color.success_green,
            R.string.home_tool_book_vet
        ) { openHealthcareAddRecord() }

        bindCareTool(
            binding.toolTraining, R.drawable.ic_graduation_cap, R.color.soft_lavender, R.color.primary_coral,
            R.string.home_tool_training
        ) { startActivity(Intent(requireContext(), RoutinesActivity::class.java)) }

        bindCareTool(
            binding.toolVaccination, R.drawable.ic_syringe, R.color.soft_green, R.color.success_green,
            R.string.home_tool_vaccination
        ) { openHealthcareAddRecord() }

        bindCareTool(
            binding.toolFoodDiet, R.drawable.ic_food_can, R.color.soft_coral, R.color.primary_coral,
            R.string.home_tool_food_diet
        ) { openTasks() }

        bindCareTool(
            binding.toolRecords, R.drawable.ic_briefcase_medical, R.color.soft_coral, R.color.primary_coral,
            R.string.home_tool_records
        ) { openHealthcare() }

        bindCareTool(
            binding.toolMore, R.drawable.ic_grid_more, R.color.soft_blue, R.color.info_blue,
            R.string.home_tool_more
        ) { openProfile() }
    }

    private fun bindCareTool(
        tool: ItemHomeCareToolBinding,
        iconRes: Int,
        bgColorRes: Int,
        tintColorRes: Int,
        labelRes: Int,
        onClick: () -> Unit
    ) {
        tool.toolIcon.setImageResource(iconRes)
        tool.toolIconContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(
            androidx.core.content.ContextCompat.getColor(tool.root.context, bgColorRes)
        )
        tool.toolIcon.imageTintList = android.content.res.ColorStateList.valueOf(
            androidx.core.content.ContextCompat.getColor(tool.root.context, tintColorRes)
        )
        tool.toolLabel.text = getString(labelRes)
        tool.root.setOnClickListener { onClick() }
    }

    private fun renderRecentActivity(records: List<HealthcareRecordEntity>) {
        val binding = binding ?: return
        val recent = records.take(3)

        binding.homeRecentActivityContainer.removeAllViews()
        if (recent.isEmpty()) {
            binding.homeNoRecentActivity.isVisible = true
            return
        }
        binding.homeNoRecentActivity.isVisible = false

        val inflater = LayoutInflater.from(binding.root.context)
        recent.forEach { record ->
            val itemBinding = ItemHomeRecentActivityBinding.inflate(inflater, binding.homeRecentActivityContainer, false)
            bindActivityItem(itemBinding, record)
            binding.homeRecentActivityContainer.addView(itemBinding.root)
        }
    }

    private fun bindActivityItem(itemBinding: ItemHomeRecentActivityBinding, record: HealthcareRecordEntity) {
        val petName = allPets.find { it.id == record.petId }?.name ?: "Pet"
        itemBinding.activityTitle.text = getString(R.string.home_activity_pet_title_format, petName, record.title)
        itemBinding.activitySubtitle.text = record.clinicName?.takeIf { it.isNotBlank() }
            ?: record.notes?.takeIf { it.isNotBlank() }
            ?: humanizeHealthcareType(record.type)
        itemBinding.activityDate.text = formatActivityDate(record.date)

        itemBinding.activityIcon.setImageResource(healthcareIconFor(record.type))
        itemBinding.activityIconContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(
            androidx.core.content.ContextCompat.getColor(itemBinding.root.context, healthcareColorFor(record.type))
        )

        itemBinding.root.setOnClickListener {
            startActivity(
                Intent(requireContext(), HealthcareActivity::class.java).apply {
                    putExtra(HealthcareActivity.EXTRA_RECORD_ID, record.id)
                }
            )
        }
    }

    private fun formatActivityDate(millis: Long): String {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = millis }

        return when {
            today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) ->
                getString(R.string.home_activity_today_format, SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis)))

            today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR) == 1 ->
                getString(R.string.home_activity_yesterday)

            else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(millis))
        }
    }

    private fun renderPetCountSubtitle(binding: FragmentHomeBinding) {
        binding.homePetCountSubtitle.text = when (allPets.size) {
            0 -> getString(R.string.home_no_pets_subtitle)
            1 -> getString(R.string.home_proud_parent_one)
            else -> getString(R.string.home_proud_parent_many, allPets.size)
        }
        binding.homeActivePetsPill.text = getString(R.string.home_active_pets_pill_format, allPets.size)
    }

    private fun setupPetPager(binding: FragmentHomeBinding) {
        val adapter = PetPagerAdapter(onPetClick = { pet -> openPetDetails(pet.id) })
        petPagerAdapter = adapter
        binding.petPager.adapter = adapter
        binding.petPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                updatePageIndicator(binding)
                updateSelectedPetStats(binding)
            }
        })
    }

    private fun onPetsChanged(pets: List<PetEntity>) {
        allPets = pets
        binding?.let { renderPetCountSubtitle(it) }
        applyFilter()
    }

    private fun applyFilter() {
        val binding = binding ?: return
        filteredPets = when (selectedCategory) {
            PetCategoryFilter.ALL -> allPets
            PetCategoryFilter.DOGS -> allPets.filter { it.species.equals("Dog", ignoreCase = true) }
            PetCategoryFilter.CATS -> allPets.filter { it.species.equals("Cat", ignoreCase = true) }
        }

        when {
            allPets.isEmpty() -> {
                binding.homePetsSection.isVisible = false
                binding.petPager.isVisible = false
                binding.petPageIndicator.isVisible = false
                binding.homeEmptyPets.root.isVisible = true
                binding.homeEmptyFilteredPets.root.isVisible = false
                bindEmptyPets(binding)
            }
            filteredPets.isEmpty() -> {
                binding.homePetsSection.isVisible = false
                binding.petPager.isVisible = false
                binding.petPageIndicator.isVisible = false
                binding.homeEmptyPets.root.isVisible = false
                binding.homeEmptyFilteredPets.root.isVisible = true
                bindEmptyFilteredPets(binding)
            }
            else -> {
                binding.homePetsSection.isVisible = true
                binding.homeEmptyPets.root.isVisible = false
                binding.homeEmptyFilteredPets.root.isVisible = false
                binding.petPager.isVisible = true
                currentPage = currentPage.coerceIn(0, filteredPets.lastIndex)
                petPagerAdapter?.submitList(filteredPets, nextCheckupByPetId)
                if (binding.petPager.currentItem != currentPage) {
                    binding.petPager.setCurrentItem(currentPage, false)
                }
                updatePageIndicator(binding)
            }
        }
        updateSelectedPetStats(binding)
    }

    private fun bindEmptyPets(binding: FragmentHomeBinding) {
        val empty = binding.homeEmptyPets
        empty.emptyStateTitle.text = getString(R.string.home_no_pets_title)
        empty.emptyStateDescription.text = getString(R.string.home_no_pets_description)
        empty.emptyStateAction.text = getString(R.string.home_add_first_pet)
        empty.emptyStateAction.setOnClickListener { openPets() }
    }

    private fun bindEmptyFilteredPets(binding: FragmentHomeBinding) {
        val empty = binding.homeEmptyFilteredPets
        empty.emptyStateTitle.text = getString(R.string.home_no_pets_filtered_title)
        empty.emptyStateDescription.text = getString(R.string.home_no_pets_filtered_description)
        empty.emptyStateAction.text = getString(R.string.home_show_all_pets)
        empty.emptyStateAction.setOnClickListener {
            selectedCategory = PetCategoryFilter.ALL
            binding.chipDogs.isChecked = false
            binding.chipCats.isChecked = false
            applyFilter()
        }
    }

    private fun updatePageIndicator(binding: FragmentHomeBinding) {
        val container = binding.petPageIndicator
        if (filteredPets.size <= 1) {
            container.isVisible = false
            return
        }
        container.isVisible = true
        if (container.childCount != filteredPets.size) {
            container.removeAllViews()
            repeat(filteredPets.size) {
                val dot = View(container.context)
                val size = (6 * resources.displayMetrics.density).toInt()
                val params = android.widget.LinearLayout.LayoutParams(size, size)
                params.marginStart = (4 * resources.displayMetrics.density).toInt()
                params.marginEnd = (4 * resources.displayMetrics.density).toInt()
                dot.layoutParams = params
                container.addView(dot)
            }
        }
        for (i in 0 until container.childCount) {
            val dot = container.getChildAt(i)
            val isActive = i == currentPage
            val size = ((if (isActive) 8 else 6) * resources.displayMetrics.density).toInt()
            dot.layoutParams = dot.layoutParams.apply {
                width = size
                height = size
            }
            dot.background = androidx.core.content.ContextCompat.getDrawable(
                dot.context,
                R.drawable.bg_dot_indicator
            )
            dot.alpha = if (isActive) 1f else 0.35f
            dot.requestLayout()
        }
    }

    private fun updateSelectedPetStats(binding: FragmentHomeBinding) {
        val selectedPet = filteredPets.getOrNull(currentPage.coerceIn(0, (filteredPets.size - 1).coerceAtLeast(0)))
        binding.homeWeightValue.text = if (selectedPet != null && selectedPet.weight > 0) {
            getString(R.string.home_weight_format, selectedPet.weight.toString())
        } else {
            getString(R.string.home_weight_placeholder)
        }
        binding.homeAgeValue.text = if (selectedPet != null) {
            getString(R.string.pet_age_years, selectedPet.ageInYears())
        } else {
            getString(R.string.home_weight_placeholder)
        }
    }

    private fun setupCategoryChips(binding: FragmentHomeBinding) {
        binding.chipDogs.setOnClickListener {
            selectedCategory = if (selectedCategory == PetCategoryFilter.DOGS) {
                binding.chipDogs.isChecked = false
                PetCategoryFilter.ALL
            } else {
                binding.chipCats.isChecked = false
                PetCategoryFilter.DOGS
            }
            currentPage = 0
            applyFilter()
        }
        binding.chipCats.setOnClickListener {
            selectedCategory = if (selectedCategory == PetCategoryFilter.CATS) {
                binding.chipCats.isChecked = false
                PetCategoryFilter.ALL
            } else {
                binding.chipDogs.isChecked = false
                PetCategoryFilter.CATS
            }
            currentPage = 0
            applyFilter()
        }
        binding.chipVetCare.setOnClickListener { openHealthcare() }
    }

    private fun renderTasks(tasks: List<CareTaskUiModel>, completed: Int, total: Int) {
        val binding = binding ?: return

        val score = if (total > 0) (completed.toFloat() / total * 100).toInt() else 100
        binding.homeHealthScore.text = getString(R.string.home_health_score_format, score)
        binding.homeHealthProgress.progress = score

        binding.homeCareTaskContainer.removeAllViews()
        if (tasks.isEmpty()) {
            binding.homeNoTasksCard.isVisible = true
            binding.homeSeeAllTasks.isVisible = false
        } else {
            binding.homeNoTasksCard.isVisible = false
            val inflater = LayoutInflater.from(binding.root.context)
            tasks.take(3).forEach { taskUi ->
                val itemBinding = ItemHomeCareTaskBinding.inflate(inflater, binding.homeCareTaskContainer, false)
                bindTaskItem(itemBinding, taskUi)
                binding.homeCareTaskContainer.addView(itemBinding.root)
            }
            binding.homeSeeAllTasks.isVisible = tasks.size > 3
        }
    }

    private fun bindTaskItem(itemBinding: ItemHomeCareTaskBinding, taskUi: CareTaskUiModel) {
        itemBinding.taskTitle.text = taskUi.task.title
        itemBinding.taskTime.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(taskUi.task.dueAt))

        val iconRes = when (taskUi.task.category) {
            "FEEDING" -> R.drawable.ic_restaurant
            "EXERCISE" -> R.drawable.ic_directions_walk
            "MEDICATION" -> R.drawable.ic_medical
            else -> R.drawable.ic_assignment
        }
        val bgColorRes = when (taskUi.task.category) {
            "FEEDING" -> R.color.soft_yellow
            "EXERCISE" -> R.color.soft_green
            "MEDICATION" -> R.color.soft_blue
            else -> R.color.soft_lavender
        }
        itemBinding.taskIcon.setImageResource(iconRes)
        itemBinding.taskIconContainer.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(itemBinding.root.context, bgColorRes)
            )

        itemBinding.taskCompletionIcon.isVisible = taskUi.isCompleted
        itemBinding.taskCompletionPlaceholder.isVisible = !taskUi.isCompleted

        itemBinding.root.setOnClickListener {
            taskViewModel.toggleTaskCompletion(taskUi, taskViewModel.selectedDate.value)
        }
    }

    private val currencyFormatter: NumberFormat by lazy { NumberFormat.getCurrencyInstance(Locale("en", "NP")) }

    private fun setupFinanceSnippet(binding: FragmentHomeBinding) {
        binding.homeManageExpensesButton.setOnClickListener {
            startActivity(Intent(requireContext(), ExpensesActivity::class.java))
        }
    }

    private fun renderFinance(state: ExpenseUiState) {
        val binding = binding ?: return
        binding.homeMonthlySpendingValue.text = currencyFormatter.format(state.monthlySpending)

        val budgetAmount = state.budget?.amount ?: 0.0
        if (budgetAmount > 0) {
            val progress = (state.monthlySpending / budgetAmount * 100).toInt().coerceIn(0, 100)
            binding.homeBudgetProgress.isVisible = true
            binding.homeBudgetProgress.progress = progress
        } else {
            binding.homeBudgetProgress.isVisible = false
        }
    }

    private fun setupBottomNavigation(binding: FragmentHomeBinding) {
        val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            R.id.home_bottom_navigation
        )
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_pets -> { openPets(); false }
                R.id.nav_care -> { openTasks(); false }
                R.id.nav_health -> { openHealthcare(); false }
                R.id.nav_profile -> { openProfile(); false }
                else -> false
            }
        }
    }

    private fun openTasks() {
        startActivity(Intent(requireContext(), TasksActivity::class.java))
    }

    private fun openHealthcare() {
        startActivity(Intent(requireContext(), HealthcareActivity::class.java))
    }

    private fun openProfile() {
        startActivity(Intent(requireContext(), ProfileActivity::class.java))
    }

    private fun openPets() {
        startActivity(Intent(requireContext(), PetsActivity::class.java))
    }

    private fun openPetDetails(petId: Long) {
        startActivity(
            Intent(requireContext(), PetsActivity::class.java).apply {
                putExtra(PetsActivity.EXTRA_PET_ID, petId)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        petPagerAdapter = null
        binding = null
    }
}
