package com.notes.vault.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import com.notes.vault.data.model.NoteEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

/**
 * Graphical note card: abstract content silhouette (no letter preview),
 * small description field, swipe-left soft delete / swipe-right hard delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicalNoteCard(
    note: NoteEntity,
    accent: Color,
    onDescriptionChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSoftDelete: () -> Unit,
    onHardDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var description by remember(note.id, note.title) { mutableStateOf(note.title) }
    var expanded by remember(note.id) { mutableStateOf(note.content.isBlank() && note.title.isBlank()) }
    var content by remember(note.id, note.content) { mutableStateOf(note.content) }
    val scope = rememberCoroutineScope()
    var saveJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleDescriptionSave(value: String) {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(400)
            onDescriptionChange(value)
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onSoftDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onHardDelete()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFFC62828)
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF6C00)
                else -> Color.Transparent
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.DeleteForever
                else -> Icons.Default.History
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        }
    ) {
        Card(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            scheduleDescriptionSave(it)
                        },
                        placeholder = { Text("···") },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focus ->
                                if (!focus.isFocused && description != note.title) {
                                    onDescriptionChange(description)
                                }
                            },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = accent.copy(alpha = 0.45f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                NoteContentSilhouette(
                    content = note.content.ifBlank { content },
                    accent = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                )

                if (expanded) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = {
                            content = it
                            scope.launch {
                                delay(350)
                                onContentChange(it)
                            }
                        },
                        placeholder = { Text("···") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 10,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NoteContentSilhouette(
    content: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val lines = remember(content) { contentToBars(content) }
    val seed = remember(content) { content.hashCode() }
    Canvas(modifier = modifier.padding(12.dp)) {
        val barCount = max(3, lines.size)
        val gap = 8.dp.toPx()
        val barHeight = ((size.height - gap * (barCount - 1)) / barCount).coerceAtLeast(4.dp.toPx())
        lines.take(barCount).forEachIndexed { index, fraction ->
            val widthFraction = fraction.coerceIn(0.18f, 1f)
            val top = index * (barHeight + gap)
            val barWidth = size.width * widthFraction
            val wobble = ((seed + index * 17) % 11) / 55f
            drawRoundRect(
                color = accent.copy(alpha = 0.55f - index * 0.07f),
                topLeft = Offset(0f, top),
                size = Size(barWidth * (1f - wobble), barHeight),
                cornerRadius = CornerRadius(barHeight / 2, barHeight / 2),
                style = Fill
            )
        }
        // Decorative blobs for attachments / density feel
        val blobCount = (abs(seed) % 3) + 1
        repeat(blobCount) { i ->
            val cx = size.width * (0.72f + i * 0.08f)
            val cy = size.height * (0.25f + i * 0.22f)
            drawCircle(
                color = accent.copy(alpha = 0.35f),
                radius = 5.dp.toPx() + i,
                center = Offset(cx.coerceAtMost(size.width - 4.dp.toPx()), cy)
            )
        }
    }
}

private fun contentToBars(content: String): List<Float> {
    if (content.isBlank()) return listOf(0.35f, 0.55f, 0.28f)
    val parts = content.lines().filter { it.isNotBlank() }.ifEmpty {
        content.chunked(28)
    }
    return parts.take(5).map { line ->
        (line.length / 42f).coerceIn(0.2f, 1f)
    }
}
