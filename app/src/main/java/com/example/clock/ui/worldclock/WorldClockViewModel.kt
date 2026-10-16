package com.example.clock.ui.worldclock

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clock.data.citydata.CityDatabase
import com.example.clock.data.citydata.CityInfo
import com.example.clock.data.db.AppDatabase
import com.example.clock.data.model.City
import com.example.clock.data.preferences.ClockFaceStyle
import com.example.clock.data.preferences.ClockPreferences
import com.example.clock.data.suncalc.SunCalculator
import com.example.clock.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.TimeZone

data class WorldClockRow(
    val cityId: String,
    val name: String,
    val country: String,
    val timeZoneId: String,
    val latitude: Double,
    val longitude: Double,
    val isHome: Boolean,
    val timeText: String,
    val amPmText: String,
    val dayOffsetCaption: String,
    val offsetHours: Int
)

enum class CitySortOrder {
    MANUAL,
    ALPHABETICAL,
    TIME_OFFSET
}

class WorldClockViewModel(context: Context) : ViewModel() {

    private val db = AppDatabase.getInstance(context)
    private val prefs = ClockPreferences(context)
    private val appContext = context.applicationContext

    val clockFaceStyle: StateFlow<ClockFaceStyle> = prefs.clockFaceStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClockFaceStyle.DIGITAL)

    val is24Hour: StateFlow<Boolean> = prefs.is24HourFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _sortOrder = MutableStateFlow(CitySortOrder.MANUAL)
    val sortOrder: StateFlow<CitySortOrder> = _sortOrder.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _selectedDetailCity = MutableStateFlow<City?>(null)
    val selectedDetailCity: StateFlow<City?> = _selectedDetailCity.asStateFlow()

    private val _showAddCitySheet = MutableStateFlow(false)
    val showAddCitySheet: StateFlow<Boolean> = _showAddCitySheet.asStateFlow()

    init {
        // Initialize default cities if table is empty
        viewModelScope.launch {
            val existing = db.cityDao().getAllCitiesSync()
            if (existing.isEmpty()) {
                val homeZone = ZoneId.systemDefault().id
                val homeCity = City(
                    id = "$homeZone#Home",
                    name = "Current Location",
                    country = TimeZone.getDefault().displayName,
                    timeZoneId = homeZone,
                    latitude = 0.0,
                    longitude = 0.0,
                    isHome = true,
                    orderIndex = 0
                )
                val defaultList = mutableListOf(homeCity)
                CityDatabase.INITIAL_DEFAULT_CITIES.forEachIndexed { index, info ->
                    defaultList.add(
                        City(
                            id = info.id,
                            name = info.name,
                            country = info.country,
                            timeZoneId = info.timeZoneId,
                            latitude = info.latitude,
                            longitude = info.longitude,
                            isHome = false,
                            orderIndex = index + 1
                        )
                    )
                }
                db.cityDao().insertCities(defaultList)
            }
        }
    }

    val savedCities: StateFlow<List<City>> = db.cityDao().getAllCities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clockRows: StateFlow<List<WorldClockRow>> = combine(savedCities, is24Hour, _sortOrder) { cities, is24h, sort ->
        val homeZone = ZoneId.systemDefault()
        val homeNow = ZonedDateTime.now(homeZone)

        val rows = cities.map { city ->
            val cityZone = runCatching { ZoneId.of(city.timeZoneId) }.getOrDefault(homeZone)
            val cityNow = ZonedDateTime.now(cityZone)

            val timeFormatter = DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm")
            val amPmFormatter = DateTimeFormatter.ofPattern("a")

            val timeText = cityNow.format(timeFormatter)
            val amPmText = if (is24h) "" else cityNow.format(amPmFormatter).uppercase()

            // Calculate offset in hours relative to Home
            val homeOffsetSec = homeZone.rules.getOffset(homeNow.toInstant()).totalSeconds
            val cityOffsetSec = cityZone.rules.getOffset(cityNow.toInstant()).totalSeconds
            val diffSec = cityOffsetSec - homeOffsetSec
            val diffHours = diffSec / 3600

            // Day relationship: Yesterday, Today, Tomorrow
            val dayCaption = when {
                cityNow.toLocalDate().isBefore(homeNow.toLocalDate()) -> "Yesterday"
                cityNow.toLocalDate().isAfter(homeNow.toLocalDate()) -> "Tomorrow"
                else -> "Today"
            }

            val sign = if (diffHours >= 0) "+$diffHours" else "$diffHours"
            val offsetCaption = if (city.isHome) "Current Time" else "$dayCaption, ${sign}HRS"

            WorldClockRow(
                cityId = city.id,
                name = if (city.isHome) "Current Location" else city.name,
                country = city.country,
                timeZoneId = city.timeZoneId,
                latitude = city.latitude,
                longitude = city.longitude,
                isHome = city.isHome,
                timeText = timeText,
                amPmText = amPmText,
                dayOffsetCaption = offsetCaption,
                offsetHours = diffHours
            )
        }

        when (sort) {
            CitySortOrder.MANUAL -> rows
            CitySortOrder.ALPHABETICAL -> rows.sortedWith(compareBy({ !it.isHome }, { it.name }))
            CitySortOrder.TIME_OFFSET -> rows.sortedWith(compareBy({ !it.isHome }, { it.offsetHours }))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun setSortOrder(order: CitySortOrder) {
        _sortOrder.value = order
    }

    fun openAddCitySheet() {
        _showAddCitySheet.value = true
    }

    fun closeAddCitySheet() {
        _showAddCitySheet.value = false
    }

    fun addCity(cityInfo: CityInfo) {
        viewModelScope.launch {
            val count = db.cityDao().getAllCitiesSync().size
            val city = City(
                id = cityInfo.id,
                name = cityInfo.name,
                country = cityInfo.country,
                timeZoneId = cityInfo.timeZoneId,
                latitude = cityInfo.latitude,
                longitude = cityInfo.longitude,
                isHome = false,
                orderIndex = count
            )
            db.cityDao().insertCity(city)
            _showAddCitySheet.value = false
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun deleteCity(cityId: String) {
        viewModelScope.launch {
            db.cityDao().deleteCityById(cityId)
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun selectDetailCity(city: City?) {
        _selectedDetailCity.value = city
    }

    fun getCityDetails(city: City): Pair<LocalTime?, LocalTime?> {
        val zone = runCatching { ZoneId.of(city.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        return SunCalculator.calculateSunriseSunset(city.latitude, city.longitude, LocalDate.now(zone), zone)
    }
}
