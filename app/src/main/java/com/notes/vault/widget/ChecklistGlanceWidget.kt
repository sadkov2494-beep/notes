package com.notes.vault.widget

import android.content.Context
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

private val NoteIdKey = ActionParameters.Key<Long>("note_id")
private val ItemIdKey = ActionParameters.Key<String>("item_id")

class ChecklistGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataHelper.getLatestChecklistNote(context)
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .padding(12.dp)
                ) {
                    Text(
                        text = data?.first?.title?.ifBlank { "Чек-лист" } ?: "Чек-лист",
                        style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface)
                    )
                    val items = data?.second.orEmpty().take(5)
                    if (items.isEmpty()) {
                        Text(
                            text = "Нет чек-листов",
                            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                            modifier = GlanceModifier.padding(top = 8.dp)
                        )
                    } else {
                        val noteId = data!!.first.id
                        items.forEach { item ->
                            Text(
                                text = (if (item.checked) "☑ " else "☐ ") + item.text,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = GlanceTheme.colors.onSurface
                                ),
                                modifier = GlanceModifier
                                    .padding(top = 4.dp)
                                    .clickable(
                                        actionRunCallback<ToggleChecklistItemAction>(
                                            ActionParameters.Builder()
                                                .apply(NoteIdKey, noteId)
                                                .apply(ItemIdKey, item.id)
                                                .build()
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

class ToggleChecklistItemAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val noteId = parameters[NoteIdKey] ?: return
        val itemId = parameters[ItemIdKey] ?: return
        WidgetDataHelper.toggleChecklistItem(context, noteId, itemId)
        ChecklistGlanceWidget().update(context, glanceId)
    }
}

class ChecklistWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChecklistGlanceWidget()
}
