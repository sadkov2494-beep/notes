package com.notes.vault.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.notes.vault.MainActivity

class QuickNoteGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Быстрая заметка",
                        style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface)
                    )
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(
                            text = "✏️ Текст",
                            modifier = GlanceModifier
                                .padding(end = 16.dp)
                                .clickable(actionRunCallback<OpenQuickNoteAction>()),
                            style = TextStyle(color = GlanceTheme.colors.primary)
                        )
                        Text(
                            text = "🎤 Голос",
                            modifier = GlanceModifier.clickable(actionRunCallback<OpenVoiceInputAction>()),
                            style = TextStyle(color = GlanceTheme.colors.primary)
                        )
                    }
                }
            }
        }
    }
}

class OpenQuickNoteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EXTRA_QUICK_NOTE, true)
        }
        context.startActivity(intent)
    }
}

class OpenVoiceInputAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, VoiceInputActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

class QuickNoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickNoteGlanceWidget()
}

const val EXTRA_QUICK_NOTE = "quick_note"
