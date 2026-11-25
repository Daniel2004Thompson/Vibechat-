package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.SupabaseClientManager
import com.example.ui.theme.VibeCyan
import com.example.ui.theme.VibeEmerald
import com.example.util.DateUtils
import com.example.util.VoiceRecorderHelper
import kotlinx.coroutines.delay

fun parseHexColor(hex: String, fallback: Color = VibeEmerald): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val value = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(0xFF000000 or value)
        } else if (cleaned.length == 8) {
            Color(value)
        } else {
            fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

fun extractInitials(name: String): String {
    val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "VC"
    if (parts.size == 1) return parts[0].take(2).uppercase()
    return "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
}

@Composable
fun VibeAvatar(
    name: String,
    avatarColorHex: String,
    modifier: Modifier = Modifier,
    avatarUri: String? = null,
    size: Dp = 50.dp,
    isOnline: Boolean = false,
    showOnlineIndicator: Boolean = false,
    isGroup: Boolean = false
) {
    val baseColor = parseHexColor(avatarColorHex)
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            baseColor,
                            baseColor.copy(alpha = 0.72f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isGroup) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "$name group icon",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.52f)
                )
            } else {
                Text(
                    text = extractInitials(name),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp
                )
            }

            if (!avatarUri.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "$name avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        if (showOnlineIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF10B981) else Color(0xFF64748B))
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun StoryRingAvatar(
    name: String,
    avatarColorHex: String,
    segmentCount: Int,
    hasUnviewed: Boolean,
    modifier: Modifier = Modifier,
    avatarUri: String? = null,
    size: Dp = 58.dp,
    onClick: () -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val viewedColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            val count = segmentCount.coerceAtLeast(1)
            val gapDegrees = if (count == 1) 0f else 10f
            val sweepPerSegment = (360f - (gapDegrees * count)) / count

            for (i in 0 until count) {
                val startAngle = -90f + i * (sweepPerSegment + gapDegrees)
                drawArc(
                    brush = if (hasUnviewed) {
                        Brush.sweepGradient(listOf(primaryColor, secondaryColor, primaryColor))
                    } else {
                        Brush.linearGradient(listOf(viewedColor, viewedColor))
                    },
                    startAngle = startAngle,
                    sweepAngle = sweepPerSegment,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        VibeAvatar(
            name = name,
            avatarColorHex = avatarColorHex,
            avatarUri = avatarUri,
            size = size - 10.dp
        )
    }
}

@Composable
fun VoiceWaveformPlayer(
    mediaUri: String?,
    durationSec: Int,
    amplitudes: List<Float>,
    isOutgoing: Boolean,
    voiceRecorderHelper: VoiceRecorderHelper,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, durationSec) {
        if (isPlaying) {
            val totalMs = (durationSec.coerceAtLeast(2) * 1000L)
            val stepMs = 80L
            var elapsed = (progress * totalMs).toLong()
            while (isPlaying && elapsed < totalMs) {
                delay(stepMs)
                elapsed += stepMs
                progress = (elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
            }
            if (elapsed >= totalMs) {
                isPlaying = false
                progress = 0f
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isPlaying) {
                voiceRecorderHelper.stopPlayback()
            }
        }
    }

    val activeBarColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    val inactiveBarColor = activeBarColor.copy(alpha = 0.32f)

    Row(
        modifier = modifier
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (isPlaying) {
                    voiceRecorderHelper.stopPlayback()
                    isPlaying = false
                } else {
                    isPlaying = true
                    voiceRecorderHelper.playAudioFile(mediaUri) {
                        isPlaying = false
                        progress = 0f
                    }
                }
            },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(activeBarColor.copy(alpha = 0.16f))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause voice note" else "Play voice note",
                tint = activeBarColor
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .height(30.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bars = if (amplitudes.isEmpty()) {
                listOf(0.3f, 0.6f, 0.85f, 0.45f, 0.9f, 0.5f, 0.7f, 0.4f, 0.8f, 0.6f, 0.35f, 0.75f, 0.55f, 0.4f)
            } else {
                amplitudes
            }
            bars.forEachIndexed { index, amp ->
                val barFraction = index.toFloat() / bars.size.toFloat()
                val isHighlighted = barFraction <= progress
                val barHeight = (amp.coerceIn(0.18f, 1f) * 26f).dp
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(barHeight)
                        .clip(RoundedCornerShape(50))
                        .background(if (isHighlighted) activeBarColor else inactiveBarColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        val displaySec = if (isPlaying) {
            ((1f - progress) * durationSec).toInt().coerceAtLeast(0)
        } else {
            durationSec
        }
        Text(
            text = DateUtils.formatDurationSeconds(displaySec),
            style = MaterialTheme.typography.labelMedium,
            color = activeBarColor.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun LiveRecordingWaveform(
    durationSec: Int,
    currentAmplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_dot"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFF43F5E).copy(alpha = pulseAlpha))
        )
        Text(
            text = DateUtils.formatDurationSeconds(durationSec),
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFFF43F5E),
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 16) {
                val factor = ((kotlin.math.sin((i + durationSec) * 0.9) * 0.35 + 0.65) * currentAmplitude)
                    .toFloat()
                    .coerceIn(0.2f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((factor * 22f).dp)
                        .clip(RoundedCornerShape(50))
                        .background(VibeEmerald)
                )
            }
        }
    }
}

@Composable
fun SupabaseConnectionBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = SupabaseClientManager.isConfigured
    val badgeColor = if (isConnected) VibeEmerald else VibeCyan
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = badgeColor.copy(alpha = 0.14f),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.42f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.Storage,
                contentDescription = "Cloud Service Status",
                tint = badgeColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Cloud Service Connected",
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
