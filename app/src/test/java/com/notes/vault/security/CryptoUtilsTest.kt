package com.notes.vault.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoUtilsTest {

    @Test
    fun hashRecoveryPhrase_isDeterministic() {
        val hash1 = CryptoUtils.hashRecoveryPhrase("my recovery phrase")
        val hash2 = CryptoUtils.hashRecoveryPhrase("my recovery phrase")
        assertEquals(hash1, hash2)
    }

    @Test
    fun hashRecoveryPhrase_differsForDifferentInput() {
        val hash1 = CryptoUtils.hashRecoveryPhrase("phrase one")
        val hash2 = CryptoUtils.hashRecoveryPhrase("phrase two")
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun deriveKeyFromPassword_isDeterministicWithSameSalt() {
        val salt = ByteArray(32) { it.toByte() }
        val key1 = CryptoUtils.deriveKeyFromPassword("password123", salt)
        val key2 = CryptoUtils.deriveKeyFromPassword("password123", salt)
        assertArrayEquals(key1, key2)
        assertEquals(32, key1.size)
    }

    @Test
    fun deriveKeyFromPassword_differsWithDifferentSalt() {
        val salt1 = ByteArray(32) { 1 }
        val salt2 = ByteArray(32) { 2 }
        val key1 = CryptoUtils.deriveKeyFromPassword("password123", salt1)
        val key2 = CryptoUtils.deriveKeyFromPassword("password123", salt2)
        assertTrue(!key1.contentEquals(key2))
    }

    @Test
    fun sha256Hex_matchesExpectedFormat() {
        val hash = CryptoUtils.sha256Hex("test data")
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }
}
