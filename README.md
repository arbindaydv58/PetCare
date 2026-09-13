# PetCare 🐾

## 1. Project Overview
PetCare is a comprehensive Android application designed to empower pet parents with tools to manage their pets' daily lives. The application provides a centralized platform for tracking care routines, health records, expenses, and delegating responsibilities to others. Every screen, list, and form in the app is native Android **XML layouts + Fragments/Activities** (View system), backed by Room for local persistence and Firebase for authentication.

## 2. Migration Note: Compose → XML/Fragments
The app was originally prototyped in Jetpack Compose and was migrated, screen by screen, to the traditional Android View system (XML layouts, Fragments, ViewBinding, RecyclerView). The migration was carried out in 12 stages, each one replacing a Compose screen with its XML/Fragment equivalent while keeping the same ViewModels, StateFlows, and business logic untouched:

| Stage | Area migrated |
|---|---|
| 1 | Authentication (Login / Register / Forgot Password) |
| 2 | Home dashboard |
| 3 | My Pets (list, details, add/edit form) |
| 4 | Care Routines (list, details, add/edit form) |
| 5–6 | Care Tasks / Daily & Weekly Checklists (incl. gestures) |
| 7 | Healthcare records (list, details, add/edit form) |
| 8 | Expenses & Budget |
| 9 | Delegation (SMS instruction builder) |
| 10 | Notification deep links & Notification Settings |
| 11 | Profile |
| 12 | Removal of the Compose navigation host/bridge and all remaining Compose screens/components |

As of the end of Stage 12, there is no Compose UI code left in the app — `MainActivity` no longer hosts a Compose `NavHost` and only decides which native Activity to launch. Every feature area lives in its own `AppCompatActivity` that hosts one or more `Fragment`s via `FragmentManager`, using ViewBinding for view access.

## 3. Application Features
The following core functionalities are implemented in PetCare:

- **Home Screen**: A personalized dashboard with a swipeable pet carousel (`ViewPager2`), category filter chips, quick stats (weight/activity), and a notification shortcut.
- **User Authentication**: Secure user registration, login, and password reset powered by Firebase Authentication, ensuring data privacy and individual ownership.
- **Pet Management**: Full CRUD operations for pet profiles, including name, species, breed, birth date, weight, gender, and permanent photo storage.
- **Care Routines**: Ability to create recurring care routines (Daily, Weekly, Monthly) tailored to each pet's needs, with a routine-details checklist view.
- **Care Task Management**: Automatic generation of tasks from routines and manual task creation with scheduling and reminder support.
- **Task Tracking**: Interactive Daily and Weekly checklists where users can mark tasks as completed for specific occurrences, grouped by category (Feeding, Exercise, Medication, etc.).
- **Healthcare Records**: Comprehensive logging of medical events such as vaccinations, veterinary appointments, and medication history, with reminder notifications.
- **Expense Tracking**: Categorized spending logs (Food, Health, Grooming, etc.) with a details view and pet-specific summaries.
- **Budgeting**: Monthly budget settings with visual progress indicators and spending analysis.
- **Pet Care Delegation**: A module for building structured care instructions and sending them to sitters or family members via SMS, plus a delegation history log.
- **Notification Settings**: Per-user control over reminder notifications, wired to `AlarmManager`-scheduled alerts that survive device reboot.
- **Profile**: Account details and sign-out.

## 4. Desirable Feature — Gesture Controls
The primary desirable feature implemented in PetCare is **Gesture Controls**, covering three distinct task-management gestures:

- **Swipe right → Complete**: Marks the current task occurrence as done for that specific date.
- **Swipe left → Delete**: Opens the same delete-confirmation dialog as the toolbar delete action for that exact task.
- **Shake device → Reset today's checklist**: Un-completes every task finished today, leaving other days and the task definitions themselves untouched.
- **How to access it**: The swipe gestures work on any task card in the Daily Checklist (`TasksActivity` / `DailyChecklistFragment`). The shake gesture is active while that screen is resumed.
- **How it works**: Swipes are implemented with `RecyclerView.ItemTouchHelper.SimpleCallback` (`attachTaskSwipeGestures` in `TaskAdapter.kt`), which always animates the row back to its resting position — the swipe itself never removes the row, it only triggers the toggle/delete action, so deletion still requires explicit confirmation. The shake gesture uses `SensorManager` with `TYPE_ACCELEROMETER` via a small `ShakeEventListener` class (`utils/ShakeDetector.kt`), registered/unregistered in the Fragment's `onResume`/`onPause`, detecting gravity-magnitude spikes with a debounce/cooldown window so one physical shake doesn't fire multiple resets.

## 5. Technology Stack
- **Programming Language**: Kotlin 2.3.10 (JVM 17)
- **Minimum SDK**: 24 (Android 7.0)
- **Target / Compile SDK**: 36 (Android 15)
- **UI Framework**: Native Android Views — XML layouts, `ViewBinding`, Material Components for Android 1.14.0 (Material 3 XML themes/widgets)
- **Navigation**: One `AppCompatActivity` per feature area hosting `Fragment`s via `FragmentManager` (no Jetpack Navigation Compose / no shared `NavHost`)
- **Database**: Room Persistence Library (v2.8.4)
- **Backend Services**: Firebase Authentication (BoM v34.18.0)
- **Image Handling**: Custom `ImageUtils` using Android `ContentResolver` and internal file storage for persistence
- **SMS Implementation**: Standard Android `Intent.ACTION_SENDTO` for secure external delegation
- **Concurrency**: Kotlin Coroutines and `Flow` for reactive data streams
- **Dependency Management**: Gradle Version Catalog (`libs.versions.toml`), AGP 8.10.0

## 6. Application Architecture
The application follows the **MVVM (Model-View-ViewModel)** architectural pattern combined with the **Repository Pattern**:

- **UI Layer (Fragments/Activities + XML)**: Inflates layouts via `ViewBinding`, populates `RecyclerView`s through `ListAdapter`s, and observes `StateFlow`/`Flow` from ViewModels inside `repeatOnLifecycle(STARTED)` blocks.
- **ViewModel Layer**: All ViewModels live in `ui/viewmodel/PetCareViewModels.kt` (`PetViewModel`, `CareRoutineViewModel`, `CareTaskViewModel`, `HealthcareRecordViewModel`, `ExpenseViewModel`, `DelegationViewModel`, `NotificationSettingsViewModel`). Each manages UI state, validates user input, and talks to the Repository — this layer was untouched by the Compose→XML migration.
- **Repository Layer** (`PetCareRepository`): The single source of truth, orchestrating data between local Room storage and Firebase Authentication's current-user id.
- **Data Layer (Room/Firebase)**: DAOs and entities under `data/local/`, plus Firebase Auth for identity.

This architecture keeps the app maintainable and testable, and let the UI layer be swapped from Compose to Views without touching business logic.

## 7. Navigation Structure
There is no single shared nav graph — each feature area is its own launchable `Activity` (all `exported="false"` except the launcher), reached via explicit `Intent`s:

```
MainActivity (launcher, exported)
  ├─ signed out            → AuthActivity (Login / Register / Forgot Password fragments)
  ├─ reminder deep link     → TasksActivity (petcare://tasks/{id}) or HealthcareActivity (petcare://healthcare/{id})
  └─ default                → HomeActivity
        └─ HomeFragment (bottom nav: Home / Pets / Care / Health / Profile)
              ├─ nav_pets   → PetsActivity      (MyPetsFragment → PetDetailsFragment → PetFormFragment,
              │                                   MyRoutinesFragment → RoutineDetailsFragment → RoutineFormFragment)
              ├─ nav_care   → TasksActivity     (DailyChecklistFragment / WeeklyChecklistFragment → TaskFormFragment)
              ├─ nav_health → HealthcareActivity (HealthcareListFragment → HealthcareDetailsFragment → HealthcareFormFragment)
              ├─ nav_profile→ ProfileActivity   (ProfileFragment)
              ├─ 🔔 bell    → NotificationSettingsActivity
              └─ (from Pet Details / Task rows) → ExpensesActivity, DelegationActivity, RoutinesActivity
```
Screen-to-screen data (e.g. a specific pet, task, or record) is passed via typed `Intent` extras such as `EXTRA_PET_ID`, `EXTRA_TASK_ID`, and `EXTRA_RECORD_ID` declared as companion constants on each target Activity.

## 8. Data Storage
PetCare uses a relational local database via Room:

- **Technology**: SQLite / Room Database (`PetCareDatabase`).
- **Entities** (`data/local/entity/`): `PetEntity`, `CareRoutineEntity`, `CareTaskEntity`, `TaskCompletionEntity`, `HealthcareRecordEntity`, `ExpenseEntity`, `BudgetEntity`, `DelegationEntity`, `NotificationSettingsEntity`.
- **DAOs** (`data/local/dao/`): one per entity (`PetDao`, `CareRoutineDao`, `CareTaskDao`, `TaskCompletionDao`, `HealthcareRecordDao`, `ExpenseDao`, `BudgetDao`, `DelegationDao`, `NotificationSettingsDao`), each exposing `Flow`-based queries.
- **Relationships**: `data/local/relationship/PetWithRelations.kt` composes a pet with its related rows; Foreign Keys (e.g. `petId`) link tasks/expenses/healthcare records/routines to specific animals, with cascading deletes for dependent rows (e.g. deleting a task cascades its `TaskCompletionEntity` rows).
- **Data isolation**: Every user-owned record is tagged with a `userId` sourced from Firebase Auth's current user, so queries are always scoped to the signed-in account.

## 9. Main Screens
- **Login / Register / Forgot Password** (`AuthActivity`): Entry point for user authentication.
- **Home** (`HomeActivity`): Pet carousel, category chips, quick stats, notification shortcut, bottom navigation.
- **My Pets / Pet Details / Add-Edit Pet** (`PetsActivity`): Grid/list of pets, per-pet detail view, and the pet form.
- **My Routines / Routine Details / Add-Edit Routine** (`RoutinesActivity`): Recurring care-routine management.
- **Daily / Weekly Checklist / Task Form** (`TasksActivity`): Gesture-enabled task tracking (see §4).
- **Healthcare List / Details / Form** (`HealthcareActivity`): Medical log and reminder management.
- **Expenses List / Details / Form / Budget** (`ExpensesActivity`): Transaction tracking, categorization, and budgeting.
- **Delegation Form / History** (`DelegationActivity`): SMS instruction builder and history log.
- **Notification Settings** (`NotificationSettingsActivity`): Reminder preferences.
- **Profile** (`ProfileActivity`): Account details and sign-out.

## 10. Permissions
- `POST_NOTIFICATIONS`: Required for sending care task and healthcare reminders.
- `SEND_SMS`: Required to launch the SMS application for delegation.
- `READ_MEDIA_IMAGES` (API 33+) / `READ_EXTERNAL_STORAGE` (API ≤32): Required to select and upload pet photos from the gallery.
- `RECEIVE_BOOT_COMPLETED`: Required to reschedule alarms after a device reboot (`BootReceiver`).
- `SCHEDULE_EXACT_ALARM` (API ≤32) / `USE_EXACT_ALARM` (API 33+): Required for precise notification timing of medications and appointments.
- `INTERNET` / `ACCESS_NETWORK_STATE`: Required for Firebase Authentication.

## 11. SMS / Delegation
Delegation is implemented as a **safe Intent-based workflow**.
- **Process**: The user selects a pet and specific tasks; `SmsMessageGenerator` builds a structured text message summarizing the care plan.
- **Implementation**: The app uses `Intent(Intent.ACTION_SENDTO)` with a `smsto:` URI. This hands the formatted message to the user's preferred SMS app, letting them review and confirm before sending — the app itself never sends SMS directly, maximizing privacy and transparency.

## 12. Notifications
- `NotificationScheduler` schedules reminders via `AlarmManager` for care tasks and healthcare events.
- `NotificationReceiver` fires at the scheduled time and posts the notification (built with `NotificationHelper`); tapping it deep-links back into `MainActivity` via `petcare://tasks/{id}` or `petcare://healthcare/{id}`, which routes straight to the relevant `TasksActivity`/`HealthcareActivity` screen.
- `BootReceiver` reschedules all pending alarms after `RECEIVE_BOOT_COMPLETED`, since exact alarms don't survive a reboot.

## 13. Testing
The project includes a testing suite in `src/test` and `src/androidTest`:
- **Unit Tests** (`src/test`): `PetValidationTest.kt` (form validation), `DelegationMessageTest.kt` (`SmsMessageGenerator` output), `NotificationSchedulerTest.kt`, `ShakeDetectorTest.kt` (shake-detection math with debounce/cooldown).
- **Instrumentation Tests** (`src/androidTest`): `PetManagementAndroidTest.kt`, `ExpenseAndroidTest.kt`, `HealthcareAndroidTest.kt`, `DelegationAndroidTest.kt`, and `CareTaskGestureAndroidTest.kt` (swipe-right occurrence completion, swipe-left cascade deletion, shake-based reset scoped to today and the current user).
- **Firebase Compatibility**: Automated resource ids / test tags are used throughout the primary forms to support **Firebase Test Lab Robo Tests**.

## 14. Project Structure
```text
com.example.petcare/
├── data/local/
│   ├── entity/          # Room entities (Pet, CareRoutine, CareTask, TaskCompletion, HealthcareRecord, Expense, Budget, Delegation, NotificationSettings)
│   ├── dao/              # Room DAOs, one per entity
│   ├── relationship/     # PetWithRelations composite query model
│   └── repository/       # PetCareRepository - single source of truth
├── receivers/            # NotificationReceiver, BootReceiver
├── ui/
│   ├── screens/
│   │   ├── auth/          # AuthActivity + Login/Register/ForgotPassword fragments
│   │   ├── home/           # HomeActivity, HomeFragment, PetPagerAdapter
│   │   ├── pet/             # PetsActivity, RoutinesActivity + My Pets/Routines, Details, Form fragments, adapters
│   │   ├── checklist/       # TasksActivity + Daily/Weekly checklist, Task form fragments, TaskAdapter (+ swipe gestures)
│   │   ├── healthcare/      # HealthcareActivity + List/Details/Form fragments, HealthcareAdapter
│   │   ├── expenses/        # ExpensesActivity + List/Details/Form/Budget fragments, ExpenseAdapter
│   │   ├── delegation/      # DelegationActivity + Form/History fragments, DelegationAdapter
│   │   ├── settings/        # NotificationSettingsActivity + Fragment
│   │   └── profile/         # ProfileActivity + Fragment
│   ├── theme/             # XML Material 3 theme (values/themes.xml, values-night mirrors light)
│   └── viewmodel/          # PetCareViewModels.kt - all ViewModels
└── utils/                 # DateUtils, ImageUtils, NotificationHelper, NotificationScheduler, ShakeDetector, SmsMessageGenerator
```

## 15. Setup and Installation
1. **Clone**: Clone the repository to your local machine.
2. **Open**: Open the project folder in Android Studio (Ladybug/2024.2.1 or newer).
3. **Firebase**: Ensure a valid `google-services.json` file is placed in the `app/` directory.
4. **Sync**: Allow Gradle to sync and download all dependencies defined in `libs.versions.toml`.

## 16. Build and Run
1. **Clean**: `Build > Clean Project`.
2. **Build**: `Build > Rebuild Project`, or `./gradlew :app:assembleDebug` from the command line.
3. **Target**: Connect a physical Android device or start an emulator (API 24+).
4. **Run**: Click "Run 'app'" in Android Studio, or `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## 17. Known Issues
- **Photo Permissions**: On some Android versions, the app may require a restart after the first photo upload to correctly render the internal file path in all screens.
- **OEM font rendering**: On some MIUI devices, unstyled small `TextView`s can render with their glyph tops clipped due to an OEM system-font metrics mismatch; screens affected by this are given a small explicit top padding as a buffer (see Home's welcome label).

## 18. Future Improvements
- **Cloud Sync**: Moving Room data to Firebase Firestore for cross-device synchronization.
- **AI Integration**: Implementing image recognition to automatically detect pet breeds and suggest care tips.
- **Community Forum**: A social module for pet parents to share advice and local vet recommendations.

## 19. Assignment Requirements Mapping

| Requirement | Implementation in PetCare |
|---|---|
| User Registration & Login | `AuthActivity` + Login/Register/Forgot Password fragments, backed by Firebase Auth. |
| Home Screen | `HomeActivity`/`HomeFragment` dashboard with pet carousel and quick stats. |
| Pet Profiles (CRUD) | Full lifecycle management in `PetFormFragment` and `PetDetailsFragment` (`PetsActivity`). |
| Care Routines | `RoutinesActivity` with recurring frequencies (Daily/Weekly/Monthly). |
| Task Checklists | `TasksActivity` Daily/Weekly fragments with persistence across sessions. |
| Item Editing/Deletion | Unified edit forms and confirmation dialogs across every module. |
| SMS Delegation | `DelegationActivity`, via `Intent.ACTION_SENDTO` and `SmsMessageGenerator`. |
| Desirable Feature | **Gesture Controls**: swipe right to complete, swipe left to delete, shake to reset today's checklist. |
| Data Persistence | Local Room Database with relational integrity and per-user data isolation. |
| UI Design | Native XML layouts with Material Components (Material 3 theming). |
