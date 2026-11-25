package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalChatAccentTheme = staticCompositionLocalOf { ChatAccentTheme.EMERALD }

val VibeChatShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VibeChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentTheme: ChatAccentTheme = ChatAccentTheme.EMERALD,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentTheme.primary,
            onPrimary = Color(0xFF042F2E),
            primaryContainer = accentTheme.outgoingBubbleDark,
            onPrimaryContainer = Color(0xFFECFDF5),
            secondary = accentTheme.secondary,
            onSecondary = Color(0xFF082F49),
            secondaryContainer = DarkSurfaceElevated,
            onSecondaryContainer = DarkOnSurface,
            tertiary = VibeViolet,
            background = DarkBackground,
            onBackground = DarkOnSurface,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurfaceVariant,
            outline = DarkOutline,
            error = VibeCoral
        )
    } else {
        lightColorScheme(
            primary = accentTheme.primary,
            onPrimary = Color.White,
            primaryContainer = accentTheme.outgoingBubbleLight,
            onPrimaryContainer = Color(0xFF064E3B),
            secondary = accentTheme.secondary,
            onSecondary = Color.White,
            secondaryContainer = LightSurfaceElevated,
            onSecondaryContainer = LightOnSurface,
            tertiary = VibeViolet,
            background = LightBackground,
            onBackground = LightOnSurface,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurfaceVariant,
            outline = LightOutline,
            error = VibeCoral
        )
    }

    CompositionLocalProvider(LocalChatAccentTheme provides accentTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = VibeChatShapes,
            content = content
        )
    }
}
