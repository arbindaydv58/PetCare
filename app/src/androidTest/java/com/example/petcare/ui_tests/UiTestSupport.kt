package com.example.petcare.ui_tests

import android.app.Instrumentation
import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.example.petcare.R
import com.example.petcare.ui.screens.auth.AuthActivity
import com.example.petcare.ui.screens.pet.PetsActivity
import com.example.petcare.utils.TestScreenshotHelper
import com.google.firebase.auth.FirebaseAuth
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher

/** Shared real-UI helpers. Nothing here touches ViewModels, repositories, DAOs or Room. */
object UiTestSupport {

    const val TEST_EMAIL = "petcare.ui.tester@example.com"
    const val TEST_PASSWORD = "TestPass123"
    const val TEST_USERNAME = "Test User"

    private const val DEFAULT_TIMEOUT_MS = 15_000L
    private const val NETWORK_TIMEOUT_MS = 40_000L

    val instrumentation: Instrumentation get() = InstrumentationRegistry.getInstrumentation()
    val device: UiDevice get() = UiDevice.getInstance(instrumentation)
    val context: Context get() = instrumentation.targetContext
    val pkg: String get() = context.packageName

    fun uniqueTag(): String = (System.currentTimeMillis() % 100_000_000L).toString()

    // ---------------------------------------------------------------- evidence

    fun shot(folder: String, name: String) {
        device.waitForIdle()
        TestScreenshotHelper.captureScreenshot(folder, name)
    }

    // ------------------------------------------------------------ synchronization

    fun waitUntil(description: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS, check: () -> Unit) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var last: Throwable? = null
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                check()
                return
            } catch (t: Throwable) {
                last = t
                SystemClock.sleep(150)
            }
        }
        try {
            val stamp = SystemClock.uptimeMillis()
            shot("_failures", "timeout_$stamp")
            val dir = java.io.File(context.getExternalFilesDir("test-results"), "_failures").apply { mkdirs() }
            device.dumpWindowHierarchy(java.io.File(dir, "timeout_$stamp.xml"))
        } catch (_: Throwable) {}
        throw AssertionError("Timed out after ${timeoutMs}ms waiting for: $description", last)
    }

    fun waitDisplayed(matcher: Matcher<View>, timeoutMs: Long = DEFAULT_TIMEOUT_MS) =
        waitUntil("view displayed: $matcher", timeoutMs) {
            onView(matcher).check(matches(isDisplayed()))
        }

    /** In the view tree and VISIBLE, but possibly still off-screen inside a scroll container. */
    fun waitVisible(matcher: Matcher<View>, timeoutMs: Long = DEFAULT_TIMEOUT_MS) =
        waitUntil("view visible: $matcher", timeoutMs) {
            onView(matcher).check(matches(androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility(
                androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE)))
        }

    fun waitNotDisplayed(matcher: Matcher<View>, timeoutMs: Long = DEFAULT_TIMEOUT_MS) =
        waitUntil("view NOT displayed: $matcher", timeoutMs) {
            if (isDisplayedNow(matcher)) throw AssertionError("still displayed: $matcher")
        }

    fun isDisplayedNow(matcher: Matcher<View>): Boolean = try {
        onView(matcher).check(matches(isDisplayed()))
        true
    } catch (t: Throwable) {
        false
    }

    // -------------------------------------------------------------------- input

    private fun scrollIfPossible(matcher: Matcher<View>) {
        try {
            onView(matcher).perform(scrollTo())
        } catch (t: Throwable) {
            // not inside a scroll container, or already fully visible
        }
    }

    /** Click first (fields only push text to their ViewModel while focused), then type. */
    fun typeInto(@IdRes id: Int, text: String) {
        val matcher = withId(id)
        waitVisible(matcher)
        scrollIfPossible(matcher)
        onView(matcher).perform(click(), replaceText(text), closeSoftKeyboard())
    }

    fun clickScrolled(@IdRes id: Int) {
        val matcher = withId(id)
        waitVisible(matcher)
        scrollIfPossible(matcher)
        onView(matcher).perform(click())
    }

    fun clickText(text: String, within: Matcher<View>? = null) {
        val matcher = if (within == null) {
            allOf(withText(text), isDisplayed())
        } else {
            allOf(withText(text), isDescendantOfA(within))
        }
        waitVisible(matcher)
        scrollIfPossible(matcher)
        onView(matcher).perform(click())
    }

    fun selectDropdown(@IdRes id: Int, item: String) {
        val field = withId(id)
        waitVisible(field)
        scrollIfPossible(field)
        onView(field).perform(click())
        waitUntil("dropdown item '$item'", 6_000) {
            onView(withText(item)).inRoot(isPlatformPopup()).check(matches(isDisplayed()))
        }
        onView(withText(item)).inRoot(isPlatformPopup()).perform(click())
    }

    fun textOf(matcher: Matcher<View>): String {
        var text = ""
        onView(matcher).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(TextView::class.java)
            override fun getDescription() = "read text"
            override fun perform(uiController: UiController, view: View) {
                text = (view as TextView).text.toString()
            }
        })
        return text
    }

    // -------------------------------------------------------------- list helpers

    /** Item view (direct child of the RecyclerView) containing every given child matcher. */
    class ListItemMatcher(rvId: Int, children: List<Matcher<View>>) : org.hamcrest.TypeSafeMatcher<View>() {
        private val descendants = children.map { hasDescendant(it) }
        val withoutParent: Matcher<View> = allOf(descendants.map { it as Matcher<in View> })
        private val full: Matcher<View> = allOf(
            ArrayList<Matcher<in View>>().apply {
                add(withParent(withId(rvId)))
                addAll(descendants)
            }
        )
        override fun matchesSafely(item: View): Boolean = full.matches(item)
        override fun describeTo(description: org.hamcrest.Description) = full.describeTo(description)
    }

    fun listItem(@IdRes rvId: Int, vararg children: Matcher<View>): Matcher<View> =
        ListItemMatcher(rvId, children.toList())

    fun scrollToItem(@IdRes rvId: Int, item: Matcher<View>) {
        try {
            // RecyclerViewActions tests detached holder views, so the parent constraint must be dropped.
            val scrollMatcher = if (item is ListItemMatcher) item.withoutParent else item
            onView(withId(rvId)).perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(scrollMatcher))
        } catch (t: Throwable) {
            // item may already be laid out inside an outer scroll container
        }
        scrollIfPossible(item)
    }

    fun waitItemDisplayed(@IdRes rvId: Int, item: Matcher<View>, timeoutMs: Long = DEFAULT_TIMEOUT_MS) =
        waitUntil("list item $item in rv", timeoutMs) {
            scrollToItem(rvId, item)
            onView(item).check(matches(isDisplayed()))
        }

    // ----------------------------------------------------------- UiAutomator swipe

    /** Real finger swipe across a task row, avoiding the system back-gesture edge zones. */
    fun swipeTaskRow(title: String, toRight: Boolean) {
        val titleView = device.wait(
            Until.findObject(By.res(pkg, "task_item_title").text(title)), DEFAULT_TIMEOUT_MS
        ) ?: throw AssertionError("Task row '$title' not found for swipe")
        val y = titleView.visibleBounds.centerY()
        val w = device.displayWidth
        if (toRight) {
            device.swipe((w * 0.25).toInt(), y, (w * 0.90).toInt(), y, 25)
        } else {
            device.swipe((w * 0.80).toInt(), y, (w * 0.10).toInt(), y, 25)
        }
        device.waitForIdle()
    }

    // ------------------------------------------------------------------ session

    fun signOutForTestSetup() {
        FirebaseAuth.getInstance().signOut()
    }

    /**
     * Guarantees the fixed test account is signed in, using the real Login / Register screens.
     * Only touches Firebase to read/clear session state before driving the UI.
     */
    fun ensureSignedInAsTestUser() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser?.email.equals(TEST_EMAIL, ignoreCase = true)) return
        auth.signOut()

        ActivityScenario.launch(AuthActivity::class.java).use {
            waitDisplayed(withId(R.id.login))
            typeInto(R.id.email, TEST_EMAIL)
            typeInto(R.id.password, TEST_PASSWORD)
            onView(withId(R.id.login)).perform(click())

            waitUntil("home or login error", NETWORK_TIMEOUT_MS) {
                if (!isDisplayedNow(withId(R.id.home_greeting)) &&
                    !isDisplayedNow(withId(R.id.error_message))
                ) throw AssertionError("neither home nor error yet")
            }
            if (isDisplayedNow(withId(R.id.home_greeting))) return

            // Account does not exist yet -> create it through the Register screen.
            clickScrolled(R.id.create_account)
            waitDisplayed(withId(R.id.register))
            typeInto(R.id.username, TEST_USERNAME)
            typeInto(R.id.email, TEST_EMAIL)
            typeInto(R.id.password, TEST_PASSWORD)
            typeInto(R.id.confirm_password, TEST_PASSWORD)
            clickScrolled(R.id.register)
            waitDisplayed(withId(R.id.home_greeting), NETWORK_TIMEOUT_MS)
        }
    }

    // -------------------------------------------------------- reusable UI flows

    /** Fills the Add Pet form (must already be open). */
    fun fillPetForm(
        name: String,
        species: String = "Dog",
        breed: String = "Test Breed",
        weight: String = "12.5",
        gender: String = "Male"
    ) {
        waitDisplayed(withId(R.id.et_pet_name))
        typeInto(R.id.et_pet_name, name)
        selectDropdown(R.id.et_species, species)
        typeInto(R.id.et_breed, breed)
        typeInto(R.id.et_weight, weight)
        selectDropdown(R.id.et_gender, gender)
    }

    /** From the My Pets list: Add Pet -> fill -> Save -> back on the list. */
    fun addPetViaUi(name: String, species: String = "Dog", breed: String = "Test Breed", weight: String = "12.5", gender: String = "Male") {
        clickScrolled(R.id.btn_add_pet)
        fillPetForm(name, species, breed, weight, gender)
        clickScrolled(R.id.btn_save_pet)
        waitDisplayed(withId(R.id.btn_add_pet))
        waitItemDisplayed(R.id.rv_pets, listItem(R.id.rv_pets, withText(name)))
    }

    fun createPetViaUi(name: String, species: String = "Dog", breed: String = "Test Breed", weight: String = "12.5", gender: String = "Male") {
        ActivityScenario.launch(PetsActivity::class.java).use {
            waitDisplayed(withId(R.id.btn_add_pet))
            addPetViaUi(name, species, breed, weight, gender)
        }
    }

    fun openPetFromList(name: String) {
        val item = listItem(R.id.rv_pets, withText(name))
        waitItemDisplayed(R.id.rv_pets, item)
        onView(allOf(withId(R.id.item_pet_name), withText(name))).perform(click())
        waitDisplayed(allOf(withId(R.id.pet_details_title), withText(name)))
    }

    /** From the Daily checklist: + -> pick pet -> title -> (optional Daily) -> Save. */
    fun addTaskViaUi(petName: String, title: String, daily: Boolean = false) {
        clickScrolled(R.id.fab_add_task)
        waitDisplayed(withId(R.id.et_task_title))
        clickText(petName, withId(R.id.sp_task_pet))
        typeInto(R.id.et_task_title, title)
        if (daily) clickScrolled(R.id.chip_task_freq_daily)
        clickScrolled(R.id.btn_save_task)
        waitDisplayed(withId(R.id.fab_add_task))
        waitItemDisplayed(R.id.rv_care_tasks, listItem(R.id.rv_care_tasks, withText(title)))
    }

    fun taskRow(@IdRes rvId: Int, title: String, checked: Boolean? = null): Matcher<View> {
        val children = ArrayList<Matcher<View>>()
        children.add(withText(title))
        if (checked != null) {
            children.add(
                allOf(
                    withId(R.id.checkbox_complete),
                    if (checked) androidx.test.espresso.matcher.ViewMatchers.isChecked()
                    else androidx.test.espresso.matcher.ViewMatchers.isNotChecked()
                )
            )
        }
        return listItem(rvId, *children.toTypedArray())
    }

    fun clickTaskCheckbox(@IdRes rvId: Int, title: String) {
        val row = taskRow(rvId, title)
        waitItemDisplayed(rvId, row)
        onView(allOf(withId(R.id.checkbox_complete), isDescendantOfA(row))).perform(click())
    }
}
