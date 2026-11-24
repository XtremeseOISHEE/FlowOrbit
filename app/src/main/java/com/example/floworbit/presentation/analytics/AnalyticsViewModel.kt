package com.example.floworbit.presentation.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.floworbit.data.blockedapps.BlockedApp
import com.example.floworbit.data.blockedapps.BlockedAppsRepository
import com.example.floworbit.data.violationlog.ViolationLog
import com.example.floworbit.data.violationlog.ViolationLogRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AnalyticsRange(val label: String, val days: Int) {
    TODAY("Today", 1),
    WEEK("7 days", 7),
    MONTH("30 days", 30),
}

data class AnalyticsBar(val name: String, val packageName: String, val count: Int, val isLead: Boolean)

data class AnalyticsUiState(
    val range: AnalyticsRange = AnalyticsRange.WEEK,
    val bars: List<AnalyticsBar> = emptyList(),
    val axisMax: Int = 1,
    val totalCount: Int = 0,
    val deltaPercent: Int? = null,
    val worstHourLabel: String = "—",
    val isEmpty: Boolean = true
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val logRepo = ViolationLogRepository(application)
    private val blockedAppsRepository = BlockedAppsRepository(application)

    private val _range = MutableStateFlow(AnalyticsRange.WEEK)
    private val _allLogs = MutableStateFlow<List<ViolationLog>>(emptyList())

    val uiState: StateFlow<AnalyticsUiState> = combine(_allLogs, _range) { logs, range ->
        buildState(logs, range)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    init {
        viewModelScope.launch {
            logRepo.allLogs().collect { _allLogs.value = it }
        }
    }

    fun setRange(range: AnalyticsRange) {
        _range.value = range
    }

    /** Blocks whichever apps are currently shown as the top offenders. */
    fun blockTopApps() {
        val bars = uiState.value.bars
        viewModelScope.launch {
            bars.forEach { blockedAppsRepository.addBlockedApp(BlockedApp(it.packageName, it.name)) }
        }
    }

    private fun buildState(logs: List<ViolationLog>, range: AnalyticsRange): AnalyticsUiState {
        val now = System.currentTimeMillis()
        val rangeStart = rangeStartMillis(range, now)
        val periodLength = now - rangeStart
        val prevStart = rangeStart - periodLength

        val currentLogs = logs.filter { it.timestamp in rangeStart..now }
        val previousLogs = logs.filter { it.timestamp in prevStart until rangeStart }

        val bars = currentLogs
            .filter { !it.label.isNullOrBlank() }
            .groupBy { it.packageName to it.label!! }
            .map { (key, entries) -> Triple(key.second, key.first, entries.size) }
            .sortedByDescending { it.third }
            .take(4)
            .let { top ->
                val leadCount = top.maxOfOrNull { it.third } ?: 0
                top.map { (name, pkg, count) -> AnalyticsBar(name, pkg, count, isLead = count == leadCount) }
            }

        val totalCount = bars.sumOf { it.count }
        val axisMax = (bars.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)

        val deltaPercent = if (previousLogs.isNotEmpty()) {
            (((currentLogs.size - previousLogs.size).toFloat() / previousLogs.size) * 100).toInt()
        } else null

        val worstHourLabel = worstHour(currentLogs)

        return AnalyticsUiState(
            range = range,
            bars = bars,
            axisMax = axisMax,
            totalCount = totalCount,
            deltaPercent = deltaPercent,
            worstHourLabel = worstHourLabel,
            isEmpty = bars.isEmpty()
        )
    }

    private fun rangeStartMillis(range: AnalyticsRange, now: Long): Long {
        return when (range) {
            AnalyticsRange.TODAY -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            else -> now - range.days * 24L * 60L * 60L * 1000L
        }
    }

    private fun worstHour(logs: List<ViolationLog>): String {
        if (logs.isEmpty()) return "—"
        val cal = Calendar.getInstance()
        val counts = IntArray(24)
        for (log in logs) {
            cal.timeInMillis = log.timestamp
            counts[cal.get(Calendar.HOUR_OF_DAY)]++
        }
        val h1 = counts.indices.maxByOrNull { counts[it] } ?: return "—"
        val h2 = (h1 + 1) % 24
        val suffix1 = if (h1 < 12) "am" else "pm"
        val suffix2 = if (h2 < 12) "am" else "pm"
        return if (suffix1 == suffix2) {
            "${displayHour(h1)}–${displayHour(h2)} $suffix1"
        } else {
            "${displayHour(h1)} $suffix1–${displayHour(h2)} $suffix2"
        }
    }

    private fun displayHour(hour: Int): Int = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
}
