package com.example.traveltracker.viewmodel

import com.example.traveltracker.data.StatisticsRepository // Din Statistik Repository
import com.example.traveltracker.data.StatisticsData // Din Statistik Data Class
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StatisticsViewModel(
    private val statisticsRepository: StatisticsRepository // Injicera Statistik Repository
    // Du kan även injicera FirebaseAuth här om ViewModel behöver det direkt
) : ViewModel() {

    // State som UI:t observerar
    private val _statisticsUiState = MutableStateFlow(StatisticsData(isLoading = true))
    val statisticsUiState: StateFlow<StatisticsData> = _statisticsUiState.asStateFlow()

    init {
        // Starta coroutine för att hämta statistikdata via repositoryn
        viewModelScope.launch {
            statisticsRepository.getStatisticsData()
                // Repositoryn hanterar redan error.catch(), men du kan lägga till mer här om du vill
                .collect { statisticsData ->
                    _statisticsUiState.value = statisticsData // Uppdatera state när ny data kommer
                }
        }
    }
    // Du kan lägga till reload funktion här om du vill kunna manuellt ladda om
    // fun reloadStatistics() { ... starta om Flow-kollektionen ... }
}
