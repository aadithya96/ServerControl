package com.servercontrol.presentation.metrics

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.MetricSample
import com.servercontrol.domain.usecase.ExportMetricsCsvUseCase
import com.servercontrol.domain.usecase.GetMetricsHistoryUseCase
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MetricsHistoryViewModel @Inject constructor(
    private val getMetricsHistoryUseCase: GetMetricsHistoryUseCase,
    private val exportMetricsCsvUseCase: ExportMetricsCsvUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serverId: Long = savedStateHandle["serverId"] ?: -1L
    val timeRange = MutableStateFlow(6)  // hours: 1/6/24/168

    val samples: StateFlow<Resource<List<MetricSample>>> = timeRange.flatMapLatest { hours ->
        getMetricsHistoryUseCase(serverId.toString(), hours)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading)

    fun setTimeRange(hours: Int) {
        timeRange.value = hours
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            exportMetricsCsvUseCase(serverId.toString(), timeRange.value).collect { result ->
                if (result is Resource.Success) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_TEXT, result.data)
                        putExtra(Intent.EXTRA_SUBJECT, "Metrics Export - Server $serverId")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Export Metrics CSV"))
                }
            }
        }
    }

    fun refresh() {
        // Trigger re-emission by toggling the value
        viewModelScope.launch {
            val current = timeRange.value
            timeRange.value = -1
            timeRange.value = current
        }
    }
}
