package com.notes.vault.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.notes.vault.data.model.NoteEntity
import com.notes.vault.data.model.NoteGroupEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NotesDatabaseTest {

    private lateinit var db: NotesDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NotesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndObserveNote() = runBlocking {
        val id = db.noteDao().insert(NoteEntity(title = "Test", content = "Body"))
        val notes = db.noteDao().observeAllRegular().first()
        assertEquals(1, notes.size)
        assertEquals("Test", notes.first().title)
        assertEquals(id, notes.first().id)
    }

    @Test
    fun insertGroupAndFilterByVault() = runBlocking {
        db.groupDao().insert(NoteGroupEntity(name = "Work", colorArgb = 0xFF0000, iconName = "work", isVault = false))
        db.groupDao().insert(NoteGroupEntity(name = "Secrets", colorArgb = 0x00FF00, iconName = "lock", isVault = true))
        val regular = db.groupDao().observeRegularGroups().first()
        val vault = db.groupDao().observeVaultGroups().first()
        assertEquals(1, regular.size)
        assertEquals("Work", regular.first().name)
        assertEquals(1, vault.size)
        assertEquals("Secrets", vault.first().name)
    }
}
