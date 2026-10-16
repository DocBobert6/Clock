package com.example.clock.ui.settings

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clock.widget.*

data class WidgetGalleryItem(
    val title: String,
    val size: String,
    val description: String,
    val providerClass: Class<*>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetGallerySheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appWidgetManager = AppWidgetManager.getInstance(context)

    val widgetItems = listOf(
        WidgetGalleryItem("Digital Clock", "2x1, 4x1, 2x2", "Large clean time numerals and full date", DigitalClockWidgetProvider::class.java),
        WidgetGalleryItem("Analog Clock", "2x2, 3x3", "Minimalist analog clock face with blue second hand", AnalogClockWidgetProvider::class.java),
        WidgetGalleryItem("Clock & Date Combo", "4x2", "Digital time, date, and next alarm bell indicator", ClockDateWidgetProvider::class.java),
        WidgetGalleryItem("World Clock Small", "2x2", "Two cities with local times and day offsets", WorldClockSmallWidgetProvider::class.java),
        WidgetGalleryItem("World Clock Large", "4x2, 4x4", "Global cities list with local times and analog mini faces", WorldClockLargeWidgetProvider::class.java),
        WidgetGalleryItem("Next Alarm", "2x1, 4x1", "Upcoming alarm countdown and tap-to-open shortcut", NextAlarmWidgetProvider::class.java),
        WidgetGalleryItem("Alarm List", "4x2, 4x3", "Interactive scrollable list with direct on-widget toggles", AlarmListWidgetProvider::class.java),
        WidgetGalleryItem("Stopwatch", "2x2, 4x2", "Live chronometer with Start, Stop, and Lap directly from home screen", StopwatchWidgetProvider::class.java),
        WidgetGalleryItem("Timer", "2x2, 4x2", "Live countdown progress ring and quick preset triggers", TimerWidgetProvider::class.java),
        WidgetGalleryItem("Bedtime", "2x2, 4x1", "Sleep schedule dial, duration, and quick sleep ambiance button", BedtimeWidgetProvider::class.java),
        WidgetGalleryItem("Dashboard", "4x4", "All-in-one view: Digital clock, next alarm, world cities, and active timer", DashboardWidgetProvider::class.java)
    )

    fun pinWidget(providerClass: Class<*>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val component = ComponentName(context, providerClass)
                appWidgetManager.requestPinAppWidget(component, null, null)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add Widgets",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Tap Pin to add directly to Home Screen",
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_widget_gallery_button")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF8E8E93))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(widgetItems) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .testTag("widget_item_${item.title.lowercase().replace(" ", "_")}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.size,
                                        fontSize = 12.sp,
                                        color = Color(0xFF0A84FF),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.description,
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = { pinWidget(item.providerClass) },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0A84FF),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pin", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
