package com.axainstudios.piclite.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.axainstudios.piclite.domain.OutputFormat

@Composable
fun FormatRow(selected: OutputFormat, gradient: Brush, onSelect: (OutputFormat) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutputFormat.entries.forEach { f ->
            SelectableChip(
                label = f.label,
                selected = selected == f,
                gradient = gradient,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(f) }
            )
        }
    }
}
