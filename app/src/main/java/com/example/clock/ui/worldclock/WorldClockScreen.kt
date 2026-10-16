package com.example.clock.ui.worldclock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clock.data.model.City
import com.example.clock.data.preferences.ClockFaceStyle
import com.example.clock.ui.components.AnalogClockView
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen(
    viewModel: WorldClockViewModel,
    onOpenSettings: () -> Unit
) {
    val rows by viewModel.clockRows.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val clockFaceStyle by viewModel.clockFaceStyle.collectAsStateWithLifecycle()
    val showAddSheet by viewModel.showAddCitySheet.collectAsStateWithLifecycle()
    val selectedDetailCity by viewModel.selectedDetailCity.collectAsStateWithLifecycle()
    val savedCities by viewModel.savedCities.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Top Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { viewModel.toggleEditMode() },
                    modifier = Modifier.testTag("world_clock_edit_button")
                ) {
                    Text(
                        text = if (isEditMode) "Done" else "Edit",
                        fontSize = 17.sp,
                        color = Color(0xFF0A84FF),
                        fontWeight = FontWeight.Normal
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sort Menu Button
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("world_clock_sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort cities",
                                tint = Color(0xFF0A84FF)
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            containerColor = Color(0xFF2C2C2E)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Manual Order", color = Color.White) },
                                onClick = {
                                    viewModel.setSortOrder(CitySortOrder.MANUAL)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Alphabetical", color = Color.White) },
                                onClick = {
                                    viewModel.setSortOrder(CitySortOrder.ALPHABETICAL)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Time Offset", color = Color.White) },
                                onClick = {
                                    viewModel.setSortOrder(CitySortOrder.TIME_OFFSET)
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    // Settings Button
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("world_clock_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF0A84FF)
                        )
                    }

                    // Plus Button
                    IconButton(
                        onClick = { viewModel.openAddCitySheet() },
                        modifier = Modifier.testTag("world_clock_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add City",
                            tint = Color(0xFF0A84FF),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Large Title
            Text(
                text = "World Clock",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Cities List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(rows, key = { it.cityId }) { row ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val city = savedCities.find { it.id == row.cityId }
                                    viewModel.selectDetailCity(city)
                                }
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Section: Edit delete button + day offset + city name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isEditMode && !row.isHome) {
                                    IconButton(
                                        onClick = { viewModel.deleteCity(row.cityId) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_city_${row.name.lowercase()}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RemoveCircle,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFFF453A)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (row.isHome) {
                                            Icon(
                                                imageVector = Icons.Default.Home,
                                                contentDescription = "Home",
                                                tint = Color(0xFF8E8E93),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = row.dayOffsetCaption,
                                            fontSize = 13.sp,
                                            color = Color(0xFF8E8E93)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = row.name,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            // Right Section: Digital time or mini Analog face
                            if (clockFaceStyle == ClockFaceStyle.ANALOG) {
                                val zone = runCatching { ZoneId.of(row.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
                                AnalogClockView(zoneId = zone, size = 52.dp, showSeconds = false)
                            } else {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = row.timeText,
                                        fontSize = 46.sp,
                                        fontWeight = FontWeight.Light,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (row.amPmText.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = row.amPmText,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Light,
                                            color = Color(0xFF8E8E93),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }

        // Add City Sheet
        if (showAddSheet) {
            AddCitySheet(
                onCitySelected = { viewModel.addCity(it) },
                onDismiss = { viewModel.closeAddCitySheet() }
            )
        }

        // Detail City Sheet
        selectedDetailCity?.let { city ->
            CityDetailSheet(
                city = city,
                onDismiss = { viewModel.selectDetailCity(null) }
            )
        }
    }
}
