package com.example.exp10.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exp10.R
import com.example.exp10.databinding.ItemContactBinding
import com.example.exp10.model.Contact
import kotlin.math.abs

class ContactAdapter(
    private val onItemClick: (Contact) -> Unit,
    private val onQuickShareClick: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(DiffCallback) {

    private var originalList: List<Contact> = emptyList()

    private val avatarColors = intArrayOf(
        R.color.avatar_1,
        R.color.avatar_2,
        R.color.avatar_3,
        R.color.avatar_4,
        R.color.avatar_5,
        R.color.avatar_6,
        R.color.avatar_7,
        R.color.avatar_8
    )

    fun submitFullList(list: List<Contact>) {
        originalList = list
        submitList(list)
    }

    fun filter(query: String, onFilterComplete: (filteredCount: Int) -> Unit) {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) {
            submitList(originalList) {
                onFilterComplete(originalList.size)
            }
        } else {
            val filtered = originalList.filter { contact ->
                contact.name.lowercase().contains(trimmed) ||
                        contact.phoneNumber.replace(Regex("[^0-9+]"), "").contains(trimmed)
            }
            submitList(filtered) {
                onFilterComplete(filtered.size)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            val context = binding.root.context

            binding.tvName.text = contact.name
            binding.tvPhone.text = contact.phoneNumber

            // Avatar setup
            val initials = contact.getInitials()
            binding.tvInitials.text = initials

            // Photo URI or initials
            if (!contact.photoUri.isNullOrBlank()) {
                try {
                    val photoUri = Uri.parse(contact.photoUri)
                    binding.imgPhoto.setImageURI(photoUri)
                    binding.imgPhoto.visibility = View.VISIBLE
                    binding.tvInitials.visibility = View.GONE
                } catch (e: Exception) {
                    showInitialsAvatar(context, contact.name)
                }
            } else {
                showInitialsAvatar(context, contact.name)
            }

            binding.root.setOnClickListener {
                onItemClick(contact)
            }

            binding.btnQuickShare.setOnClickListener {
                onQuickShareClick(contact)
            }
        }

        private fun showInitialsAvatar(context: Context, name: String) {
            binding.imgPhoto.visibility = View.GONE
            binding.tvInitials.visibility = View.VISIBLE

            val colorRes = avatarColors[abs(name.hashCode()) % avatarColors.size]
            val colorInt = ContextCompat.getColor(context, colorRes)

            val background = binding.avatarContainer.background
            if (background is GradientDrawable) {
                background.setColor(colorInt)
            } else {
                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(colorInt)
                }
                binding.avatarContainer.background = shape
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.id == newItem.id && oldItem.phoneNumber == newItem.phoneNumber
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem == newItem
        }
    }
}
