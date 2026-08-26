package com.axainstudios.piclite.ui.screens

import androidx.compose.runtime.Composable

/**
 * Batch processing is the multi-select compression flow: pick many photos,
 * one target size, one tap. It reuses [CompressScreen] with a batch-focused header.
 */
@Composable
fun BatchScreen(dark: Boolean, onBack: () -> Unit) {
    CompressScreen(
        dark = dark,
        onBack = onBack,
        title = "Batch",
        subtitle = "Many photos, one target size"
    )
}
