package com.womensafety.app.utils

object ValidationUtils {

    private val EMAIL_PATTERN = Regex(
        "[a-zA-Z0-9+._%\\-]{1,256}" +
        "@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(" +
        "\\." +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
        ")+"
    )

    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && EMAIL_PATTERN.matches(email)
    }

    fun isValidPassword(password: String): Boolean {
        return password.isNotEmpty() && password.length >= 6
    }

    fun validateEmailAndPassword(email: String, password: String): ValidationResult {
        val emailError = when {
            email.isEmpty() -> "Email is required"
            !isValidEmail(email) -> "Please enter a valid email"
            else -> null
        }

        val passwordError = when {
            password.isEmpty() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }

        return ValidationResult(emailError, passwordError)
    }

    data class ValidationResult(
        val emailError: String?,
        val passwordError: String?
    ) {
        val isValid: Boolean = emailError == null && passwordError == null
    }
}
