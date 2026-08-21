package com.axain.photocompressor.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.axain.photocompressor.navigation.Routes
import com.axain.photocompressor.ui.theme.Amber
import com.axain.photocompressor.ui.theme.CropGreen
import com.axain.photocompressor.ui.theme.ExifRed
import com.axain.photocompressor.ui.theme.Rose
import com.axain.photocompressor.ui.theme.Sky
import com.axain.photocompressor.ui.theme.Violet

data class ToolInfo(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

val allTools: List<ToolInfo> = listOf(
    ToolInfo("Compress", Icons.Rounded.Compress, Violet, Routes.COMPRESS),
    ToolInfo("Resize", Icons.Rounded.AspectRatio, Sky, Routes.RESIZE),
    ToolInfo("Crop", Icons.Rounded.Crop, CropGreen, Routes.CROP),
    ToolInfo("Convert", Icons.Rounded.SwapHoriz, Amber, Routes.CONVERT),
    ToolInfo("Quality", Icons.Rounded.Speed, Rose, Routes.QUALITY),
    ToolInfo("Batch", Icons.Rounded.Layers, Violet, Routes.BATCH),
    ToolInfo("Format", Icons.Rounded.Image, Sky, Routes.CONVERT),
    ToolInfo("Enhance", Icons.Rounded.AutoFixHigh, Amber, Routes.ENHANCE),
    ToolInfo("Delete EXIF", Icons.Rounded.Delete, ExifRed, Routes.DELETE_EXIF),
)
