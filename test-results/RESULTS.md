# PetCare UI automation - definitive run (Xiaomi M2101K6G, Android 13)

Run: `com.example.petcare.ui_tests.PetCareUiTests`, 18 tests, app data wiped before the run.
Result: 14 PASS, 3 FAIL, 1 BLOCKED. Raw instrumentation output: `ui_run_raw.txt`.

| ID | Test | Status | Evidence folder |
|----|------|--------|-----------------|
| T01 | Register new account | PASS | T01_Register |
| T02 | Login valid | PASS | T02_Valid_Login |
| T03 | Login invalid | PASS | T03_Invalid_Login |
| T04 | Create pet | PASS | T04_Create_Pet |
| T05 | Edit pet | PASS | T05_Edit_Pet |
| T06 | Delete pet | PASS | T06_Delete_Pet |
| T07 | Create routine (WEEKLY) | PASS | T07_Create_Care_Routine |
| T08 | Mark task completed | PASS | T08_Mark_Task_Completed |
| T09 | Swipe right | PASS | T09_Swipe_Right |
| T10 | Swipe left (dialog) | PASS | T10_Swipe_Left |
| T11 | Shake device | BLOCKED | T11_Shake_Device (1 screenshot) |
| T12 | Delegate care (SMS) | FAIL | _failures |
| T13 | Healthcare record | PASS | T13_Healthcare_Record |
| T14 | Add expense | PASS | T14_Add_Expense |
| T15 | Monthly budget | FAIL | T15_Monthly_Budget, _failures |
| T16 | Reopen saved info | PASS | T16_Reopen_Saved_Information |
| T17 | Recurring task, one date | PASS | T17_Recurring_Task |
| T18 | Vet data import | FAIL | _failures |

## Reasons
- T11 BLOCKED: real accelerometer shake cannot be injected over ADB on a non-rooted phone.
- T12 / T13 / T18: Pet Details opens with header only (blank body), so Delegate / Add Healthcare / Healthcare card
  are never shown. Intermittent app defect: the affected test differs run to run (T12, T13 or T18).
  Likely cause (from code, unconfirmed): PetViewModel.loadPet sets selectedPet, then resets petUiState to a value equal
  to the initial one, so the StateFlow collector may never re-run renderUiState and pet_details_content stays GONE.
  T12 and T18 have passed in other runs (T12 SMS evidence was verified in earlier runs), T13 passed in this run.
- T15: after saving a budget of 7500 the Expenses screen still shows "Set Monthly Budget" (budget_progress_section
  stays hidden) in every run. Likely cause (unconfirmed): BudgetFormFragment calls setBudget() on its own ViewModel and
  immediately navigates back, which can cancel the save coroutine.
- Also observed (not a test failure): Weekly Plan strip shows Sun 27 - Sat 3 while today is Wed 23, so today is not in it.
