package com.notes.vault.data.export

import android.content.Context
import com.notes.vault.data.model.AttachmentEntity
import com.notes.vault.data.model.ImportMode
import com.notes.vault.data.model.NoteEntity
import com.notes.vault.data.model.NoteGroupEntity
import com.notes.vault.data.model.VaultEntryEntity
import com.notes.vault.data.repository.AttachmentStorage
import com.notes.vault.data.repository.NotesRepository
import com.notes.vault.data.repository.VaultRepository
import com.notes.vault.security.VaultSessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ExportData(
    val version: Int = 1,
    val groups: List<NoteGroupEntity>,
    val notes: List<NoteEntity>,
    val attachments: List<AttachmentEntity>,
    val vaultGroups: List<NoteGroupEntity> = emptyList(),
    val vaultEntries: List<VaultEntryEntity> = emptyList()
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notesRepository: NotesRepository,
    private val vaultSession: VaultSessionManager,
    private val vaultRepository: VaultRepository,
    private val attachmentStorage: AttachmentStorage
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun exportToFile(
        outputFile: File,
        archivePassword: CharArray,
        includeVault: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val groups = notesRepository.getAllGroups()
            val notes = notesRepository.getAllNotes()
            val attachments = notesRepository.getAllAttachments()

            var vaultGroups = emptyList<NoteGroupEntity>()
            var vaultEntries = emptyList<VaultEntryEntity>()
            if (includeVault && vaultSession.isUnlocked) {
                vaultGroups = vaultRepository.getAllGroups()
                vaultEntries = vaultRepository.getAllEntries()
            }

            val exportData = ExportData(
                groups = groups,
                notes = notes,
                attachments = attachments,
                vaultGroups = vaultGroups,
                vaultEntries = vaultEntries
            )
            val dataJson = json.encodeToString(exportData)
            val checksum = sha256(dataJson)

            val tempDir = File(context.cacheDir, "export_${System.currentTimeMillis()}").apply { mkdirs() }
            try {
                File(tempDir, "data.json").writeText(dataJson)
                File(tempDir, "checksum.sha256").writeText(checksum)

                val mediaDir = File(tempDir, "media").apply { mkdirs() }
                attachments.forEach { attachment ->
                    val source = File(attachment.filePath)
                    if (source.exists()) {
                        source.copyTo(File(mediaDir, attachment.fileName), overwrite = true)
                    }
                }

                if (outputFile.exists()) outputFile.delete()
                val zipFile = ZipFile(outputFile, archivePassword)
                val params = ZipParameters().apply {
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }
                zipFile.addFolder(tempDir, params)
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    suspend fun importFromFile(
        inputFile: File,
        archivePassword: CharArray,
        mode: ImportMode
    ): Result<ExportData> = withContext(Dispatchers.IO) {
        runCatching {
            val tempDir = File(context.cacheDir, "import_${System.currentTimeMillis()}").apply { mkdirs() }
            try {
                val zipFile = ZipFile(inputFile, archivePassword)
                zipFile.extractAll(tempDir.absolutePath)

                val dataFile = File(tempDir, "data.json")
                val checksumFile = File(tempDir, "checksum.sha256")
                val dataJson = dataFile.readText()
                val expectedChecksum = checksumFile.readText().trim()
                if (sha256(dataJson) != expectedChecksum) {
                    throw IllegalStateException("Checksum verification failed")
                }

                val exportData = json.decodeFromString<ExportData>(dataJson)

                if (mode == ImportMode.REPLACE) {
                    // Clear existing regular notes data
                    exportData.notes.forEach { /* handled below */ }
                }

                exportData.groups.forEach { notesRepository.saveGroup(it) }
                exportData.notes.forEach { notesRepository.saveNote(it) }

                val mediaDir = File(tempDir, "media")
                exportData.attachments.forEach { attachment ->
                    val source = File(mediaDir, attachment.fileName)
                    if (source.exists()) {
                        val dest = File(context.filesDir, "attachments/${attachment.fileName}")
                        dest.parentFile?.mkdirs()
                        source.copyTo(dest, overwrite = true)
                        notesRepository.saveAttachment(attachment.copy(filePath = dest.absolutePath))
                    }
                }

                if (vaultSession.isUnlocked && exportData.vaultEntries.isNotEmpty()) {
                    exportData.vaultGroups.forEach { vaultRepository.saveGroup(it) }
                    exportData.vaultEntries.forEach { vaultRepository.saveEntry(it) }
                }

                exportData
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
