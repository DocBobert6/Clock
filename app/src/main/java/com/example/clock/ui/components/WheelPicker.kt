package com.example.clock.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleItemsCount = 5
    val itemHeight = 44.dp
    // Wrap around effectively with repeated virtual list
    val repeatCount = 20
    val totalCount = items.size * repeatCount
    val middleOffset = (repeatCount / 2) * items.size
    val startIndex = middleOffset + (initialIndex % items.size)

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex - (visibleItemsCount / 2))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val view = LocalView.current

    val centerIndex by remember {
        derivedStateOf {
            val visibleCenter = listState.firstVisibleItemIndex + (visibleItemsCount / 2)
            visibleCenter
        }
    }

    var lastReportedIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(centerIndex) {
        val selectedItem = centerIndex % items.size
        if (selectedItem != lastReportedIndex) {
            lastReportedIndex = selectedItem
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onItemSelected(selectedItem)
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .widthIn(min = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection highlight band
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                )
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(totalCount) { index ->
                val actualItem = items[index % items.size]
                val distance = abs(index - centerIndex)
                val alpha = when (distance) {
                    0 -> 1.0f
                    1 -> 0.5f
                    2 -> 0.25f
                    else -> 0.1f
                }
                val scale = when (distance) {
                    0 -> 24.sp
                    1 -> 20.sp
                    else -> 16.sp
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = actualItem,
                        fontSize = scale,
                        fontWeight = if (distance == 0) FontWeight.Normal else FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alpha(alpha)
                    )
                }
            }
        }
    }
}
