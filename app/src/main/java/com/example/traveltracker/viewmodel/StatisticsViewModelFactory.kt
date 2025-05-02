package com.example.traveltracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.traveltracker.data.StatisticsRepository

class StatisticsViewModelFactory(
    private val statisticsRepository: StatisticsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            return StatisticsViewModel(statisticsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}