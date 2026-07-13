package com.notes.vault.data.local

import android.content.Context
import androidx.room.Room
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseProvider @Inject constructor(
    private val context: Context
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
            ).build().also { notesDatabase = it }
        }
    }

    fun openVaultDatabase(key: ByteArray): VaultDatabase {
        vaultKey = key
        return vaultDatabase ?: synchronized(this) {
            vaultDatabase ?: buildVaultDatabase(key).also { vaultDatabase = it }
        }
    }

    fun getVaultDatabaseOrNull(): VaultDatabase? = vaultDatabase

    fun isVaultOpen(): Boolean = vaultDatabase != null

    fun closeVault() {
        vaultDatabase?.close()
        vaultDatabase = null
        vaultKey = null
    }

    fun rekeyVault(newKey: ByteArray) {
        val db = vaultDatabase ?: return
        val passphrase = VaultKeyHolder.keyToSqlCipherPassphrase(newKey)
        db.openHelper.writableDatabase.query("PRAGMA rekey = '$passphrase'").close()
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
    fun keyToSqlCipherPassphrase(key: ByteArray): ByteArray {
        return key
    }
}
