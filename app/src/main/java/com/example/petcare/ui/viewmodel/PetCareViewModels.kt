package com.example.petcare.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.local.entity.BudgetEntity
import com.example.petcare.data.local.entity.CareRoutineEntity
import com.example.petcare.data.local.entity.CareTaskEntity
import com.example.petcare.data.local.entity.DelegationEntity
import com.example.petcare.data.local.entity.ExpenseEntity
import com.example.petcare.data.local.entity.HealthcareRecordEntity
import com.example.petcare.data.local.entity.NotificationSettingsEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.data.local.entity.TaskCompletionEntity
import com.example.petcare.data.local.repository.PetCareRepository
import com.example.petcare.utils.DateUtils
import com.example.petcare.utils.ImageUtils
import com.example.petcare.utils.NotificationScheduler
import com.example.petcare.utils.SmsMessageGenerator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun currentUserIdOrEmpty(): String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

@OptIn(ExperimentalCoroutinesApi::class)
data class CareTaskUiModel(
    val task: CareTaskEntity,
    val isCompleted: Boolean,
    val completedAt: Long? = null,
    val petName: String = "Pet"
)

data class ChecklistUiState(
    val selectedDate: Long = DateUtils.startOfDay(System.currentTimeMillis()),
    val tasks: List<CareTaskUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val totalCount: Int = 0,
    val completedCount: Int = 0
)

sealed class ResetChecklistResult {
    object AlreadyClear : ResetChecklistResult()
    data class Reset(val count: Int) : ResetChecklistResult()
    data class Error(val message: String) : ResetChecklistResult()
}

data class PetFormState(
    val name: String = "",
    val species: String = "",
    val breed: String = "",
    val dateOfBirthMillis: Long? = null,
    val weight: String = "",
    val gender: String = "",
    val dietaryPreferences: String = "",
    val allergies: String = "",
    val vaccinationHistory: String = "",
    val favouriteToys: String = "",
    val medicalNotes: String = "",
    val generalNotes: String = "",
    val imageUri: String? = null
)

data class PetUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val pets: List<PetEntity> = emptyList(),
    val selectedPet: PetEntity? = null
)

data class HealthcareUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val records: List<HealthcareRecordEntity> = emptyList(),
    val selectedRecord: HealthcareRecordEntity? = null,
    val upcomingRecords: List<HealthcareRecordEntity> = emptyList()
)

data class HealthcareFormState(
    val type: String = "VETERINARY_APPOINTMENT",
    val title: String = "",
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val time: Long? = null,
    val veterinarianName: String = "",
    val clinicName: String = "",
    val medicationName: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val duration: String = "",
    val nextDueDate: Long? = null,
    val notes: String = "",
    val imageUri: String? = null,
    val reminderEnabled: Boolean = false,
    val createCareTask: Boolean = false
)

data class ExpenseUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val expenses: List<ExpenseEntity> = emptyList(),
    val selectedExpense: ExpenseEntity? = null,
    val totalSpending: Double = 0.0,
    val monthlySpending: Double = 0.0,
    val budget: BudgetEntity? = null
)

data class ExpenseFormState(
    val title: String = "",
    val description: String = "",
    val amount: String = "",
    val category: String = "Food",
    val date: Long = System.currentTimeMillis(),
    val petId: Long? = null,
    val paymentMethod: String = "Cash",
    val vendor: String = "",
    val notes: String = "",
    val imageUri: String? = null
)

data class DelegationUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val availableTasks: List<CareTaskUiModel> = emptyList(),
    val selectedTaskIds: Set<Long> = emptySet(),
    val recipientName: String = "",
    val recipientPhone: String = "",
    val generatedMessage: String = "",
    val history: List<DelegationEntity> = emptyList()
)

data class NotificationSettingsUiState(
    val settings: NotificationSettingsEntity? = null,
    val isLoading: Boolean = false,
    val successMessage: String? = null
)

class PetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PetCareRepository.getInstance(application)
    private var currentUserIdProvider: () -> String = { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    fun setCurrentUserIdForTesting(userId: String) {
        currentUserIdProvider = { userId }
        observeCurrentUserPets()
    }

    fun clearCurrentUserIdOverride() {
        currentUserIdProvider = { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    }

    private fun currentUserIdOrEmpty(): String = currentUserIdProvider()

    private val _pets = MutableStateFlow<List<PetEntity>>(emptyList())
    val pets: StateFlow<List<PetEntity>> = _pets.asStateFlow()

    private val _selectedPet = MutableStateFlow<PetEntity?>(null)
    val selectedPet: StateFlow<PetEntity?> = _selectedPet.asStateFlow()

    private val _petUiState = MutableStateFlow(PetUiState())
    val petUiState: StateFlow<PetUiState> = _petUiState.asStateFlow()

    private val _formState = MutableStateFlow(PetFormState())
    val formState: StateFlow<PetFormState> = _formState.asStateFlow()

    init {
        observeCurrentUserPets()
    }

    fun updateForm(formState: PetFormState) {
        _formState.value = formState
        _petUiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun resetForm() {
        _formState.value = PetFormState()
        _selectedPet.value = null
        _petUiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun loadPet(petId: Long) {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) {
            _petUiState.update { it.copy(isLoading = false, errorMessage = "Please sign in to continue.") }
            return
        }

        viewModelScope.launch {
            _petUiState.update { it.copy(isLoading = true, errorMessage = null) }
            val pet = repository.getPet(userId, petId)
            if (pet != null) {
                _selectedPet.value = pet
                _formState.value = pet.toFormState()
                _petUiState.update { it.copy(isLoading = false, errorMessage = null) }
            } else {
                _selectedPet.value = null
                _petUiState.update {
                    it.copy(isLoading = false, errorMessage = "Unable to load pet details. Please try again.")
                }
            }
        }
    }

    fun observeCurrentUserPets() {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) {
            _pets.value = emptyList()
            _petUiState.update { it.copy(isLoading = false, errorMessage = "User not authenticated.") }
            return
        }

        viewModelScope.launch {
            _petUiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.observePets(userId).collectLatest { petsList ->
                _pets.value = petsList
                _petUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        pets = petsList
                    )
                }
            }
        }
    }

    fun savePet(): Boolean {
        val userId = currentUserIdOrEmpty()
        val form = _formState.value
        val validationError = validatePetInput(form)
        if (validationError != null) {
            _petUiState.update { it.copy(errorMessage = validationError, successMessage = null) }
            return false
        }
        if (userId.isBlank()) {
            _petUiState.update { it.copy(errorMessage = "Please sign in to save your pet.") }
            return false
        }

        viewModelScope.launch {
            _petUiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            val finalImageUri = if (!form.imageUri.isNullOrBlank() && form.imageUri != _selectedPet.value?.imageUri) {
                val uri = Uri.parse(form.imageUri)
                if (uri.scheme == "content") {
                    val localPath = ImageUtils.saveImageToInternalStorage(getApplication(), uri)
                    if (localPath != null) {
                        ImageUtils.deleteImage(_selectedPet.value?.imageUri)
                        localPath
                    } else form.imageUri
                } else form.imageUri
            } else form.imageUri

            val pet = if (_selectedPet.value != null) {
                _selectedPet.value!!.copy(
                    userId = userId,
                    name = form.name.trim(),
                    species = form.species.trim(),
                    breed = form.breed.trim(),
                    dateOfBirth = form.dateOfBirthMillis,
                    weight = form.weight.toDoubleOrNull() ?: 0.0,
                    gender = form.gender,
                    dietaryPreferences = form.dietaryPreferences.trim(),
                    allergies = form.allergies.trim(),
                    vaccinationHistory = form.vaccinationHistory.trim(),
                    favouriteToys = form.favouriteToys.trim(),
                    medicalNotes = form.medicalNotes.trim(),
                    generalNotes = form.generalNotes.trim(),
                    imageUri = finalImageUri,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                PetEntity(
                    userId = userId,
                    name = form.name.trim(),
                    species = form.species.trim(),
                    breed = form.breed.trim(),
                    dateOfBirth = form.dateOfBirthMillis,
                    weight = form.weight.toDoubleOrNull() ?: 0.0,
                    gender = form.gender,
                    dietaryPreferences = form.dietaryPreferences.trim(),
                    allergies = form.allergies.trim(),
                    vaccinationHistory = form.vaccinationHistory.trim(),
                    favouriteToys = form.favouriteToys.trim(),
                    medicalNotes = form.medicalNotes.trim(),
                    generalNotes = form.generalNotes.trim(),
                    imageUri = finalImageUri
                )
            }

            if (_selectedPet.value != null) {
                repository.updatePet(pet)
                _petUiState.update { it.copy(isLoading = false, successMessage = "Pet updated successfully", errorMessage = null) }
            } else {
                repository.insertPet(pet)
                _petUiState.update { it.copy(isLoading = false, successMessage = "Pet added successfully", errorMessage = null) }
            }
            _selectedPet.value = pet
            observeCurrentUserPets()
        }
        return true
    }

    fun deletePet(petId: Long, onSuccess: (() -> Unit)? = null) {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) {
            _petUiState.update { it.copy(errorMessage = "Please sign in to delete a pet.") }
            return
        }

        viewModelScope.launch {
            _petUiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val pet = repository.getPet(userId, petId)
            if (pet != null) {
                ImageUtils.deleteImage(pet.imageUri)

                repository.deletePet(pet)
                observeCurrentUserPets()
                _petUiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Pet deleted successfully",
                        errorMessage = null
                    )
                }
                onSuccess?.invoke()
            } else {
                _petUiState.update {
                    it.copy(isLoading = false, errorMessage = "Pet not found. It may already be deleted.")
                }
            }
        }
    }

    companion object {
        fun validatePetInput(form: PetFormState): String? {
            if (form.name.trim().isBlank()) return "Pet name cannot be empty."
            if (form.species.trim().isBlank()) return "Species cannot be empty."
            val weightValue = form.weight.trim()
            if (weightValue.isNotBlank()) {
                val parsedWeight = weightValue.toDoubleOrNull()
                if (parsedWeight == null || parsedWeight <= 0) {
                    return "Weight must be a valid positive number."
                }
            }
            return null
        }

        fun PetEntity.toFormState(): PetFormState = PetFormState(
            name = name,
            species = species,
            breed = breed,
            dateOfBirthMillis = dateOfBirth,
            weight = if (weight > 0) weight.toString() else "",
            gender = gender,
            dietaryPreferences = dietaryPreferences,
            allergies = allergies,
            vaccinationHistory = vaccinationHistory,
            favouriteToys = favouriteToys,
            medicalNotes = medicalNotes,
            generalNotes = generalNotes,
            imageUri = imageUri
        )
    }
}

fun PetEntity.ageInYears(): Int {
    val birthMillis = dateOfBirth ?: return 0
    val calendar = Calendar.getInstance()
    val birth = Calendar.getInstance().apply { timeInMillis = birthMillis }
    var age = calendar.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
    if (calendar.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
        age -= 1
    }
    return if (age < 0) 0 else age
}

fun PetEntity.formattedDate(): String {
    val millis = dateOfBirth ?: return "Not provided"
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    return String.format(Locale.getDefault(), "%1\$td %1\$tB %1\$tY", calendar)
}

data class RoutineUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class CareRoutineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PetCareRepository.getInstance(application)

    private val _uiState = MutableStateFlow(RoutineUiState())
    val uiState: StateFlow<RoutineUiState> = _uiState.asStateFlow()

    fun observeRoutinesForPet(petId: Long): Flow<List<CareRoutineEntity>> =
        repository.observeCareRoutinesForPet(currentUserIdOrEmpty(), petId)

    val routines: StateFlow<List<CareRoutineEntity>> = repository.observeCareRoutines(currentUserIdOrEmpty())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addRoutine(routine: CareRoutineEntity) {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "User not authenticated.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.insertCareRoutine(routine.copy(userId = userId))
            _uiState.update { it.copy(isLoading = false, successMessage = "Care routine created successfully") }
        }
    }

    fun updateRoutine(routine: CareRoutineEntity) {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.updateCareRoutine(routine.copy(userId = userId))
            _uiState.update { it.copy(isLoading = false, successMessage = "Care routine updated successfully") }
        }
    }

    fun deleteRoutine(routine: CareRoutineEntity) {
        viewModelScope.launch {
            repository.deleteCareRoutine(routine)
            _uiState.update { it.copy(successMessage = "Care routine deleted successfully") }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CareTaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PetCareRepository.getInstance(application)
    private var currentUserIdProvider: () -> String = { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    fun setCurrentUserIdForTesting(userId: String) {
        currentUserIdProvider = { userId }
        observeChecklist()
    }

    fun clearCurrentUserIdOverride() {
        currentUserIdProvider = { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    }

    private fun currentUserIdOrEmpty(): String = currentUserIdProvider()

    private val _selectedDate = MutableStateFlow(DateUtils.startOfDay(System.currentTimeMillis()))
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _checklistState = MutableStateFlow(ChecklistUiState())
    val checklistState: StateFlow<ChecklistUiState> = _checklistState.asStateFlow()

    val tasks: StateFlow<List<CareTaskEntity>> = repository.observeCareTasks(currentUserIdOrEmpty())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        observeChecklist()
    }

    private fun observeChecklist() {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) return

        viewModelScope.launch {
            combine(
                _selectedDate,
                repository.observeCareTasks(userId),
                repository.observePets(userId)
            ) { date, tasks, pets ->
                Triple(date, tasks, pets)
            }.flatMapLatest { (date, allTasks, pets) ->
                repository.observeCompletionsForDate(userId, date).combine(repository.observePets(userId)) { completions, userPets ->
                    val filteredTasks = allTasks.filter { task ->
                        DateUtils.isTaskActiveOnDate(task, date)
                    }.map { task ->
                        val completion = completions.find { it.taskId == task.id }
                        CareTaskUiModel(
                            task = task,
                            isCompleted = completion?.isCompleted ?: (task.frequency == "ONCE" && task.isCompleted),
                            completedAt = completion?.completedAt ?: if (task.frequency == "ONCE") task.completedAt else null,
                            petName = userPets.find { it.id == task.petId }?.name ?: "Pet"
                        )
                    }.sortedBy { it.task.dueAt }

                    ChecklistUiState(
                        selectedDate = date,
                        tasks = filteredTasks,
                        totalCount = filteredTasks.size,
                        completedCount = filteredTasks.count { it.isCompleted }
                    )
                }
            }.collectLatest { state ->
                _checklistState.value = state
            }
        }
    }

    fun setSelectedDate(millis: Long) {
        _selectedDate.value = DateUtils.startOfDay(millis)
    }

    fun toggleTaskCompletion(taskUi: CareTaskUiModel, dateMillis: Long) {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) return

        val normalizedDate = DateUtils.startOfDay(dateMillis)

        viewModelScope.launch {
            if (taskUi.task.frequency == "ONCE") {
                repository.updateCareTask(taskUi.task.copy(
                    isCompleted = !taskUi.isCompleted,
                    completedAt = if (!taskUi.isCompleted) System.currentTimeMillis() else null
                ))
            } else {
                if (taskUi.isCompleted) {
                    repository.markTaskIncomplete(userId, taskUi.task.id, normalizedDate)
                } else {
                    repository.markTaskComplete(userId, taskUi.task.id, normalizedDate)
                }
            }
        }
    }

    fun observePendingTasks(): Flow<List<CareTaskEntity>> = repository.observePendingCareTasks(currentUserIdOrEmpty())

    fun observeTasksForPet(petId: Long, date: Long): Flow<List<CareTaskUiModel>> {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())

        return repository.observeCareTasksForPet(userId, petId).combine(
            repository.observeCompletionsForDate(userId, date)
        ) { tasks, completions ->
            tasks.filter { task ->
                DateUtils.isTaskActiveOnDate(task, date)
            }.map { task ->
                val completion = completions.find { it.taskId == task.id }
                CareTaskUiModel(
                    task = task,
                    isCompleted = completion?.isCompleted ?: (task.frequency == "ONCE" && task.isCompleted),
                    completedAt = completion?.completedAt ?: if (task.frequency == "ONCE") task.completedAt else null,
                    petName = ""
                )
            }.sortedBy { it.task.dueAt }
        }
    }

    fun observeTasksForRoutine(routineId: Long): Flow<List<CareTaskEntity>> =
        repository.observeCareTasksByRoutine(currentUserIdOrEmpty(), routineId)

    fun addTask(task: CareTaskEntity) {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) return
        viewModelScope.launch {
            val insertedId = repository.insertCareTask(task.copy(userId = userId))
            if (task.reminderEnabled) {
                NotificationScheduler.scheduleCareTaskReminder(
                    getApplication(),
                    task.copy(id = insertedId, userId = userId)
                )
            }
        }
    }

    fun updateTask(task: CareTaskEntity) {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) return
        viewModelScope.launch {
            repository.updateCareTask(task.copy(userId = userId))
            if (task.reminderEnabled) {
                NotificationScheduler.scheduleCareTaskReminder(getApplication(), task)
            } else {
                NotificationScheduler.cancelCareTaskReminder(getApplication(), task)
            }
        }
    }

    fun deleteTask(task: CareTaskEntity) {
        viewModelScope.launch {
            NotificationScheduler.cancelCareTaskReminder(getApplication(), task)
            repository.deleteCareTask(task)
        }
    }

    fun resetTodayChecklist(onResult: (ResetChecklistResult) -> Unit) {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) {
            onResult(ResetChecklistResult.Error("Please sign in to continue."))
            return
        }

        val today = DateUtils.startOfDay(System.currentTimeMillis())

        viewModelScope.launch {
            try {
                val todaysCompletions = repository.observeCompletionsForDate(userId, today).first()
                val completedOnceTasksToday = repository.observeCareTasks(userId).first().filter { task ->
                    task.frequency == "ONCE" && task.isCompleted && DateUtils.startOfDay(task.dueAt) == today
                }

                if (todaysCompletions.isEmpty() && completedOnceTasksToday.isEmpty()) {
                    onResult(ResetChecklistResult.AlreadyClear)
                    return@launch
                }

                todaysCompletions.forEach { completion ->
                    repository.markTaskIncomplete(userId, completion.taskId, today)
                }
                completedOnceTasksToday.forEach { task ->
                    repository.updateCareTask(task.copy(isCompleted = false, completedAt = null))
                }

                onResult(ResetChecklistResult.Reset(todaysCompletions.size + completedOnceTasksToday.size))
            } catch (e: Exception) {
                onResult(ResetChecklistResult.Error(e.message ?: "Unable to reset today's checklist. Please try again."))
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HealthcareRecordViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PetCareRepository.getInstance(application)
    private var currentUserIdProvider: () -> String = { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    private val currentUserIdFlow = MutableStateFlow(currentUserIdProvider())

    fun setCurrentUserIdForTesting(userId: String) {
        currentUserIdProvider = { userId }
        currentUserIdFlow.value = userId
    }

    fun clearCurrentUserIdOverride() {
        currentUserIdProvider = { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
        currentUserIdFlow.value = currentUserIdProvider()
    }

    private fun currentUserIdOrEmpty(): String = currentUserIdProvider()

    private val _uiState = MutableStateFlow(HealthcareUiState())
    val uiState: StateFlow<HealthcareUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(HealthcareFormState())
    val formState: StateFlow<HealthcareFormState> = _formState.asStateFlow()

    val records: StateFlow<List<HealthcareRecordEntity>> = currentUserIdFlow
        .flatMapLatest { userId -> repository.observeHealthcareRecords(userId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val upcomingRecords: StateFlow<List<HealthcareRecordEntity>> = currentUserIdFlow
        .flatMapLatest { userId -> repository.observeUpcomingHealthcare(userId, System.currentTimeMillis()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun observeRecordsForPet(petId: Long): Flow<List<HealthcareRecordEntity>> =
        repository.observeHealthcareRecordsForPet(currentUserIdOrEmpty(), petId)

    fun updateForm(form: HealthcareFormState) {
        _formState.value = form
    }

    fun resetForm() {
        _formState.value = HealthcareFormState()
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun loadRecord(recordId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val record = repository.getHealthcareRecordById(currentUserIdOrEmpty(), recordId)
            if (record != null) {
                _uiState.update { it.copy(isLoading = false, selectedRecord = record) }
                _formState.value = record.toFormState()
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Record not found") }
            }
        }
    }

    fun saveRecord(petId: Long) {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) return

        val form = _formState.value
        if (form.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val record = HealthcareRecordEntity(
                id = _uiState.value.selectedRecord?.id ?: 0,
                userId = userId,
                petId = petId,
                type = form.type,
                title = form.title.trim(),
                description = form.description.trim(),
                date = form.date,
                time = form.time,
                veterinarianName = form.veterinarianName.trim(),
                clinicName = form.clinicName.trim(),
                medicationName = form.medicationName.trim(),
                dosage = form.dosage.trim(),
                frequency = form.frequency.trim(),
                duration = form.duration.trim(),
                nextDueDate = form.nextDueDate,
                reminderEnabled = form.reminderEnabled,
                notes = form.notes.trim(),
                imageUri = form.imageUri
            )

            if (record.id == 0L) {
                val insertedId = repository.insertHealthcareRecord(record)
                val finalRecord = record.copy(id = insertedId)

                if (record.reminderEnabled) {
                    NotificationScheduler.scheduleHealthcareReminder(getApplication(), finalRecord)
                }

                if (form.createCareTask && form.type == "MEDICATION") {
                    val task = CareTaskEntity(
                        userId = userId,
                        petId = petId,
                        title = "Take ${form.medicationName}",
                        description = "Dosage: ${form.dosage}, Frequency: ${form.frequency}",
                        category = "MEDICATION",
                        dueAt = form.date,
                        frequency = when (form.frequency.uppercase()) {
                            "DAILY" -> "DAILY"
                            "WEEKLY" -> "WEEKLY"
                            else -> "ONCE"
                        },
                        notes = form.notes
                    )
                    repository.insertCareTask(task)
                }

                _uiState.update { it.copy(isLoading = false, successMessage = "Record added successfully") }
            } else {
                repository.updateHealthcareRecord(record)
                if (record.reminderEnabled) {
                    NotificationScheduler.scheduleHealthcareReminder(getApplication(), record)
                } else {
                    NotificationScheduler.cancelHealthcareReminder(getApplication(), record)
                }
                _uiState.update { it.copy(isLoading = false, successMessage = "Record updated successfully") }
            }
        }
    }

    fun deleteRecord(record: HealthcareRecordEntity) {
        viewModelScope.launch {
            NotificationScheduler.cancelHealthcareReminder(getApplication(), record)
            repository.deleteHealthcareRecord(record)
            _uiState.update { it.copy(successMessage = "Record deleted successfully") }
        }
    }

    fun importVeterinaryRecords() {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please sign in to import records.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val externalRecords = repository.fetchExternalVeterinaryRecords()
                if (externalRecords.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, successMessage = "No veterinary records found to import.") }
                    return@launch
                }

                val pets = repository.observePets(userId).first()
                var importedCount = 0
                var skippedCount = 0

                externalRecords.forEach { external ->
                    val pet = pets.find {
                        it.name.equals(external.name, ignoreCase = true) &&
                            it.species.equals(external.species, ignoreCase = true)
                    }
                    if (pet == null) {
                        skippedCount++
                        return@forEach
                    }

                    val vaccinationDate = parseIsoDate(external.vaccinationDate)
                    if (!external.vaccination.isNullOrBlank() && vaccinationDate != null) {
                        val added = importRecordIfNew(
                            userId = userId,
                            petId = pet.id,
                            type = "VACCINATION",
                            title = external.vaccination,
                            date = vaccinationDate,
                            nextDueDate = parseIsoDate(external.nextVaccination),
                            clinicName = external.clinicName
                        )
                        if (added) importedCount++ else skippedCount++
                    }

                    val appointmentDate = parseIsoDate(external.appointmentDate)
                    if (appointmentDate != null) {
                        val added = importRecordIfNew(
                            userId = userId,
                            petId = pet.id,
                            type = "VETERINARY_APPOINTMENT",
                            title = "Veterinary Appointment",
                            date = appointmentDate,
                            nextDueDate = null,
                            clinicName = external.clinicName
                        )
                        if (added) importedCount++ else skippedCount++
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Imported $importedCount record(s). Skipped $skippedCount (already imported or no matching pet)."
                    )
                }
            } catch (e: java.io.IOException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "No internet connection. Please check your connection and try again.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to import veterinary data. Please try again.") }
            }
        }
    }

    private suspend fun importRecordIfNew(
        userId: String,
        petId: Long,
        type: String,
        title: String,
        date: Long,
        nextDueDate: Long?,
        clinicName: String?
    ): Boolean {
        if (repository.healthcareRecordExists(userId, petId, type, title, date)) return false
        repository.insertHealthcareRecord(
            HealthcareRecordEntity(
                userId = userId,
                petId = petId,
                type = type,
                title = title,
                date = date,
                nextDueDate = nextDueDate,
                clinicName = clinicName,
                notes = "Imported from Veterinary API"
            )
        )
        return true
    }

    private fun parseIsoDate(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(value)?.time
        } catch (e: Exception) {
            null
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun HealthcareRecordEntity.toFormState() = HealthcareFormState(
        type = type,
        title = title,
        description = description ?: "",
        date = date,
        time = time,
        veterinarianName = veterinarianName ?: "",
        clinicName = clinicName ?: "",
        medicationName = medicationName ?: "",
        dosage = dosage ?: "",
        frequency = frequency ?: "",
        duration = duration ?: "",
        nextDueDate = nextDueDate,
        reminderEnabled = reminderEnabled,
        notes = notes ?: "",
        imageUri = imageUri
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PetCareRepository.getInstance(application)
    private var currentUserIdProvider: () -> String = { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    private val currentUserIdFlow = MutableStateFlow(currentUserIdProvider())

    fun setCurrentUserIdForTesting(userId: String) {
        currentUserIdProvider = { userId }
        currentUserIdFlow.value = userId
    }

    fun clearCurrentUserIdOverride() {
        currentUserIdProvider = { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
        currentUserIdFlow.value = currentUserIdProvider()
    }

    private fun currentUserIdOrEmpty(): String = currentUserIdProvider()

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(ExpenseFormState())
    val formState: StateFlow<ExpenseFormState> = _formState.asStateFlow()

    val expenses: StateFlow<List<ExpenseEntity>> = currentUserIdFlow
        .flatMapLatest { userId -> repository.observeExpenses(userId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val budget: StateFlow<BudgetEntity?> = currentUserIdFlow
        .flatMapLatest { userId -> repository.observeBudget(userId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    init {
        observeSpending()
    }

    fun observeExpensesForPet(petId: Long): Flow<List<ExpenseEntity>> =
        repository.observeExpensesForPet(currentUserIdOrEmpty(), petId)

    private fun observeSpending() {
        viewModelScope.launch {
            currentUserIdFlow.flatMapLatest { userId ->
                val startOfMonth = getStartOfMonth()
                val endOfMonth = getEndOfMonth()

                combine(
                    repository.observeTotalSpending(userId),
                    repository.observeSpendingInRange(userId, startOfMonth, endOfMonth),
                    repository.observeBudget(userId)
                ) { total, monthly, currentBudget ->
                    Triple(total ?: 0.0, monthly ?: 0.0, currentBudget)
                }
            }.collectLatest { (total, monthly, currentBudget) ->
                _uiState.update { it.copy(
                    totalSpending = total,
                    monthlySpending = monthly,
                    budget = currentBudget
                ) }
            }
        }
    }

    fun updateForm(form: ExpenseFormState) {
        _formState.value = form
    }

    fun resetForm() {
        _formState.value = ExpenseFormState()
        _uiState.update { it.copy(errorMessage = null, successMessage = null, selectedExpense = null) }
    }

    fun loadExpense(expenseId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val expense = repository.getExpenseById(currentUserIdOrEmpty(), expenseId)
            if (expense != null) {
                _uiState.update { it.copy(isLoading = false, selectedExpense = expense) }
                _formState.value = expense.toFormState()
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Expense not found") }
            }
        }
    }

    fun saveExpense() {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please sign in to log expenses") }
            return
        }

        val form = _formState.value
        if (form.title.trim().isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title is required") }
            return
        }
        val amount = form.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Valid amount is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val expense = ExpenseEntity(
                    id = _uiState.value.selectedExpense?.id ?: 0,
                    userId = userId,
                    petId = form.petId,
                    title = form.title.trim(),
                    description = form.description.trim(),
                    amount = amount,
                    category = form.category,
                    date = form.date,
                    paymentMethod = form.paymentMethod,
                    vendor = form.vendor.trim(),
                    notes = form.notes.trim(),
                    imageUri = form.imageUri
                )

                if (expense.id == 0L) {
                    repository.insertExpense(expense)
                    _uiState.update { it.copy(isLoading = false, successMessage = "Expense added successfully") }
                } else {
                    repository.updateExpense(expense)
                    _uiState.update { it.copy(isLoading = false, successMessage = "Expense updated successfully") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to save: ${e.message}") }
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _uiState.update { it.copy(successMessage = "Expense deleted successfully") }
        }
    }

    fun setBudget(amount: Double) {
        val userId = currentUserIdOrEmpty()
        if (userId.isBlank()) return

        viewModelScope.launch {
            val current = repository.getBudget(userId)
            if (current == null) {
                repository.insertBudget(BudgetEntity(userId = userId, amount = amount))
            } else {
                repository.updateBudget(current.copy(amount = amount))
            }
            _uiState.update { it.copy(successMessage = "Budget updated successfully") }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun ExpenseEntity.toFormState() = ExpenseFormState(
        title = title,
        description = description,
        amount = amount.toString(),
        category = category,
        date = date,
        petId = petId,
        paymentMethod = paymentMethod,
        vendor = vendor,
        notes = notes,
        imageUri = imageUri
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class DelegationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PetCareRepository.getInstance(application)
    private var currentUserIdProvider: () -> String = { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    private val currentUserIdFlow = MutableStateFlow(currentUserIdProvider())

    fun setCurrentUserIdForTesting(userId: String) {
        currentUserIdProvider = { userId }
        currentUserIdFlow.value = userId
    }

    fun clearCurrentUserIdOverride() {
        currentUserIdProvider = { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
        currentUserIdFlow.value = currentUserIdProvider()
    }

    private fun currentUserIdOrEmpty(): String = currentUserIdProvider()

    private val _uiState = MutableStateFlow(DelegationUiState())
    val uiState: StateFlow<DelegationUiState> = _uiState.asStateFlow()

    private val _selectedPetId = MutableStateFlow<Long?>(null)
    private val _selectedDate = MutableStateFlow(DateUtils.startOfDay(System.currentTimeMillis()))

    private var currentPet: PetEntity? = null
    private var currentHealthRecords: List<HealthcareRecordEntity> = emptyList()
    private var currentRoutines: List<CareRoutineEntity> = emptyList()

    val history: StateFlow<List<DelegationEntity>> = currentUserIdFlow
        .flatMapLatest { userId -> repository.observeDelegations(userId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    init {
        observeTasksForDelegation()
        observePetDetailsForDelegation()
    }

    // Feeds the SMS message with the selected pet's real allergies/medical notes,
    // full healthcare record history, and care routines - so a sitter/family member
    // gets everything relevant, not just the day's task checklist.
    private fun observePetDetailsForDelegation() {
        viewModelScope.launch {
            combine(currentUserIdFlow, _selectedPetId) { userId, petId -> userId to petId }
                .flatMapLatest { (userId, petId) ->
                    if (userId.isBlank() || petId == null) {
                        kotlinx.coroutines.flow.flowOf(Triple<PetEntity?, List<HealthcareRecordEntity>, List<CareRoutineEntity>>(null, emptyList(), emptyList()))
                    } else {
                        repository.observeHealthcareRecordsForPet(userId, petId)
                            .combine(repository.observeCareRoutinesForPet(userId, petId)) { records, routines -> records to routines }
                            .map { (records, routines) -> Triple(repository.getPet(userId, petId), records, routines) }
                    }
                }.collectLatest { (pet, records, routines) ->
                    currentPet = pet
                    currentHealthRecords = records
                    currentRoutines = routines
                    updateGeneratedMessage()
                }
        }
    }

    private fun observeTasksForDelegation() {
        viewModelScope.launch {
            combine(currentUserIdFlow, _selectedPetId, _selectedDate) { userId, petId, date ->
                Triple(userId, petId, date)
            }.flatMapLatest { (userId, petId, date) ->
                repository.observeCareTasks(userId).combine(repository.observeCompletionsForDate(userId, date)) { tasks, completions ->
                    val pet = if (petId != null) repository.getPet(userId, petId) else null
                    val filtered = tasks.filter { task ->
                        (petId == null || task.petId == petId) && DateUtils.isTaskActiveOnDate(task, date)
                    }.map { task ->
                        val completion = completions.find { it.taskId == task.id }
                        CareTaskUiModel(
                            task = task,
                            isCompleted = completion?.isCompleted ?: (task.frequency == "ONCE" && task.isCompleted),
                            petName = pet?.name ?: "Pet"
                        )
                    }
                    filtered
                }
            }.collectLatest { tasks ->
                _uiState.update { it.copy(availableTasks = tasks) }
                updateGeneratedMessage()
            }
        }
    }

    fun setPetAndDate(petId: Long?, dateMillis: Long) {
        _selectedPetId.value = petId
        _selectedDate.value = DateUtils.startOfDay(dateMillis)
    }

    fun preselectTask(taskId: Long?) {
        if (taskId != null) {
            _uiState.update { it.copy(selectedTaskIds = setOf(taskId)) }
            updateGeneratedMessage()
        }
    }

    fun toggleTaskSelection(taskId: Long) {
        _uiState.update { state ->
            val newSelection = if (state.selectedTaskIds.contains(taskId)) {
                state.selectedTaskIds - taskId
            } else {
                state.selectedTaskIds + taskId
            }
            state.copy(selectedTaskIds = newSelection)
        }
        updateGeneratedMessage()
    }

    fun updateRecipient(name: String, phone: String) {
        _uiState.update { it.copy(recipientName = name, recipientPhone = phone) }
        updateGeneratedMessage()
    }

    private fun updateGeneratedMessage() {
        val state = _uiState.value
        val selectedTasks = state.availableTasks.filter { state.selectedTaskIds.contains(it.task.id) }
        val petName = if (_selectedPetId.value != null) {
            state.availableTasks.firstOrNull()?.petName ?: "Pet"
        } else "My Pets"

        val msg = SmsMessageGenerator.generateDelegationMessage(
            recipientName = state.recipientName,
            petName = petName,
            dateMillis = _selectedDate.value,
            selectedTasks = selectedTasks,
            pet = currentPet,
            healthRecords = currentHealthRecords,
            routines = currentRoutines
        )
        _uiState.update { it.copy(generatedMessage = msg) }
    }

    fun saveDelegation() {
        val userId = currentUserIdOrEmpty()
        val state = _uiState.value
        if (userId.isBlank() || state.selectedTaskIds.isEmpty() || state.recipientPhone.isBlank()) return

        viewModelScope.launch {
            val petId = _selectedPetId.value ?: state.availableTasks.firstOrNull()?.task?.petId ?: 0L
            val taskTitles = state.availableTasks
                .filter { state.selectedTaskIds.contains(it.task.id) }
                .joinToString(", ") { it.task.title }

            val delegation = DelegationEntity(
                userId = userId,
                petId = petId,
                recipientName = state.recipientName,
                recipientPhone = state.recipientPhone,
                taskTitles = taskTitles,
                dateMillis = _selectedDate.value,
                message = state.generatedMessage
            )
            repository.insertDelegation(delegation)
        }
    }

    fun deleteDelegation(delegation: DelegationEntity) {
        viewModelScope.launch {
            repository.deleteDelegation(delegation)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PetCareRepository.getInstance(application)
    private var currentUserIdProvider: () -> String = { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    private val currentUserIdFlow = MutableStateFlow(currentUserIdProvider())

    fun setCurrentUserIdForTesting(userId: String) {
        currentUserIdProvider = { userId }
        currentUserIdFlow.value = userId
    }

    fun clearCurrentUserIdOverride() {
        currentUserIdProvider = { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
        currentUserIdFlow.value = currentUserIdProvider()
    }

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            currentUserIdFlow.flatMapLatest { userId ->
                if (userId.isBlank()) kotlinx.coroutines.flow.flowOf(null) else repository.observeNotificationSettings(userId)
            }.collectLatest { settings ->
                val userId = currentUserIdProvider()
                if (settings == null && userId.isNotBlank()) {
                    repository.insertNotificationSettings(NotificationSettingsEntity(userId = userId))
                } else {
                    _uiState.update { it.copy(settings = settings) }
                }
            }
        }
    }

    fun updateSettings(settings: NotificationSettingsEntity) {
        viewModelScope.launch {
            repository.updateNotificationSettings(settings)
            _uiState.update { it.copy(successMessage = "Settings updated") }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
