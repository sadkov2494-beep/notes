package com.notes.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val groupColors = listOf(
    0xFFE57373.toInt(),
    0xFF81C784.toInt(),
    0xFF64B5F6.toInt(),
    0xFFFFB74D.toInt(),
    0xFFBA68C8.toInt(),
    0xFF4DB6AC.toInt()
)

private val groupIcons = listOf(
    Icons.Default.Folder to "folder",
    Icons.Default.Star to "star",
    Icons.Default.Home to "home",
    Icons.Default.Work to "work"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateGroupDialog(
    isVault: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, colorArgb: Int, iconName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableIntStateOf(groupColors.first()) }
    var selectedIcon by remember { mutableStateOf(groupIcons.first().second) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая группа") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название группы") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Цвет")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    groupColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable { selectedColor = color }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Иконка")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    groupIcons.forEach { (icon, iconName) ->
                        Icon(
                            imageVector = icon,
                            contentDescription = iconName,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { selectedIcon = iconName }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, selectedColor, selectedIcon) },
                enabled = name.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
