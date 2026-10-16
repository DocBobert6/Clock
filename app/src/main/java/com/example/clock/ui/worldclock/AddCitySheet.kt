package com.example.clock.ui.worldclock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clock.data.citydata.CityDatabase
import com.example.clock.data.citydata.CityInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCitySheet(
    onCitySelected: (CityInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            CityDatabase.CITIES
        } else {
            CityDatabase.CITIES.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.country.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val alphabet = ('A'..'Z').toList()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Choose a City",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_add_city_button")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF8E8E93))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("city_search_input"),
                placeholder = { Text("Search city or country", color = Color(0xFF8E8E93)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8E8E93)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0A84FF),
                    unfocusedBorderColor = Color(0xFF38383A),
                    focusedContainerColor = Color(0xFF2C2C2E),
                    unfocusedContainerColor = Color(0xFF2C2C2E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // City list with alphabet scrubber
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    items(filteredCities, key = { it.id }) { city ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCitySelected(city) }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                                .testTag("city_item_${city.name.lowercase().replace(" ", "_")}")
                        ) {
                            Text(
                                text = city.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                            Text(
                                text = "${city.country} • ${city.timeZoneId}",
                                fontSize = 13.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                        HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
                    }
                }

                // Alphabet Index Scrubber
                if (searchQuery.isBlank()) {
                    Column(
                        modifier = Modifier
                            .width(24.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        alphabet.forEach { letter ->
                            Text(
                                text = letter.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0A84FF),
                                modifier = Modifier
                                    .clickable {
                                        val targetIdx = filteredCities.indexOfFirst {
                                            it.name.startsWith(letter, ignoreCase = true)
                                        }
                                        if (targetIdx >= 0) {
                                            coroutineScope.launch {
                                                listState.scrollToItem(targetIdx)
                                            }
                                        }
                                    }
                                    .padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
