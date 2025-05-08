package com.example.traveltracker.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StaticCountry(
    val code: String, // ISO 3166-1 alpha-2 country code
    val name: String
    //val continent: String
)