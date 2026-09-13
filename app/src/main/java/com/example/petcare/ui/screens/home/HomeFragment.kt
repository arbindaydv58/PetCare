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
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.databinding.FragmentHomeBinding
import com.example.petcare.databinding.ItemHomeCareTaskBinding
import com.example.petcare.ui.viewmodel.CareTaskUiModel
import com.example.petcare.ui.viewmodel.CareTaskViewModel
import com.example.petcare.ui.viewmodel.ExpenseUiState
import com.example.petcare.ui.viewmodel.ExpenseViewModel
import com.example.petcare.ui.viewmodel.PetViewModel
import com.example.petcare.ui.viewmodel.ageInYears
import com.example.petcare.ui.screens.checklist.TasksActivity
import com.example.petcare.ui.screens.expenses.ExpensesActivity
import com.example.petcare.ui.screens.healthcare.HealthcareActivity
import com.example.petcare.ui.screens.pet.PetsActivity
import com.example.petcare.ui.screens.profile.ProfileActivity
import com.example.petcare.ui.screens.settings.NotificationSettingsActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private val petViewModel: PetViewModel by viewModels()
    private val taskViewModel: CareTaskViewModel by viewModels()
    private val expenseViewModel: ExpenseViewModel by viewModels()

    private var binding: FragmentHomeBinding? = null
    private var petPagerAdapter: PetPagerAdapter? = null

    private enum class PetCategoryFilter { ALL, DOGS, CATS }

    private var selectedCategory = PetCategoryFilter.ALL
    private var allPets: List<PetEntity> = emptyList()
    private var filteredPets: List<PetEntity> = emptyList()
    private var currentPage = 0

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

        val userName = FirebaseAuth.getInstance().currentUser?.displayName
            ?: getString(R.string.home_default_user_name)
        binding.homeGreeting.text = getString(R.string.home_greeting_format, userName)

        binding.homeNotificationButton.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationSettingsActivity::class.java))
        }

        setupPetPager(binding)
        setupCategoryChips(binding)
        setupFinanceSnippet(binding)
        setupBottomNavigation(binding)

        binding.homeSeeAllTasks.setOnClickListener {
            openTasks()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { petViewModel.pets.collect { pets -> onPetsChanged(pets) } }
                launch { taskViewModel.checklistState.collect { state -> renderTasks(state.tasks, state.completedCount, state.totalCount) } }
                launch { expenseViewModel.uiState.collect { state -> renderFinance(state) } }
            }
        }
    }

    private fun setupPetPager(binding: FragmentHomeBinding) {
        val adapter = PetPagerAdapter(onPetClick = { openPets() })
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
                petPagerAdapter?.submitList(filteredPets)
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

    override fun onDestroyView() {
        super.onDestroyView()
        petPagerAdapter = null
        binding = null
    }
}
