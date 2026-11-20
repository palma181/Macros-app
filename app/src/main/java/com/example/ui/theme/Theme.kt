package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class MacroColors(
    val calories: Color,
    val protein: Color,
    val carbs: Color,
    val fat: Color
)

val LocalMacroColors = staticCompositionLocalOf {
    MacroColors(
        calories = EmeraldPrimary,
        protein = ProteinColorLight,
        carbs = CarbsColorLight,
        fat = FatColorLight
    )
}

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = SageSecondary,
    onSecondary = SageOnSecondary,
    secondaryContainer = SageContainer,
    onSecondaryContainer = SageOnContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    background = WarmBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = WarmSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = WarmSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = WarmOutlineLight,
    outlineVariant = WarmOutlineLight.copy(alpha = 0.6f),
    error = ErrorLight
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldOnContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = SageOnSecondaryDark,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = SageOnContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = AmberOnTertiaryDark,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = AmberOnContainerDark,
    background = WarmBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = WarmSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = WarmSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = WarmOutlineDark,
    outlineVariant = WarmOutlineDark.copy(alpha = 0.6f),
    error = ErrorDark
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MisMacrosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val macroColors = if (darkTheme) {
        MacroColors(
            calories = EmeraldPrimaryDark,
            protein = ProteinColorDark,
            carbs = CarbsColorDark,
            fat = FatColorDark
        )
    } else {
        MacroColors(
            calories = EmeraldPrimary,
            protein = ProteinColorLight,
            carbs = CarbsColorLight,
            fat = FatColorLight
        )
    }

    CompositionLocalProvider(LocalMacroColors provides macroColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
