package com.notes.vault.di

import android.content.Context
import com.notes.vault.data.local.DatabaseProvider
import com.notes.vault.data.local.GroupDao
import com.notes.vault.data.local.NoteDao
import com.notes.vault.data.local.AttachmentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabaseProvider(@ApplicationContext context: Context): DatabaseProvider {
        return DatabaseProvider(context)
    }

    @Provides
    fun provideGroupDao(provider: DatabaseProvider): GroupDao {
        return provider.getNotesDatabase().groupDao()
    }

    @Provides
    fun provideNoteDao(provider: DatabaseProvider): NoteDao {
        return provider.getNotesDatabase().noteDao()
    }

    @Provides
    fun provideAttachmentDao(provider: DatabaseProvider): AttachmentDao {
        return provider.getNotesDatabase().attachmentDao()
    }
}
