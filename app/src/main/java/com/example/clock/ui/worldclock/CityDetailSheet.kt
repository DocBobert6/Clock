package com.example.clock.ui.worldclock

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clock.data.model.City
import com.example.clock.data.suncalc.SunCalculator
import com.example.clock.ui.components.AnalogClockView
import com.example.clock.ui.components.SunArcDisplay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityDetailSheet(
    city: City,
    onDismiss: () -> Unit
) {
    val zone = runCatching { ZoneId.of(city.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
    var currentCityTime by remember { mutableStateOf(ZonedDateTime.now(zone)) }

    LaunchedEffect(city) {
        while (true) {
            currentCityTime = ZonedDateTime.now(zone)
            kotlinx.coroutines.delay(1000L)
        }
    }

    val (sunrise, sunset) = remember(city) {
        SunCalculator.calculateSunriseSunset(city.latitude, city.longitude, LocalDate.now(zone), zone)
    }

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (city.isHome) "Current Location" else city.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = city.country,
                        fontSize = 14.sp,
                        color = Color(0xFF8E8E93)
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_city_detail_button")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF8E8E93))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Analog Clock View
            AnalogClockView(
                zoneId = zone,
                size = 190.dp,
                showSeconds = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Digital Time Display
            Text(
                text = currentCityTime.format(timeFormatter),
                fontSize = 42.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )

            Text(
                text = currentCityTime.format(dateFormatter),
                fontSize = 15.sp,
                color = Color(0xFF8E8E93)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sun Arc Display
            SunArcDisplay(
                sunrise = sunrise,
                sunset = sunset,
                currentLocalTime = currentCityTime.toLocalTime()
            )
        }
    }
}
