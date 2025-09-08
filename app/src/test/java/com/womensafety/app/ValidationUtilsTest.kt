package com.womensafety.app

import com.womensafety.app.utils.ValidationUtils
import org.junit.Test
import org.junit.Assert.*

class ValidationUtilsTest {

    @Test
    fun `valid email and password should return valid result`() {
        val result = ValidationUtils.validateEmailAndPassword("test@example.com", "password123")
        assertTrue(result.isValid)
        assertNull(result.emailError)
        assertNull(result.passwordError)
    }

    @Test
    fun `empty email should return email error`() {
        val result = ValidationUtils.validateEmailAndPassword("", "password123")
        assertFalse(result.isValid)
        assertEquals("Email is required", result.emailError)
        assertNull(result.passwordError)
    }

    @Test
    fun `invalid email should return email error`() {
        val result = ValidationUtils.validateEmailAndPassword("invalid-email", "password123")
        assertFalse(result.isValid)
        assertEquals("Please enter a valid email", result.emailError)
        assertNull(result.passwordError)
    }

    @Test
    fun `empty password should return password error`() {
        val result = ValidationUtils.validateEmailAndPassword("test@example.com", "")
        assertFalse(result.isValid)
        assertNull(result.emailError)
        assertEquals("Password is required", result.passwordError)
    }

    @Test
    fun `short password should return password error`() {
        val result = ValidationUtils.validateEmailAndPassword("test@example.com", "123")
        assertFalse(result.isValid)
        assertNull(result.emailError)
        assertEquals("Password must be at least 6 characters", result.passwordError)
    }

    @Test
    fun `isValidEmail returns true for valid email`() {
        assertTrue(ValidationUtils.isValidEmail("test@example.com"))
    }

    @Test
    fun `isValidEmail returns false for invalid email`() {
        assertFalse(ValidationUtils.isValidEmail("invalid-email"))
        assertFalse(ValidationUtils.isValidEmail(""))
    }

    @Test
    fun `isValidPassword returns true for valid password`() {
        assertTrue(ValidationUtils.isValidPassword("password123"))
    }

    @Test
    fun `isValidPassword returns false for invalid password`() {
        assertFalse(ValidationUtils.isValidPassword(""))
        assertFalse(ValidationUtils.isValidPassword("123"))
    }
}
