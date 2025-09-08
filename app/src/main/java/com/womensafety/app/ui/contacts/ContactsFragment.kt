package com.womensafety.app.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.womensafety.app.R
import com.womensafety.app.SosApplication
import com.womensafety.app.databinding.FragmentContactsBinding
import com.womensafety.app.data.model.Contact

class ContactsFragment : Fragment() {
    
    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ContactsViewModel
    private lateinit var contactsAdapter: ContactsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val application = requireActivity().application as SosApplication
        val contactRepository = application.contactRepository
        if (contactRepository == null) {
            // Handle the case where repository is not available
            // You might want to show an error message or redirect
            return
        }
        viewModel = ViewModelProvider(
            this,
            ContactsViewModelFactory(contactRepository)
        )[ContactsViewModel::class.java]
        
        setupRecyclerView()
        setupUI()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter(
            onContactClick = { contact ->
                // Handle contact click (e.g., call or edit)
                showContactOptionsDialog(contact)
            },
            onDeleteClick = { contact ->
                showDeleteConfirmationDialog(contact)
            }
        )
        
        binding.recyclerViewContacts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contactsAdapter
        }
    }
    
    private fun setupUI() {
        binding.fabAddContact.setOnClickListener {
            showAddContactDialog()
        }
    }
    
    private fun observeViewModel() {
        viewModel.contacts.observe(viewLifecycleOwner) { contacts ->
            contactsAdapter.submitList(contacts)
            
            if (contacts.isEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.recyclerViewContacts.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.recyclerViewContacts.visibility = View.VISIBLE
            }
        }
    }
    
    private fun showAddContactDialog() {
        AddContactDialogFragment { name, phone ->
            viewModel.addContact(name, phone)
        }.show(parentFragmentManager, "AddContactDialog")
    }
    
    private fun showContactOptionsDialog(contact: Contact) {
        val options = arrayOf("Call", "Edit", "Set as Primary", "Delete")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(contact.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.callContact(requireContext(), contact)
                    1 -> showEditContactDialog(contact)
                    2 -> viewModel.setPrimaryContact(contact.id)
                    3 -> showDeleteConfirmationDialog(contact)
                }
            }
            .show()
    }
    
    private fun showEditContactDialog(contact: Contact) {
        AddContactDialogFragment(
            existingContact = contact
        ) { name, phone ->
            viewModel.updateContact(contact.copy(name = name, phoneNumber = phone))
        }.show(parentFragmentManager, "EditContactDialog")
    }
    
    private fun showDeleteConfirmationDialog(contact: Contact) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete ${contact.name}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteContact(contact)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
