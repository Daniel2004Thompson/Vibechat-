package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// VibeChat Core Palette (Inspired by Chat-Connect / VibeChat Emerald & Midnight Aesthetic)
val VibeEmerald = Color(0xFF10B981)
val VibeEmeraldDark = Color(0xFF059669)
val VibeEmeraldLight = Color(0xFF34D399)
val VibeEmeraldContainer = Color(0xFF064E3B)
val VibeOnEmeraldContainer = Color(0xFFA7F3D0)

val VibeCyan = Color(0xFF06B6D4)
val VibeCyanDark = Color(0xFF0891B2)
val VibeViolet = Color(0xFF8B5CF6)
val VibeCoral = Color(0xFFF43F5E)
val VibeAmber = Color(0xFFF59E0B)

// Dark Theme Surfaces
val DarkBackground = Color(0xFF0B1120)
val DarkSurface = Color(0xFF111827)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkSurfaceElevated = Color(0xFF26334D)
val DarkOnSurface = Color(0xFFF8FAFC)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF334155)

// Light Theme Surfaces
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightSurfaceElevated = Color(0xFFE2E8F0)
val LightOnSurface = Color(0xFF0F172A)
val LightOnSurfaceVariant = Color(0xFF64748B)
val LightOutline = Color(0xFFCBD5E1)

// Avatar & Status Gradient Palettes
val AvatarColors = listOf(
    "#10B981", // Emerald
    "#06B6D4", // Cyan
    "#6366F1", // Indigo
    "#8B5CF6", // Violet
    "#EC4899", // Pink
    "#F43F5E", // Rose
    "#F59E0B", // Amber
    "#3B82F6"  // Blue
)

val StatusBackgroundColors = listOf(
    "#0F172A", // Midnight Slate
    "#065F46", // Deep Emerald
    "#1E1B4B", // Deep Indigo
    "#4C1D95", // Royal Violet
    "#831843", // Crimson Rose
    "#0C4A6E", // Ocean Cyan
    "#78350F", // Warm Amber
    "#14532D"  // Forest Green
)

enum class ChatAccentTheme(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color,
    val outgoingBubbleDark: Color,
    val outgoingBubbleLight: Color
) {
    EMERALD(
        id = "emerald",
        label = "Emerald Vibe",
        primary = Color(0xFF10B981),
        secondary = Color(0xFF06B6D4),
        outgoingBubbleDark = Color(0xFF065F46),
        outgoingBubbleLight = Color(0xFFD1FAE5)
    ),
    CYBER_CYAN(
        id = "cyan",
        label = "Cyber Teal",
        primary = Color(0xFF06B6D4),
        secondary = Color(0xFF3B82F6),
        outgoingBubbleDark = Color(0xFF155E75),
        outgoingBubbleLight = Color(0xFFCFFAFE)
    ),
    ELECTRIC_INDIGO(
        id = "indigo",
        label = "Electric Indigo",
        primary = Color(0xFF6366F1),
        secondary = Color(0xFF8B5CF6),
        outgoingBubbleDark = Color(0xFF3730A3),
        outgoingBubbleLight = Color(0xFFE0E7FF)
    ),
    SUNSET_ROSE(
        id = "rose",
        label = "Sunset Coral",
        primary = Color(0xFFF43F5E),
        secondary = Color(0xFFFB923C),
        outgoingBubbleDark = Color(0xFF881337),
        outgoingBubbleLight = Color(0xFFFFE4E6)
    ),
    ROYAL_AMETHYST(
        id = "amethyst",
        label = "Royal Amethyst",
        primary = Color(0xFF8B5CF6),
        secondary = Color(0xFFEC4899),
        outgoingBubbleDark = Color(0xFF4C1D95),
        outgoingBubbleLight = Color(0xFFEDE9FE)
    );

    companion object {
        fun fromId(id: String): ChatAccentTheme =
            entries.find { it.id == id } ?: EMERALD
    }
}
