// package com.example.traveltracker.data

import android.content.Context
import com.example.traveltracker.R
import com.example.traveltracker.data.StaticCountry
import kotlinx.serialization.json.Json
import java.io.IOException

// Data class som mappar till objekten i din JSON
// @Serializable har vi redan definierat i förra steget.
// data class StaticCountry(val code: String, val name: String)

class LocalCountryDataSource(private val context: Context) {

    // Gör JSON-parsern till en singleton/reusabel instans
    private val json = Json { ignoreUnknownKeys = true }

    // Cache för den statiska landlistan
    private val cachedCountries: List<StaticCountry> by lazy { // Använd lazy för att ladda bara en gång
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