package com.notes.vault.security

import com.notes.vault.data.local.DatabaseProvider
import com.notes.vault.data.local.VaultEntryDao
import com.notes.vault.data.local.GroupDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultSessionManager @Inject constructor(
    private val cryptoManager: VaultCryptoManager,
    private val databaseProvider: DatabaseProvider
) {
    private var currentKey: ByteArray? = null

    val isUnlocked: Boolean get() = databaseProvider.isVaultOpen()
    val isCreated: Boolean get() = cryptoManager.isVaultCreated()

    fun unlockWithPassword(password: String): Boolean {
        val key = cryptoManager.unlockWithPassword(password) ?: return false
        databaseProvider.openVaultDatabase(key)
        currentKey = key
        return true
    }

    fun unlockWithPin(pin: String): Boolean = unlockWithPassword(pin)

    fun createVault(password: String, recoveryPhrase: String): Boolean {
        val key = cryptoManager.createVault(password, recoveryPhrase)
        databaseProvider.openVaultDatabase(key)
        currentKey = key
        return true
    }

    fun createVaultWithPin(pin: String, recoveryPhrase: String): Boolean =
        createVault(pin, recoveryPhrase)

    fun unlockWithKey(key: ByteArray) {
        databaseProvider.openVaultDatabase(key)
        currentKey = key
    }

    fun enableBiometric(): Boolean {
        val key = currentKey ?: return false
        if (!cryptoManager.isBiometricAvailable()) return false
        cryptoManager.wrapKeyForBiometric(key)
        return true
    }

    fun lock() {
        databaseProvider.closeVault()
        currentKey = null
    }

    fun resetPassword(newPassword: String, recoveryPhrase: String): Boolean {
        val key = cryptoManager.resetPassword(newPassword, recoveryPhrase) ?: return false
        if (databaseProvider.isVaultOpen()) {
            databaseProvider.rekeyVault(key)
        } else {
            databaseProvider.openVaultDatabase(key)
        }
        currentKey = key
        return true
    }

    fun vaultGroupDao(): GroupDao? = databaseProvider.getVaultDatabaseOrNull()?.groupDao()
    fun vaultEntryDao(): VaultEntryDao? = databaseProvider.getVaultDatabaseOrNull()?.vaultEntryDao()
}
