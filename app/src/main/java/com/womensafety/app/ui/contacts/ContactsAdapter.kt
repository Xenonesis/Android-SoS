package com.womensafety.app.ui.contacts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.womensafety.app.data.model.Contact
import com.womensafety.app.databinding.ItemContactBinding

class ContactsAdapter(
    private val onContactClick: (Contact) -> Unit,
    private val onDeleteClick: (Contact) -> Unit
) : ListAdapter<Contact, ContactsAdapter.ContactViewHolder>(ContactDiffCallback()) {
    
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
    
    inner class ContactViewHolder(
        private val binding: ItemContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(contact: Contact) {
            binding.apply {
                // Set contact name and phone
                tvContactName.text = contact.name
                tvContactPhone.text = contact.phoneNumber
                
                // Set avatar with first letter of name
                tvContactAvatar.text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?"
                
                // Show primary badge if this is a primary contact
                tvPrimaryBadge.visibility = if (contact.isPrimary) View.VISIBLE else View.GONE
                
                // Set click listeners
                root.setOnClickListener {
                    onContactClick(contact)
                }
                
                btnDelete.setOnClickListener {
                    onDeleteClick(contact)
                }
            }
        }
    }
    
    private class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem == newItem
        }
    }
}
