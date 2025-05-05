package com.example.traveltracker.data

import android.content.Context
import com.example.traveltracker.R
import kotlinx.serialization.json.Json
import java.io.IOException

class LocalCountryDataSource(private val context: Context) {

    // Gör JSON-parsern till en singleton/reusabel instans
    private val json = Json { ignoreUnknownKeys = true }

    // Cache för den statiska landlistan
    private val cachedCountries: List<StaticCountry> by lazy {
        loadCountriesFromJson()
    }

    fun getStaticCountries(): List<StaticCountry> {
        return cachedCountries
    }

    private fun loadCountriesFromJson(): List<StaticCountry> {
        return try {
            context.resources.openRawResource(R.raw.countries).use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString<List<StaticCountry>>(jsonString)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}