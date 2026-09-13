package com.example.petcare

import com.example.petcare.ui.viewmodel.PetFormState
import com.example.petcare.ui.viewmodel.PetViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetValidationTest {
    @Test
    fun emptyPetNameValidation() {
        val result = PetViewModel.validatePetInput(
            PetFormState(
                name = "",
                species = "Dog",
                weight = "12"
            )
        )

        assertEquals("Pet name cannot be empty.", result)
    }

    @Test
    fun emptySpeciesValidation() {
        val result = PetViewModel.validatePetInput(
            PetFormState(
                name = "Max",
                species = "",
                weight = "12"
            )
        )

        assertEquals("Species cannot be empty.", result)
    }

    @Test
    fun invalidWeightValidation() {
        val result = PetViewModel.validatePetInput(
            PetFormState(
                name = "Max",
                species = "Dog",
                weight = "-5"
            )
        )

        assertEquals("Weight must be a valid positive number.", result)
    }

    @Test
    fun validPetInformationValidation() {
        val result = PetViewModel.validatePetInput(
            PetFormState(
                name = "Max",
                species = "Dog",
                weight = "28",
                gender = "Male",
                breed = "Golden Retriever"
            )
        )

        assertNull(result)
    }
}
