package com.notes.vault.data.local

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile
    private var notesDatabase: NotesDatabase? = null

    @Volatile
    private var vaultDatabase: VaultDatabase? = null

    @Volatile
    private var vaultKey: ByteArray? = null

    fun getNotesDatabase(): NotesDatabase {
        return notesDatabase ?: synchronized(this) {
                notesDatabase ?: Room.databaseBuilder(
                context,
                NotesDatabase::class.java,
                "notes.db"
            )
                .fallbackToDestructiveMigration()
                .build().also { notesDatabase = it }
        }
    }

    /**
     * Opens vault DB only after verifying the key. Returns null on wrong key
     * instead of leaving a half-open encrypted database that later crashes.
     */
    fun openVaultDatabase(key: ByteArray): VaultDatabase? {
        if (vaultDatabase != null && vaultKey.contentEquals(key)) {
            return vaultDatabase
        }
        closeVault()
        return synchronized(this) {
            try {
                val db = buildVaultDatabase(key)
                // Force open + verify passphrase before advertising unlocked state
                db.openHelper.readableDatabase.query("SELECT count(*) FROM sqlite_master").use { cursor ->
                    if (!cursor.moveToFirst()) throw IllegalStateException("empty master table")
                }
                vaultKey = key
                vaultDatabase = db
                db
            } catch (_: Exception) {
                closeVault()
                null
            }
        }
    }

    fun getVaultDatabaseOrNull(): VaultDatabase? = vaultDatabase

    fun isVaultOpen(): Boolean = vaultDatabase != null

    fun closeVault() {
        try {
            vaultDatabase?.close()
        } catch (_: Exception) {
        }
        vaultDatabase = null
        vaultKey = null
    }

    fun rekeyVault(newKey: ByteArray) {
        val db = vaultDatabase ?: return
        val passphrase = String(VaultKeyHolder.keyToSqlCipherPassphrase(newKey), Charsets.ISO_8859_1)
            .replace("'", "''")
        db.openHelper.writableDatabase.execSQL("PRAGMA rekey = '$passphrase'")
        vaultKey = newKey
    }

    private fun buildVaultDatabase(key: ByteArray): VaultDatabase {
        SQLiteDatabase.loadLibs(context)
        val factory = SupportFactory(VaultKeyHolder.keyToSqlCipherPassphrase(key))
        return Room.databaseBuilder(
            context,
            VaultDatabase::class.java,
            "vault.db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    fun deleteVaultDatabase() {
        closeVault()
        File(context.getDatabasePath("vault.db").path).delete()
        File(context.getDatabasePath("vault.db-shm").path).delete()
        File(context.getDatabasePath("vault.db-wal").path).delete()
    }
}

object VaultKeyHolder {
    fun keyToSqlCipherPassphrase(key: ByteArray): ByteArray = key
}
