package com.example.petcare.data.remote.model

data class ExternalPetRecord(
    val name: String? = null,
    val species: String? = null,
    val breed: String? = null,
    val vaccination: String? = null,
    val vaccinationDate: String? = null,
    val nextVaccination: String? = null,
    val appointmentDate: String? = null,
    val clinicName: String? = null
)
