package com.thinh.snaplet.ui.screens.maintenance

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.theme.Primary70
import kotlinx.coroutines.delay

@Composable
fun MaintenanceScreen(estimatedEndTime: String) {
    var dotCount by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(600)
            dotCount = if (dotCount >= 3) 1 else dotCount + 1
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ring1",
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.05f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ring2",
    )

    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.06f), background.copy(alpha = 0f)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            // Logo + pulse rings
            Box(
                modifier = Modifier.size(130.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale1)
                        .border(
                            width = 1.dp, color = primary.copy(alpha = 0.2f), shape = CircleShape
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .scale(pulseScale2)
                        .border(
                            width = 1.dp, color = primary.copy(alpha = 0.12f), shape = CircleShape
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(primary, Primary70),
                                start = Offset(0f, 0f),
                                end = Offset(64f, 64f),
                            ),
                            shape = RoundedCornerShape(18.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "S",
                        color = onPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        lineHeight = 30.sp,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Status pill with animated dots
            Row(
                modifier = Modifier
                    .background(
                        surfaceContainerHigh, shape = RoundedCornerShape(99.dp)
                    )
                    .border(1.dp, outlineVariant, shape = RoundedCornerShape(99.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = null,
                    tint = onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                BaseText(
                    text = stringResource(R.string.maintenance_status_label) + ".".repeat(dotCount),
                    typography = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(20.dp))

            BaseText(
                text = stringResource(R.string.maintenance_title),
                typography = MaterialTheme.typography.headlineMedium,
                color = onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 36.dp),
            )

            Spacer(Modifier.height(12.dp))

            BaseText(
                text = stringResource(R.string.maintenance_body),
                typography = MaterialTheme.typography.bodySmall,
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 36.dp),
            )

            Spacer(Modifier.height(36.dp))

            // Time card
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .background(surface, shape = RoundedCornerShape(20.dp))
                    .border(1.dp, primaryContainer, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BaseText(
                    text = stringResource(R.string.maintenance_card_label),
                    typography = MaterialTheme.typography.labelMedium,
                    color = onSurfaceVariant,
                )

                Spacer(Modifier.height(12.dp))

                if (estimatedEndTime.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        BaseText(
                            text = estimatedEndTime,
                            typography = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primary,
                        )
                    }
                } else {
                    BaseText(
                        text = stringResource(R.string.maintenance_no_time),
                        typography = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primary,
                    )
                }

                Spacer(Modifier.height(14.dp))

                HorizontalDivider(color = outlineVariant.copy(alpha = 0.5f))

                Spacer(Modifier.height(12.dp))

                BaseText(
                    text = stringResource(R.string.maintenance_card_footer),
                    typography = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Normal,
                    color = onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )
            }

            Spacer(Modifier.weight(1f))

            // Bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BaseText(
                    text = stringResource(R.string.maintenance_status_update),
                    typography = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Normal,
                    color = onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                BaseText(
                    text = stringResource(R.string.maintenance_handle),
                    typography = MaterialTheme.typography.labelMedium,
                    color = primary,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(5.dp)
                        .background(onSurface.copy(alpha = 0.15f), shape = CircleShape),
                )
            }
        }
    }
}
