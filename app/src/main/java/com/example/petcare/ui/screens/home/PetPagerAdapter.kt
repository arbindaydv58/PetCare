package com.example.petcare.ui.screens.home

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.petcare.R
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.databinding.ItemPetPageBinding
import com.example.petcare.ui.viewmodel.ageInYears
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PetPagerAdapter(
    private val onPetClick: (PetEntity) -> Unit
) : RecyclerView.Adapter<PetPagerAdapter.PetViewHolder>() {

    private var pets: List<PetEntity> = emptyList()
    private var nextCheckupByPetId: Map<Long, Long?> = emptyMap()

    fun submitList(newPets: List<PetEntity>, nextCheckupByPetId: Map<Long, Long?> = emptyMap()) {
        pets = newPets
        this.nextCheckupByPetId = nextCheckupByPetId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val binding = ItemPetPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = pets[position]
        holder.bind(pet, nextCheckupByPetId[pet.id], onPetClick)
    }

    override fun getItemCount(): Int = pets.size

    override fun onViewRecycled(holder: PetViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelImageLoad()
    }

    class PetViewHolder(private val binding: ItemPetPageBinding) : RecyclerView.ViewHolder(binding.root) {
        private var imageJob: Job? = null

        fun bind(pet: PetEntity, nextCheckupMillis: Long?, onPetClick: (PetEntity) -> Unit) {
            binding.petName.text = pet.name
            binding.petSpeciesAge.text = binding.root.context.getString(
                R.string.pet_species_years,
                pet.species,
                pet.ageInYears()
            )
            binding.root.setOnClickListener { onPetClick(pet) }

            if (nextCheckupMillis != null) {
                binding.petCheckupDate.text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(nextCheckupMillis))
            } else {
                binding.petCheckupDate.text = binding.root.context.getString(R.string.pet_no_checkup_label)
            }

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
                        binding.petImage.setImageBitmap(bitmap)
                        binding.petImage.visibility = View.VISIBLE
                        binding.petImagePlaceholder.visibility = View.GONE
                    } else {
                        showPlaceholder()
                    }
                }
            }
        }

        private fun showPlaceholder() {
            binding.petImage.visibility = View.GONE
            binding.petImagePlaceholder.visibility = View.VISIBLE
        }

        fun cancelImageLoad() {
            imageJob?.cancel()
            imageJob = null
        }
    }
}
