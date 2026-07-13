package com.notes.vault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class BiometricState {
    IDLE,
    SUCCESS,
    ERROR
}

@Composable
fun AnimatedFingerprintIcon(
    state: BiometricState,
    modifier: Modifier = Modifier
) {
    val tint by animateColorAsState(
        targetValue = when (state) {
            BiometricState.SUCCESS -> Color(0xFF2E7D32)
            BiometricState.ERROR -> Color(0xFFC62828)
            BiometricState.IDLE -> MaterialTheme.colorScheme.primary
        },
        label = "fingerprintTint"
    )
    val scale by animateFloatAsState(
        targetValue = when (state) {
            BiometricState.SUCCESS -> 1.15f
            BiometricState.ERROR -> 0.9f
            BiometricState.IDLE -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fingerprintScale"
    )
    Icon(
        imageVector = Icons.Default.Fingerprint,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(28.dp)
            .scale(scale)
    )
}
