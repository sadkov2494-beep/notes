package com.notes.vault.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class VaultCryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "vault_secure_prefs"
        private const val KEY_SALT = "vault_salt"
        private const val KEY_RECOVERY_HASH = "vault_recovery_hash"
        private const val KEY_BIOMETRIC_WRAPPED = "vault_biometric_wrapped"
        private const val KEY_BIOMETRIC_IV = "vault_biometric_iv"
        private const val KEY_VAULT_CREATED = "vault_created"
        private const val KEY_UNLOCK_MODE = "vault_unlock_mode"
        private const val KEY_DEVICE_BOOT_COUNT = "device_boot_count"
        private const val BIOMETRIC_KEY_ALIAS = "notes_vault_biometric_key"
    }

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isVaultCreated(): Boolean = encryptedPrefs.getBoolean(KEY_VAULT_CREATED, false)

    fun markVaultCreated() {
        encryptedPrefs.edit().putBoolean(KEY_VAULT_CREATED, true).apply()
    }

    fun saveSalt(salt: ByteArray) {
        encryptedPrefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .apply()
    }

    fun getSalt(): ByteArray? {
        val encoded = encryptedPrefs.getString(KEY_SALT, null) ?: return null
        return Base64.decode(encoded, Base64.NO_WRAP)
    }

    fun saveRecoveryHash(hash: String) {
        encryptedPrefs.edit().putString(KEY_RECOVERY_HASH, hash).apply()
    }

    fun verifyRecoveryPhrase(phrase: String): Boolean {
        val stored = encryptedPrefs.getString(KEY_RECOVERY_HASH, null) ?: return false
        return stored == hashRecoveryPhrase(phrase)
    }

    fun hashRecoveryPhrase(phrase: String): String = CryptoUtils.hashRecoveryPhrase(phrase)

    fun deriveKeyFromPassword(password: String, salt: ByteArray): ByteArray =
        CryptoUtils.deriveKeyFromPassword(password, salt)

    fun createVault(password: String, recoveryPhrase: String): ByteArray {
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key = deriveKeyFromPassword(password, salt)
        saveSalt(salt)
        saveRecoveryHash(hashRecoveryPhrase(recoveryPhrase))
        markVaultCreated()
        setUnlockMode(
            if (password.length == 4 && password.all { it.isDigit() }) "PIN" else "PASSWORD"
        )
        return key
    }

    fun setUnlockMode(mode: String) {
        encryptedPrefs.edit().putString(KEY_UNLOCK_MODE, mode).apply()
    }

    fun getUnlockMode(): String {
        return encryptedPrefs.getString(KEY_UNLOCK_MODE, null)
            ?: "PASSWORD" // old installs used master password
    }

    fun usesPin(): Boolean = getUnlockMode() == "PIN"

    fun unlockWithPassword(password: String): ByteArray? {
        val salt = getSalt() ?: return null
        return deriveKeyFromPassword(password, salt)
    }

    fun resetPassword(newPassword: String, recoveryPhrase: String): ByteArray? {
        if (!verifyRecoveryPhrase(recoveryPhrase)) return null
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key = deriveKeyFromPassword(newPassword, salt)
        saveSalt(salt)
        clearBiometricKey()
        return key
    }

    fun isBiometricAvailable(): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isBiometricKeyValid(): Boolean {
        if (!encryptedPrefs.contains(KEY_BIOMETRIC_WRAPPED)) return false
        val bootCount = android.provider.Settings.Global.getInt(
            context.contentResolver,
            android.provider.Settings.Global.BOOT_COUNT,
            0
        )
        val storedBoot = encryptedPrefs.getInt(KEY_DEVICE_BOOT_COUNT, -1)
        if (storedBoot != bootCount) {
            clearBiometricKey()
            return false
        }
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)
        } catch (_: Exception) {
            false
        }
    }

    fun wrapKeyForBiometric(vaultKey: ByteArray) {
        val secretKey = getOrCreateBiometricKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val wrapped = cipher.doFinal(vaultKey)
        val bootCount = android.provider.Settings.Global.getInt(
            context.contentResolver,
            android.provider.Settings.Global.BOOT_COUNT,
            0
        )
        encryptedPrefs.edit()
            .putString(KEY_BIOMETRIC_WRAPPED, Base64.encodeToString(wrapped, Base64.NO_WRAP))
            .putString(KEY_BIOMETRIC_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putInt(KEY_DEVICE_BOOT_COUNT, bootCount)
            .apply()
    }

    fun unwrapKeyWithBiometric(cipher: Cipher): ByteArray? {
        val wrappedB64 = encryptedPrefs.getString(KEY_BIOMETRIC_WRAPPED, null) ?: return null
        val wrapped = Base64.decode(wrappedB64, Base64.NO_WRAP)
        return cipher.doFinal(wrapped)
    }

    fun getBiometricCipherForDecrypt(): Cipher? {
        if (!isBiometricKeyValid()) return null
        val ivB64 = encryptedPrefs.getString(KEY_BIOMETRIC_IV, null) ?: return null
        val iv = Base64.decode(ivB64, Base64.NO_WRAP)
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val secretKey = keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as? SecretKey ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return cipher
    }

    fun clearBiometricKey() {
        encryptedPrefs.edit()
            .remove(KEY_BIOMETRIC_WRAPPED)
            .remove(KEY_BIOMETRIC_IV)
            .remove(KEY_DEVICE_BOOT_COUNT)
            .apply()
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
                keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)
            }
        } catch (_: Exception) {
        }
    }

    private fun getOrCreateBiometricKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            return keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as SecretKey
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            BIOMETRIC_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    suspend fun authenticateBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): Result<ByteArray> = suspendCoroutine { continuation ->
        val cipher = getBiometricCipherForDecrypt()
        if (cipher == null) {
            continuation.resume(Result.failure(IllegalStateException("Biometric key unavailable")))
            return@suspendCoroutine
        }
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val crypto = result.cryptoObject?.cipher
                    if (crypto == null) {
                        continuation.resume(Result.failure(IllegalStateException("No cipher")))
                        return
                    }
                    try {
                        val key = unwrapKeyWithBiometric(crypto)
                        if (key != null) {
                            continuation.resume(Result.success(key))
                        } else {
                            continuation.resume(Result.failure(IllegalStateException("Unwrap failed")))
                        }
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    continuation.resume(Result.failure(Exception(errString.toString())))
                }

                override fun onAuthenticationFailed() {
                    // Wait for retry
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .setNegativeButtonText("PIN-код")
            .build()
        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    fun createVaultWithPin(pin: String, recoveryPhrase: String): ByteArray {
        return createVault(pin, recoveryPhrase)
    }

    fun unlockWithPin(pin: String): ByteArray? = unlockWithPassword(pin)

    fun biometricLabel(): String {
        val manager = BiometricManager.from(context)
        val face = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return when {
            face == BiometricManager.BIOMETRIC_SUCCESS -> "отпечатку или лицу"
            else -> "отпечатку пальца"
        }
    }
}
