package com.womensafety.app.ui.contacts

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.womensafety.app.R
import com.womensafety.app.data.model.Contact

class AddContactDialogFragment(
    private val existingContact: Contact? = null,
    private val onContactSaved: (name: String, phone: String) -> Unit
) : DialogFragment() {
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        
        val nameInputLayout = view.findViewById<TextInputLayout>(R.id.til_contact_name)
        val nameEditText = view.findViewById<TextInputEditText>(R.id.et_contact_name)
        val phoneInputLayout = view.findViewById<TextInputLayout>(R.id.til_contact_phone)
        val phoneEditText = view.findViewById<TextInputEditText>(R.id.et_contact_phone)
        
        // Pre-fill if editing existing contact
        existingContact?.let { contact ->
            nameEditText.setText(contact.name)
            phoneEditText.setText(contact.phoneNumber)
        }
        
        val title = if (existingContact != null) "Edit Contact" else "Add Contact"
        val positiveButtonText = if (existingContact != null) "Update" else "Add"
        
        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(view)
            .setPositiveButton(positiveButtonText) { _, _ ->
                val name = nameEditText.text.toString().trim()
                val phone = phoneEditText.text.toString().trim()
                
                if (validateInput(name, phone, nameInputLayout, phoneInputLayout)) {
                    onContactSaved(name, phone)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
    
    private fun validateInput(
        name: String,
        phone: String,
        nameInputLayout: TextInputLayout,
        phoneInputLayout: TextInputLayout
    ): Boolean {
        var isValid = true
        
        // Clear previous errors
        nameInputLayout.error = null
        phoneInputLayout.error = null
        
        // Validate name
        if (name.isEmpty()) {
            nameInputLayout.error = "Name is required"
            isValid = false
        }
        
        // Validate phone
        if (phone.isEmpty()) {
            phoneInputLayout.error = "Phone number is required"
            isValid = false
        } else if (!isValidPhoneNumber(phone)) {
            phoneInputLayout.error = "Please enter a valid phone number"
            isValid = false
        }
        
        if (!isValid) {
            Toast.makeText(requireContext(), "Please fix the errors above", Toast.LENGTH_SHORT).show()
        }
        
        return isValid
    }
    
    private fun isValidPhoneNumber(phone: String): Boolean {
        val digitsOnly = phone.replace(Regex("[^0-9]"), "")
        return digitsOnly.length >= 10
    }
}
