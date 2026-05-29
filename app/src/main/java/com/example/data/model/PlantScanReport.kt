package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlantScanReport(
    val plantName: String,
    val status: String, // "Healthy", "Diseased", etc.
    val diseaseName: String, // "None" if healthy, or name of disease
    val confidence: Double, // 0.0 to 1.0 or percentage
    val symptoms: List<String>,
    val cause: String,
    val pesticideRecommendations: List<PesticideRecommendation>,
    val preventionTips: List<String>
)

@JsonClass(generateAdapter = true)
data class PesticideRecommendation(
    val chemicalName: String,
    val pesticideType: String,
    val instructions: String,
    val organicAlternative: String
)
