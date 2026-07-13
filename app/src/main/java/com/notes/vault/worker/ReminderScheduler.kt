package com.notes.vault.worker

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun schedule(noteId: Long, title: String, reminderAt: Long) {
        val delay = reminderAt - System.currentTimeMillis()
        if (delay <= 0) return
        val data = Data.Builder()
            .putLong(ReminderWorker.KEY_NOTE_ID, noteId)
            .putString(ReminderWorker.KEY_TITLE, title)
            .build()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("reminder_$noteId")
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun cancel(noteId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag("reminder_$noteId")
    }
}
