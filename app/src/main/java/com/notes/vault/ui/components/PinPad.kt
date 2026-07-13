package com.notes.vault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinDots(
    length: Int,
    filled: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(length) { index ->
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
fun PinPad(
    pin: String,
    pinLength: Int = 4,
    onPinChange: (String) -> Unit,
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )

    Column(modifier = modifier.fillMaxWidth()) {
        PinDots(length = pinLength, filled = pin.length.coerceAtMost(pinLength))
        Spacer(Modifier.height(12.dp))
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.5f)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .then(
                                if (key.isNotEmpty()) {
                                    Modifier.clickable {
                                        when (key) {
                                            "⌫" -> onPinChange(pin.dropLast(1))
                                            else -> {
                                                if (pin.length < pinLength) {
                                                    val next = pin + key
                                                    onPinChange(next)
                                                    if (next.length == pinLength) onComplete(next)
                                                }
                                            }
                                        }
                                    }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "⌫" -> Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Стереть"
                            )
                            "" -> Unit
                            else -> Text(key, fontSize = 24.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
