package com.example.petcare.data.local.relationship

import androidx.room.Embedded
import androidx.room.Relation
import com.example.petcare.data.local.entity.CareRoutineEntity
import com.example.petcare.data.local.entity.CareTaskEntity
import com.example.petcare.data.local.entity.ExpenseEntity
import com.example.petcare.data.local.entity.HealthcareRecordEntity
import com.example.petcare.data.local.entity.PetEntity

data class PetWithCareRoutines(
    @Embedded val pet: PetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "petId"
    )
    val routines: List<CareRoutineEntity>
)

data class PetWithCareTasks(
    @Embedded val pet: PetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "petId"
    )
    val tasks: List<CareTaskEntity>
)

data class PetWithHealthcareRecords(
    @Embedded val pet: PetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "petId"
    )
    val records: List<HealthcareRecordEntity>
)

data class PetWithExpenses(
    @Embedded val pet: PetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "petId"
    )
    val expenses: List<ExpenseEntity>
)

data class PetWithAllRelations(
    @Embedded val pet: PetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "petId"
    )
    val routines: List<CareRoutineEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "petId"
    )
    val tasks: List<CareTaskEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "petId"
    )
    val records: List<HealthcareRecordEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "petId"
    )
    val expenses: List<ExpenseEntity>
)
