package com.notes.vault.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {

    private val generator = PasswordGenerator()

    @Test
    fun generate_respectsLength() {
        val password = generator.generate(length = 20)
        assertEquals(20, password.length)
    }

    @Test
    fun generate_includesRequiredCharacterClasses() {
        val password = generator.generate(length = 16, uppercase = true, digits = true, special = true)
        assertTrue(password.any { it.isUpperCase() })
        assertTrue(password.any { it.isDigit() })
        assertTrue(password.any { !it.isLetterOrDigit() })
    }

    @Test
    fun generate_lowercaseOnly_whenOthersDisabled() {
        val password = generator.generate(length = 12, uppercase = false, digits = false, special = false)
        assertTrue(password.all { it.isLowerCase() })
    }
}
