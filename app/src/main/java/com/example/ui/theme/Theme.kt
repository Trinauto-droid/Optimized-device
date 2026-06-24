package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

enum class AppThemePreset(
    val displayName: String,
    val primary: Color,
    val primaryContainer: Color,
    val background: Color,
    val surface: Color,
    val cardColor: Color,
    val secondary: Color
) {
    NEON_PURPLE(
        "Neon Purple",
        Color(0xFFD0BCFF),
        Color(0xFF4A4458),
        Color(0xFF1C1B1F),
        Color(0xFF2B2930),
        Color(0xFF25232A),
        Color(0xFFB2D1FF)
    ),
    EMERALD_GREEN(
        "Emerald Green",
        Color(0xFF10B981),
        Color(0xFF064E3B),
        Color(0xFF091410),
        Color(0xFF11241C),
        Color(0xFF0E1E17),
        Color(0xFF34D399)
    ),
    CYBERPUNK_GOLD(
        "Cyberpunk Gold",
        Color(0xFFF59E0B),
        Color(0xFF78350F),
        Color(0xFF141008),
        Color(0xFF231C0E),
        Color(0xFF1C160B),
        Color(0xFFFBBF24)
    ),
    DEEP_BLUE(
        "Deep Blue",
        Color(0xFF38BDF8),
        Color(0xFF0369A1),
        Color(0xFF0B132B),
        Color(0xFF1C2541),
        Color(0xFF111827),
        Color(0xFF60A5FA)
    ),
    CRIMSON_RAGE(
        "Crimson Rage",
        Color(0xFFF87171),
        Color(0xFF7F1D1D),
        Color(0xFF180A0A),
        Color(0xFF2D1414),
        Color(0xFF220F0F),
        Color(0xFFFCA5A5)
    )
}

object ThemeSelector {
    var currentTheme = mutableStateOf(AppThemePreset.NEON_PURPLE)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for "Elegant Dark"
  dynamicColor: Boolean = false, // Disable dynamic color to enforce our precise hex values
  content: @Composable () -> Unit,
) {
  val preset = ThemeSelector.currentTheme.value
  val colorScheme = darkColorScheme(
      primary = preset.primary,
      onPrimary = Color.Black,
      primaryContainer = preset.primaryContainer,
      onPrimaryContainer = preset.primary,
      secondary = preset.secondary,
      onSecondary = Color.Black,
      tertiary = preset.secondary,
      onTertiary = Color.Black,
      background = preset.background,
      onBackground = Color(0xFFE6E1E5),
      surface = preset.surface,
      onSurface = Color(0xFFE6E1E5),
      surfaceVariant = preset.cardColor,
      onSurfaceVariant = Color(0xFFCAC4D0),
      outline = Color(0xFF938F99),
      error = Color(0xFFF2B8B5),
      onError = Color(0xFF601410)
  )

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
