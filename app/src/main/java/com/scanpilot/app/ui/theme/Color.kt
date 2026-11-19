package com.scanpilot.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ScanPilot colors — dark green, suited to documents and reading
val TealPrimary = Color(0xFF00695C)
val TealDark = Color(0xFF004D40)
val TealLight = Color(0xFFB2DFDB)
val PaperWhite = Color(0xFFFAFAF7)
val InkDark = Color(0xFF1A1A1A)

val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealLight,
    onPrimaryContainer = TealDark,
    secondary = Color(0xFF546E7A),
    background = PaperWhite,
    onBackground = InkDark,
    surface = Color.White,
    onSurface = InkDark
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF00332C),
    primaryContainer = TealDark,
    onPrimaryContainer = TealLight,
    secondary = Color(0xFF90A4AE),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEDEDED)
)
