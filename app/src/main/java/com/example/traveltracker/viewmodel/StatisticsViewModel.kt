package com.example.traveltracker.viewmodel

import com.example.traveltracker.data.StatisticsRepository
import com.example.traveltracker.data.model.StatisticsData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StatisticsViewModel(
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {

    private val _statisticsUiState = MutableStateFlow(StatisticsData(isLoading = true))
    val statisticsUiState: StateFlow<StatisticsData> = _statisticsUiState.asStateFlow()

    init {
        viewModelScope.launch {
            statisticsRepository.getStatisticsData()
                .collect { statisticsData ->
                    _statisticsUiState.value = statisticsData
                }
        }
    }
}
