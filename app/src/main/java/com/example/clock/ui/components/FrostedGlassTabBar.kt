package com.example.clock.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ClockTab(val title: String, val icon: ImageVector) {
    WORLD_CLOCK("World Clock", Icons.Default.Public),
    ALARMS("Alarms", Icons.Default.Alarm),
    BEDTIME("Bedtime", Icons.Default.Bedtime),
    STOPWATCH("Stopwatch", Icons.Default.Timer),
    TIMERS("Timers", Icons.Default.HourglassBottom)
}

@Composable
fun FrostedGlassTabBar(
    selectedTab: ClockTab,
    onTabSelected: (ClockTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black

    val barBackground = if (isDark) {
        Color(0xCC1C1C1E) // 80% opacity dark frosted surface
    } else {
        Color(0xDDF2F2F7) // 85% opacity light frosted surface
    }

    val accentColor = Color(0xFF0A84FF)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(barBackground)
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = RenderEffect.createBlurEffect(
                            25f, 25f, Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClockTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                val pillBackground by animateColorAsState(
                    targetValue = if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Transparent,
                    animationSpec = spring(stiffness = 300f),
                    label = "tab_pill"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = 300f),
                    label = "tab_content_color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(pillBackground)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTabSelected(tab)
                        }
                        .testTag("tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = contentColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
