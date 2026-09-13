package com.example.petcare.ui.screens.pet

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.petcare.R
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.databinding.ItemPetBinding
import com.example.petcare.ui.viewmodel.ageInYears
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PetAdapter(
    private val onPetClick: (PetEntity) -> Unit
) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {

    private var pets: List<PetEntity> = emptyList()

    fun submitList(newPets: List<PetEntity>) {
        pets = newPets
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val binding = ItemPetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        holder.bind(pets[position], onPetClick)
    }

    override fun getItemCount(): Int = pets.size

    override fun onViewRecycled(holder: PetViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelImageLoad()
    }

    class PetViewHolder(private val binding: ItemPetBinding) : RecyclerView.ViewHolder(binding.root) {
        private var imageJob: Job? = null

        fun bind(pet: PetEntity, onPetClick: (PetEntity) -> Unit) {
            binding.itemPetName.text = pet.name
            binding.itemPetSpeciesBreed.text = if (pet.breed.isNotBlank()) {
                "${pet.species} • ${pet.breed}"
            } else {
                pet.species
            }
            binding.itemPetAge.text = binding.root.context.getString(R.string.pet_age_years, pet.ageInYears())
            binding.itemPetWeight.text = binding.root.context.getString(
                R.string.pet_weight_kg,
                pet.weight.toString()
            )
            binding.root.setOnClickListener { onPetClick(pet) }

            cancelImageLoad()
            val imageUri = pet.imageUri
            if (imageUri.isNullOrBlank()) {
                showPlaceholder()
            } else {
                imageJob = CoroutineScope(Dispatchers.Main).launch {
                    val bitmap = withContext(Dispatchers.IO) {
                        runCatching {
                            if (imageUri.startsWith("/")) {
                                BitmapFactory.decodeFile(imageUri)
                            } else {
                                binding.root.context.contentResolver.openInputStream(Uri.parse(imageUri))?.use {
                                    BitmapFactory.decodeStream(it)
                                }
                            }
                        }.getOrNull()
                    }
                    if (bitmap != null) {
                        binding.itemPetImage.setImageBitmap(bitmap)
                        binding.itemPetImage.visibility = View.VISIBLE
                        binding.itemPetImagePlaceholder.visibility = View.GONE
                    } else {
                        showPlaceholder()
                    }
                }
            }
        }

        private fun showPlaceholder() {
            binding.itemPetImage.visibility = View.GONE
            binding.itemPetImagePlaceholder.visibility = View.VISIBLE
        }

        fun cancelImageLoad() {
            imageJob?.cancel()
            imageJob = null
        }
    }
}
