package com.example.petcare.ui_tests

import android.content.Intent
import android.net.Uri
import android.widget.DatePicker
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.example.petcare.R
import com.example.petcare.ui.screens.auth.AuthActivity
import com.example.petcare.ui.screens.checklist.TasksActivity
import com.example.petcare.ui.screens.expenses.ExpensesActivity
import com.example.petcare.ui.screens.healthcare.HealthcareActivity
import com.example.petcare.ui.screens.pet.PetsActivity
import com.example.petcare.ui.screens.pet.RoutinesActivity
import com.example.petcare.ui_tests.UiTestSupport.TEST_EMAIL
import com.example.petcare.ui_tests.UiTestSupport.TEST_PASSWORD
import com.example.petcare.ui_tests.UiTestSupport.TEST_USERNAME
import com.example.petcare.ui_tests.UiTestSupport.addPetViaUi
import com.example.petcare.ui_tests.UiTestSupport.addTaskViaUi
import com.example.petcare.ui_tests.UiTestSupport.clickScrolled
import com.example.petcare.ui_tests.UiTestSupport.clickTaskCheckbox
import com.example.petcare.ui_tests.UiTestSupport.clickText
import com.example.petcare.ui_tests.UiTestSupport.createPetViaUi
import com.example.petcare.ui_tests.UiTestSupport.device
import com.example.petcare.ui_tests.UiTestSupport.ensureSignedInAsTestUser
import com.example.petcare.ui_tests.UiTestSupport.fillPetForm
import com.example.petcare.ui_tests.UiTestSupport.isDisplayedNow
import com.example.petcare.ui_tests.UiTestSupport.listItem
import com.example.petcare.ui_tests.UiTestSupport.openPetFromList
import com.example.petcare.ui_tests.UiTestSupport.pkg
import com.example.petcare.ui_tests.UiTestSupport.shot
import com.example.petcare.ui_tests.UiTestSupport.signOutForTestSetup
import com.example.petcare.ui_tests.UiTestSupport.swipeTaskRow
import com.example.petcare.ui_tests.UiTestSupport.taskRow
import com.example.petcare.ui_tests.UiTestSupport.textOf
import com.example.petcare.ui_tests.UiTestSupport.typeInto
import com.example.petcare.ui_tests.UiTestSupport.uniqueTag
import com.example.petcare.ui_tests.UiTestSupport.waitDisplayed
import com.example.petcare.ui_tests.UiTestSupport.waitItemDisplayed
import com.example.petcare.ui_tests.UiTestSupport.waitNotDisplayed
import com.example.petcare.ui_tests.UiTestSupport.waitUntil
import com.example.petcare.ui_tests.UiTestSupport.instrumentation
import com.example.petcare.ui_tests.UiTestSupport.context
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.Calendar

/**
 * Real UI automation for the 18 manual test cases (T01-T18).
 * Every action goes through Espresso / UI Automator on the real screens; no ViewModel,
 * repository, DAO or Room call is made anywhere in this class.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PetCareUiTests {

    @get:org.junit.Rule
    val failureEvidence = object : org.junit.rules.TestWatcher() {
        override fun failed(e: Throwable?, description: org.junit.runner.Description) {
            try { shot("_failures", description.methodName) } catch (_: Throwable) {}
        }
    }

    @Before
    fun setUp() {
        device.wakeUp()
        try { device.executeShellCommand("wm dismiss-keyguard") } catch (_: Throwable) {}
        try {
            instrumentation.uiAutomation.grantRuntimePermission(pkg, "android.permission.POST_NOTIFICATIONS")
        } catch (_: Throwable) {}
    }

    // ------------------------------------------------------------------ T01
    @Test
    fun t01_registerNewAccount() {
        val folder = "T01_Register"
        signOutForTestSetup()
        val email = "ui.test.${uniqueTag()}@example.com"

        ActivityScenario.launch(AuthActivity::class.java).use {
            waitDisplayed(withId(R.id.login))
            clickScrolled(R.id.create_account)
            waitDisplayed(withId(R.id.register))

            typeInto(R.id.username, TEST_USERNAME)
            typeInto(R.id.email, email)
            typeInto(R.id.password, TEST_PASSWORD)
            typeInto(R.id.confirm_password, TEST_PASSWORD)
            shot(folder, "01_register_screen")

            clickScrolled(R.id.register)
            waitDisplayed(withId(R.id.home_greeting), 40_000)
            shot(folder, "02_registration_success")

            onView(withId(R.id.home_greeting)).check(matches(isDisplayed()))
            onView(withId(R.id.home_avatar)).check(matches(isDisplayed()))
        }
    }

    // ------------------------------------------------------------------ T02
    @Test
    fun t02_loginWithValidDetails() {
        val folder = "T02_Valid_Login"
        ensureSignedInAsTestUser() // provisions the fixed account through the UI if it is missing
        signOutForTestSetup()

        ActivityScenario.launch(AuthActivity::class.java).use {
            waitDisplayed(withId(R.id.login))
            typeInto(R.id.email, TEST_EMAIL)
            typeInto(R.id.password, TEST_PASSWORD)
            shot(folder, "01_login_screen")

            onView(withId(R.id.login)).perform(click())
            waitDisplayed(withId(R.id.home_greeting), 40_000)
            shot(folder, "02_login_success")

            onView(withId(R.id.home_greeting)).check(matches(isDisplayed()))
        }
    }

    // ------------------------------------------------------------------ T03
    @Test
    fun t03_loginWithIncorrectDetails() {
        val folder = "T03_Invalid_Login"
        signOutForTestSetup()

        ActivityScenario.launch(AuthActivity::class.java).use {
            waitDisplayed(withId(R.id.login))
            typeInto(R.id.email, "no.such.user.${uniqueTag()}@example.com")
            typeInto(R.id.password, "WrongPassword999")
            shot(folder, "01_invalid_login")

            onView(withId(R.id.login)).perform(click())
            waitDisplayed(withId(R.id.error_message), 40_000)
            val error = textOf(withId(R.id.error_message))
            shot(folder, "02_login_error")

            assertTrue("An error message should be shown, was '$error'", error.isNotBlank())
            assertFalse("Home must not be displayed after a rejected login", isDisplayedNow(withId(R.id.home_greeting)))
            onView(withId(R.id.login)).check(matches(isDisplayed()))
        }
    }

    // ------------------------------------------------------------------ T04
    @Test
    fun t04_createPetProfile() {
        val folder = "T04_Create_Pet"
        ensureSignedInAsTestUser()
        val name = "Test Pet ${uniqueTag()}"

        ActivityScenario.launch(PetsActivity::class.java).use {
            waitDisplayed(withId(R.id.btn_add_pet))
            clickScrolled(R.id.btn_add_pet)
            fillPetForm(name, species = "Dog", breed = "Test Breed", weight = "12.5", gender = "Male")
            shot(folder, "01_add_pet_form")

            clickScrolled(R.id.btn_save_pet)
            waitDisplayed(withId(R.id.btn_add_pet))
            val item = listItem(
                R.id.rv_pets, withText(name), withText("Dog • Test Breed"), withText("12.5 kg")
            )
            waitItemDisplayed(R.id.rv_pets, item)
            shot(folder, "02_pet_created_in_my_pets")

            onView(item).check(matches(isDisplayed()))
        }
    }

    // ------------------------------------------------------------------ T05
    @Test
    fun t05_editPetInformation() {
        val folder = "T05_Edit_Pet"
        ensureSignedInAsTestUser()
        val name = "Test Pet ${uniqueTag()}"
        val newName = "$name Edited"

        ActivityScenario.launch(PetsActivity::class.java).use {
            waitDisplayed(withId(R.id.btn_add_pet))
            addPetViaUi(name)
            openPetFromList(name)

            clickScrolled(R.id.btn_edit_pet)
            waitUntil("edit form pre-filled with existing pet") {
                onView(withId(R.id.et_pet_name)).check(matches(withText(name)))
            }
            shot(folder, "01_edit_pet_screen")

            typeInto(R.id.et_pet_name, newName)
            typeInto(R.id.et_breed, "Edited Breed")
            typeInto(R.id.et_weight, "20.5")
            clickScrolled(R.id.btn_save_pet)

            waitUntil("pet details show updated info") {
                onView(withId(R.id.pet_details_title)).check(matches(withText(newName)))
                onView(withId(R.id.pet_details_weight)).check(matches(withText("20.5 kg")))
                onView(withId(R.id.pet_details_species_breed)).check(matches(withText("Dog • Edited Breed")))
            }
            shot(folder, "02_updated_pet_information")
        }
    }

    // ------------------------------------------------------------------ T06
    @Test
    fun t06_deletePet() {
        val folder = "T06_Delete_Pet"
        ensureSignedInAsTestUser()
        val name = "Test Pet ${uniqueTag()}"

        ActivityScenario.launch(PetsActivity::class.java).use {
            waitDisplayed(withId(R.id.btn_add_pet))
            addPetViaUi(name)
            shot(folder, "01_pet_before_deletion")

            openPetFromList(name)
            clickScrolled(R.id.btn_delete_pet)
            waitDisplayed(withText(R.string.delete_pet_dialog_title))
            shot(folder, "02_delete_confirmation")

            onView(withText(R.string.action_delete)).inRoot(isDialog()).perform(click())
            waitDisplayed(withId(R.id.btn_add_pet))
            waitNotDisplayed(withText(name))
            shot(folder, "03_my_pets_after_deletion")

            assertFalse("Deleted pet must be gone from My Pets", isDisplayedNow(withText(name)))
        }
    }

    // ------------------------------------------------------------------ T07
    @Test
    fun t07_createCareRoutine() {
        val folder = "T07_Create_Care_Routine"
        ensureSignedInAsTestUser()
        val tag = uniqueTag()
        val petName = "Test Pet $tag"
        val routine = "Test Routine $tag"
        createPetViaUi(petName)

        ActivityScenario.launch(RoutinesActivity::class.java).use {
            waitDisplayed(withId(R.id.btn_add_routine))
            clickScrolled(R.id.btn_add_routine)
            waitDisplayed(withId(R.id.et_routine_name))
            clickText(petName, withId(R.id.sp_pet))
            typeInto(R.id.et_routine_name, routine)
            clickScrolled(R.id.chip_freq_weekly)
            shot(folder, "01_create_routine_screen")

            clickScrolled(R.id.btn_save_routine)
            val item = listItem(R.id.rv_routines, withText(routine), withText("WEEKLY"), withText(petName))
            waitItemDisplayed(R.id.rv_routines, item)
            shot(folder, "02_saved_routine_with_frequency")

            onView(item).check(matches(isDisplayed()))
        }
    }

    // ------------------------------------------------------------------ T08
    @Test
    fun t08_markTaskAsCompleted() {
        val folder = "T08_Mark_Task_Completed"
        ensureSignedInAsTestUser()
        val tag = uniqueTag()
        val petName = "Test Pet $tag"
        val title = "Test Task $tag"
        createPetViaUi(petName)

        ActivityScenario.launch(TasksActivity::class.java).use {
            waitDisplayed(withId(R.id.fab_add_task))
            addTaskViaUi(petName, title)
            waitItemDisplayed(R.id.rv_care_tasks, taskRow(R.id.rv_care_tasks, title, checked = false))
            shot(folder, "01_task_before_completion")

            clickTaskCheckbox(R.id.rv_care_tasks, title)
            waitItemDisplayed(R.id.rv_care_tasks, taskRow(R.id.rv_care_tasks, title, checked = true))
            shot(folder, "02_task_completed")

            onView(taskRow(R.id.rv_care_tasks, title, checked = true)).check(matches(isDisplayed()))
        }
    }

    // ------------------------------------------------------------------ T09
    @Test
    fun t09_swipeRightOnTask() {
        val folder = "T09_Swipe_Right"
        ensureSignedInAsTestUser()
        val tag = uniqueTag()
        val petName = "Test Pet $tag"
        val title = "Test Swipe Task $tag"
        createPetViaUi(petName)

        ActivityScenario.launch(TasksActivity::class.java).use {
            waitDisplayed(withId(R.id.fab_add_task))
            addTaskViaUi(petName, title)
            waitItemDisplayed(R.id.rv_care_tasks, taskRow(R.id.rv_care_tasks, title, checked = false))
            shot(folder, "01_task_before_swipe")

            swipeTaskRow(title, toRight = true)
            waitItemDisplayed(R.id.rv_care_tasks, taskRow(R.id.rv_care_tasks, title, checked = true))
            shot(folder, "02_task_after_swipe_right")

            onView(taskRow(R.id.rv_care_tasks, title, checked = true)).check(matches(isDisplayed()))
        }
    }

    // ------------------------------------------------------------------ T10
    @Test
    fun t10_swipeLeftOnTask() {
        val folder = "T10_Swipe_Left"
        ensureSignedInAsTestUser()
        val tag = uniqueTag()
        val petName = "Test Pet $tag"
        val title = "Test Delete Task $tag"
        createPetViaUi(petName)

        ActivityScenario.launch(TasksActivity::class.java).use {
            waitDisplayed(withId(R.id.fab_add_task))
            addTaskViaUi(petName, title)
            shot(folder, "01_task_before_swipe")

            swipeTaskRow(title, toRight = false)
            waitDisplayed(withText(R.string.delete_task_dialog_title))
            shot(folder, "02_delete_confirmation_dialog")

            onView(withText(R.string.delete_task_dialog_title)).check(matches(isDisplayed()))
            onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click())
            // Cancelling must leave the task in place.
            waitItemDisplayed(R.id.rv_care_tasks, taskRow(R.id.rv_care_tasks, title))
        }
    }

    // ------------------------------------------------------------------ T11
    @Test
    fun t11_shakeDevice() {
        val folder = "T11_Shake_Device"
        ensureSignedInAsTestUser()

        ActivityScenario.launch(TasksActivity::class.java).use {
            waitDisplayed(withId(R.id.fab_add_task))
            shot(folder, "01_checklist_before_shake")
        }
        // The app listens to the real accelerometer (DailyChecklistFragment -> ShakeEventListener,
        // threshold 2.2 g). A physical phone cannot be shaken over ADB and sensor injection is not
        // available without root, so this can only be verified by physically shaking the device.
        assumeTrue(
            "BLOCKED/MANUAL: physical accelerometer shake cannot be automated on the connected " +
                "physical device (no sensor injection without root). Not marked as passed.",
            false
        )
    }

    // ------------------------------------------------------------------ T12
    @Test
    fun t12_delegateCareInstructions() {
        val folder = "T12_Delegate_Care"
        ensureSignedInAsTestUser()
        val tag = uniqueTag()
        val petName = "Test Pet $tag"
        val title = "Test Delegated Task $tag"
        val phone = "5551234567"
        createPetViaUi(petName)
        ActivityScenario.launch(TasksActivity::class.java).use {
            waitDisplayed(withId(R.id.fab_add_task))
            addTaskViaUi(petName, title)
        }

        var previewMessage = ""
        ActivityScenario.launch(PetsActivity::class.java).use {
            waitDisplayed(withId(R.id.btn_add_pet))
            openPetFromList(petName)
            clickScrolled(R.id.btn_delegate_care)

            waitDisplayed(withId(R.id.et_delegation_recipient_name))
            waitDisplayed(allOf(withId(R.id.delegation_task_title), withText(title)))
            onView(allOf(withId(R.id.delegation_task_title), withText(title))).perform(click())
            typeInto(R.id.et_delegation_recipient_name, "Test Sitter")
            typeInto(R.id.et_delegation_phone, phone)
            waitUntil("message preview contains the selected task") {
                assertTrue(textOf(withId(R.id.tv_delegation_message)).contains(title))
            }
            previewMessage = textOf(withId(R.id.tv_delegation_message))
            shot(folder, "01_delegation_screen")

            clickScrolled(R.id.btn_send_sms)

            // ---- external SMS application (never sent) ----
            val smsPackage = waitForExternalPackage()
            val expectedDefault = resolveSmsPackage(phone)
            shot(folder, "02_sms_app_prepared_message")

            if (expectedDefault != null && expectedDefault != "android") {
                assertEquals("Opened app should be the device's SMS app", expectedDefault, smsPackage)
            }
            val texts = device.findObjects(By.pkg(smsPackage))
                .flatMap { listOfNotNull(it.text, it.contentDescription) }
            assertTrue(
                "Recipient $phone not found in SMS app. Visible: $texts",
                texts.any { it.filter(Char::isDigit).contains(phone) }
            )
            assertTrue(
                "Prepared message (contains '$title') not found in SMS app. Visible: $texts",
                texts.any { it.contains(title) }
            )
            assertTrue("Preview should have been non-empty", previewMessage.isNotBlank())

            device.pressBack() // leave the SMS app without sending
        }
    }

    // ------------------------------------------------------------------ T13
    @Test
    fun t13_addHealthcareRecord() {
        val folder = "T13_Healthcare_Record"
        ensureSignedInAsTestUser()
        val tag = uniqueTag()
        val petName = "Test Pet $tag"
        val title = "Test Checkup $tag"
        createPetViaUi(petName)

        ActivityScenario.launch(PetsActivity::class.java).use {
            waitDisplayed(withId(R.id.btn_add_pet))
            openPetFromList(petName)
            clickScrolled(R.id.btn_add_healthcare)

            waitDisplayed(withId(R.id.et_healthcare_title))
            waitDisplayed(withText(containsString(petName)))
            typeInto(R.id.et_healthcare_title, title)
            typeInto(R.id.et_healthcare_clinic, "Test Clinic")
            typeInto(R.id.et_healthcare_notes, "UI automation record")
            shot(folder, "01_add_healthcare_screen")

            clickScrolled(R.id.btn_save_healthcare)
            waitDisplayed(withId(R.id.pet_details_title))
            clickScrolled(R.id.healthcare_card)

            val item = listItem(R.id.rv_healthcare, withText(title), withText("Pet: $petName"))
            waitItemDisplayed(R.id.rv_healthcare, item)
            shot(folder, "02_saved_healthcare_record")

            onView(item).check(matches(isDisplayed()))
        }
    }

    // ------------------------------------------------------------------ T14
    @Test
    fun t14_addExpense() {
        val folder = "T14_Add_Expense"
        ensureSignedInAsTestUser()
        val title = "Test Expense ${uniqueTag()}"

        ActivityScenario.launch(ExpensesActivity::class.java).use {
            waitDisplayed(withId(R.id.btn_add_expense))
            clickScrolled(R.id.btn_add_expense)
            waitDisplayed(withId(R.id.et_expense_title))
            typeInto(R.id.et_expense_title, title)
            typeInto(R.id.et_expense_amount, "250.75")
            clickText("Grooming", withId(R.id.sp_expense_category))
            shot(folder, "01_add_expense_screen")

            clickScrolled(R.id.btn_save_expense)
            val item = listItem(
                R.id.rv_expenses, withText(title), withText(containsString("250.75")), withText(containsString("Grooming"))
            )
            waitItemDisplayed(R.id.rv_expenses, item)
            shot(folder, "02_saved_expense")

            onView(item).check(matches(isDisplayed()))
        }
    }

    // ------------------------------------------------------------------ T15
    @Test
    fun t15_setMonthlyBudget() {
        val folder = "T15_Monthly_Budget"
        ensureSignedInAsTestUser()
        val expenseTitle = "Test Budget Expense ${uniqueTag()}"

        ActivityScenario.launch(ExpensesActivity::class.java).use {
            waitDisplayed(withId(R.id.btn_add_expense))
            // A listed expense guarantees the "Edit budget" button is reachable afterwards.
            clickScrolled(R.id.btn_add_expense)
            waitDisplayed(withId(R.id.et_expense_title))
            typeInto(R.id.et_expense_title, expenseTitle)
            typeInto(R.id.et_expense_amount, "100")
            clickScrolled(R.id.btn_save_expense)
            waitItemDisplayed(R.id.rv_expenses, listItem(R.id.rv_expenses, withText(expenseTitle)))

            waitUntil("budget entry point") {
                if (!isDisplayedNow(withId(R.id.btn_add_budget)) && !isDisplayedNow(withId(R.id.btn_edit_budget))) {
                    UiTestSupportScroll.scrollToBudgetControls()
                    if (!isDisplayedNow(withId(R.id.btn_add_budget)) && !isDisplayedNow(withId(R.id.btn_edit_budget))) {
                        throw AssertionError("no budget button visible")
                    }
                }
            }
            if (isDisplayedNow(withId(R.id.btn_add_budget))) clickScrolled(R.id.btn_add_budget) else clickScrolled(R.id.btn_edit_budget)

            waitDisplayed(withId(R.id.et_budget_amount))
            typeInto(R.id.et_budget_amount, "7500")
            shot(folder, "01_budget_screen")

            clickScrolled(R.id.btn_save_budget)
            UiTestSupport.waitVisible(withId(R.id.budget_progress_section))
            UiTestSupportScroll.scrollToBudgetControls()
            waitUntil("budget percent shown") {
                onView(withId(R.id.tv_budget_percent)).check(matches(withText(containsString("% of budget"))))
            }
            shot(folder, "02_saved_budget_summary")

            // Re-open the budget form and confirm the stored amount.
            clickScrolled(R.id.btn_edit_budget)
            waitUntil("stored budget amount shown in form") {
                onView(withId(R.id.et_budget_amount)).check(matches(withText("7500.0")))
            }
        }
    }

    // ------------------------------------------------------------------ T16
    @Test
    fun t16_reopenSavedInformation() {
        val folder = "T16_Reopen_Saved_Information"
        ensureSignedInAsTestUser()
        val title = "Test Persistent Expense ${uniqueTag()}"
        val item = listItem(R.id.rv_expenses, withText(title), withText(containsString("321.50")))

        ActivityScenario.launch(ExpensesActivity::class.java).use {
            waitDisplayed(withId(R.id.btn_add_expense))
            clickScrolled(R.id.btn_add_expense)
            waitDisplayed(withId(R.id.et_expense_title))
            typeInto(R.id.et_expense_title, title)
            typeInto(R.id.et_expense_amount, "321.50")
            clickScrolled(R.id.btn_save_expense)
            waitItemDisplayed(R.id.rv_expenses, item)
            shot(folder, "01_saved_information_before_leaving")
        }

        // Screen is closed above; open it again from scratch.
        ActivityScenario.launch(ExpensesActivity::class.java).use {
            waitItemDisplayed(R.id.rv_expenses, item)
            shot(folder, "02_reopened_screen_same_information")
            onView(item).check(matches(isDisplayed()))
        }
    }

    // ------------------------------------------------------------------ T17
    @Test
    fun t17_completeRecurringTaskForOneDate() {
        val folder = "T17_Recurring_Task"
        ensureSignedInAsTestUser()
        val tag = uniqueTag()
        val petName = "Test Pet $tag"
        val title = "Test Daily Task $tag"
        createPetViaUi(petName)

        // Saturday: tomorrow falls outside the Sun-Sat week view, so use yesterday and start the
        // recurring task in the past via the real date picker.
        val today = Calendar.getInstance()
        val useYesterday = today.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
        val otherDay = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, if (useYesterday) -1 else 1) }

        ActivityScenario.launch(TasksActivity::class.java).use {
            waitDisplayed(withId(R.id.fab_add_task))
            clickScrolled(R.id.fab_add_task)
            waitDisplayed(withId(R.id.et_task_title))
            clickText(petName, withId(R.id.sp_task_pet))
            typeInto(R.id.et_task_title, title)
            clickScrolled(R.id.chip_task_freq_daily)
            if (useYesterday) {
                val start = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -3) }
                clickScrolled(R.id.btn_task_date)
                onView(withClassName(equalTo(DatePicker::class.java.name)))
                    .perform(PickerActions.setDate(start.get(Calendar.YEAR), start.get(Calendar.MONTH) + 1, start.get(Calendar.DAY_OF_MONTH)))
                onView(withId(android.R.id.button1)).perform(click())
            }
            clickScrolled(R.id.btn_save_task)
            waitDisplayed(withId(R.id.fab_add_task))
            waitItemDisplayed(R.id.rv_care_tasks, taskRow(R.id.rv_care_tasks, title))
            shot(folder, "01_recurring_task_before_completion")
            waitItemDisplayed(R.id.rv_care_tasks, taskRow(R.id.rv_care_tasks, title, checked = false))

            clickTaskCheckbox(R.id.rv_care_tasks, title)
            waitItemDisplayed(R.id.rv_care_tasks, taskRow(R.id.rv_care_tasks, title, checked = true))
        }

        // Weekly view (only reachable through TasksActivity.EXTRA_SHOW_WEEKLY): today vs. another date.
        val weekly = Intent(context, TasksActivity::class.java).putExtra(TasksActivity.EXTRA_SHOW_WEEKLY, true)
        ActivityScenario.launch<TasksActivity>(weekly).use {
            waitDisplayed(withId(R.id.rv_weekly_tasks))
            waitItemDisplayed(R.id.rv_weekly_tasks, taskRow(R.id.rv_weekly_tasks, title, checked = true))
            shot(folder, "02_selected_occurrence_completed")

            // The app's week strip can start after today (see report), so use tomorrow when shown,
            // otherwise the first date the strip offers (always a later date, where DAILY is active).
            val shownDays = device.findObjects(By.res(pkg, "weekly_day_number")).map { it.text }
            val wanted = otherDay.get(Calendar.DAY_OF_MONTH).toString()
            val dayNumber = if (wanted in shownDays) wanted else shownDays.first()
            onView(allOf(withId(R.id.weekly_day_number), withText(dayNumber))).perform(click())
            waitItemDisplayed(R.id.rv_weekly_tasks, taskRow(R.id.rv_weekly_tasks, title, checked = false))
            shot(folder, "03_other_occurrence_unchanged")

            onView(taskRow(R.id.rv_weekly_tasks, title, checked = false)).check(matches(isDisplayed()))
        }
    }

    // ------------------------------------------------------------------ T18
    @Test
    fun t18_externalVeterinaryDataImport() {
        val folder = "T18_External_Veterinary_Data_Import"
        ensureSignedInAsTestUser()
        // The app's vet API (mockapi.io/pet) returns a record for the pet "Buddy" (Dog); the app
        // imports only for pets whose name and species match.
        val petName = "Buddy"

        ActivityScenario.launch(PetsActivity::class.java).use {
            waitDisplayed(withId(R.id.btn_add_pet))
            waitUntil("pets list loaded") {
                if (!isDisplayedNow(withId(R.id.rv_pets)) && !isDisplayedNow(withId(R.id.empty_pets))) {
                    throw AssertionError("list not loaded")
                }
            }
            if (!isDisplayedNow(allOf(withId(R.id.item_pet_name), withText(petName)))) {
                addPetViaUi(petName, species = "Dog", breed = "Golden Retriever", weight = "25", gender = "Male")
            }
            openPetFromList(petName)
            clickScrolled(R.id.healthcare_card)

            waitDisplayed(withId(R.id.btn_import_vet_data))
            shot(folder, "01_import_screen")

            onView(withId(R.id.btn_import_vet_data)).perform(click())
            device.wait(Until.hasObject(By.textContains("Imported")), 25_000)
            shot(folder, "02_import_result")

            val rabies = listItem(R.id.rv_healthcare, withText("Rabies"), withText("Pet: $petName"))
            val appointment = listItem(R.id.rv_healthcare, withText("Veterinary Appointment"), withText("Pet: $petName"))
            waitItemDisplayed(R.id.rv_healthcare, rabies, 30_000)
            waitItemDisplayed(R.id.rv_healthcare, appointment, 10_000)
            shot(folder, "03_imported_healthcare_records")

            onView(rabies).perform(click())
            waitDisplayed(withId(R.id.tv_healthcare_title))
            onView(withId(R.id.tv_healthcare_title)).check(matches(withText("Rabies")))
            onView(allOf(withId(R.id.value), isDescendantOfA(withId(R.id.row_healthcare_clinic))))
                .check(matches(withText("Happy Paws Veterinary Clinic")))
            onView(withId(R.id.tv_healthcare_notes)).check(matches(withText(containsString("Imported from Veterinary API"))))
            shot(folder, "04_imported_record_details")
        }
    }

    // ------------------------------------------------------------ SMS helpers

    private fun waitForExternalPackage(): String {
        device.wait(Until.hasObject(By.pkg(pkg).depth(0)), 1) // no-op sync point
        waitUntil("an external app (SMS) to be in the foreground", 20_000) {
            val current = device.currentPackageName
            if (current == null || current == pkg || current == "com.android.systemui") {
                throw AssertionError("foreground is still $current")
            }
        }
        device.waitForIdle()
        return device.currentPackageName
    }

    private fun resolveSmsPackage(phone: String): String? {
        val probe = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))
        return context.packageManager
            .resolveActivity(probe, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
    }

    /** Budget controls sit inside the list screen's scroll view; bring them on screen. */
    private object UiTestSupportScroll {
        fun scrollToBudgetControls() {
            try {
                onView(withId(R.id.budget_progress_section)).perform(androidx.test.espresso.action.ViewActions.scrollTo())
            } catch (_: Throwable) {
            }
            try {
                onView(withId(R.id.btn_add_budget)).perform(androidx.test.espresso.action.ViewActions.scrollTo())
            } catch (_: Throwable) {
            }
        }
    }
}
