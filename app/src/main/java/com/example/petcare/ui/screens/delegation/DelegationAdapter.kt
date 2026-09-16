package com.example.petcare.ui.screens.delegation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.petcare.R
import com.example.petcare.data.local.entity.DelegationEntity
import com.example.petcare.databinding.ItemDelegationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DelegationAdapter(
    private val onDeleteClick: (DelegationEntity) -> Unit
) : RecyclerView.Adapter<DelegationAdapter.DelegationViewHolder>() {

    private var delegations: List<DelegationEntity> = emptyList()
    private var petNames: Map<Long, String> = emptyMap()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())

    fun submitList(newDelegations: List<DelegationEntity>, newPetNames: Map<Long, String>) {
        delegations = newDelegations
        petNames = newPetNames
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegationViewHolder {
        val binding = ItemDelegationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DelegationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DelegationViewHolder, position: Int) {
        val delegation = delegations[position]
        val petName = petNames[delegation.petId] ?: holder.itemView.context.getString(R.string.bottom_nav_pets)
        holder.bind(delegation, petName, dateFormat, onDeleteClick)
    }

    override fun getItemCount(): Int = delegations.size

    class DelegationViewHolder(private val binding: ItemDelegationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            delegation: DelegationEntity,
            petName: String,
            dateFormat: SimpleDateFormat,
            onDeleteClick: (DelegationEntity) -> Unit
        ) {
            val context = binding.root.context
            val hasName = !delegation.recipientName.isNullOrBlank()
            binding.delegationItemPet.text = petName
            binding.delegationItemDate.text = dateFormat.format(Date(delegation.createdAt))
            binding.delegationItemRecipient.text = context.getString(
                R.string.delegation_to_format,
                delegation.recipientName?.takeIf { it.isNotBlank() } ?: delegation.recipientPhone
            )
            binding.delegationItemContact.isVisible = hasName
            if (hasName) {
                binding.delegationItemContact.text = context.getString(R.string.delegation_contact_format, delegation.recipientPhone)
            }
            binding.delegationItemTasks.text = context.getString(R.string.delegation_tasks_format, delegation.taskTitles)
            binding.btnDeleteDelegation.setOnClickListener { onDeleteClick(delegation) }
        }
    }
}
