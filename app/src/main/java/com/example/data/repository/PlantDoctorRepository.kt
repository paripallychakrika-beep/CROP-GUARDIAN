package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.ScanHistoryDao
import com.example.data.local.ScanHistoryEntity
import com.example.data.model.PlantScanReport
import com.example.data.network.Content
import com.example.data.network.GenerateContentRequest
import com.example.data.network.GenerationConfig
import com.example.data.network.InlineData
import com.example.data.network.Part
import com.example.data.network.RetrofitClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class PlantDoctorRepository(
    private val context: Context,
    private val scanHistoryDao: ScanHistoryDao
) {
    val allHistory: Flow<List<ScanHistoryEntity>> = scanHistoryDao.getAllHistory()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val reportAdapter = moshi.adapter(PlantScanReport::class.java)

    suspend fun analyzePlantImage(bitmap: Bitmap): PlantScanReport = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API Key is empty. Please configure it in your AI Studio Secrets panel.")
        }

        // Compress bitmap to keep base64 payload size reasonable and improve network speed
        val scaledBitmap = scaleBitmapIfNeeded(bitmap, 1024)
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val imageBytes = outputStream.toByteArray()
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val prompt = """
            Analyze the plant or leaf in this image. 
            Identify the plant type.
            Determine if the plant is healthy or has a disease. If healthy, set status to "Healthy" and diseaseName to "None".
            If diseased, identify the exact disease, provide confidence level, describe the visible symptoms, identify the cause, and offer tailored chemical pesticide recommendations (with instructions) as well as organic alternatives, and prevention tips.
            
            Return your response in the following strict JSON schema:
            {
              "plantName": "String representing the name of the plant",
              "status": "Healthy" or "Diseased",
              "diseaseName": "Name of the disease, or 'None' if healthy",
              "confidence": Float representing confidence (0.0 to 1.0),
              "symptoms": ["List of identified symptoms"],
              "cause": "Underlying cause (fungal name, insect name, nutrient deficiency, etc.)",
              "pesticideRecommendations": [
                {
                  "chemicalName": "Name of recommended chemical pesticide, or 'None required' if healthy",
                  "pesticideType": "Fungicide / Insecticide / Miticide / Herbicide / etc.",
                  "instructions": "Dosage, application frequency, safety measures",
                  "organicAlternative": "Organic/natural alternative treatment like neem oil, copper, baking soda, insecticidal soap, etc."
                }
              ],
              "preventionTips": ["List of tips to prevent this disease in the future"]
            }
            
            Your response MUST only contain the raw JSON string. Do not wrap it in ```json blocks or any markdown formatting.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Received an empty response from Gemini AI. Please try another photo.")

            val cleanedJson = cleanJsonString(rawJson)
            Log.d("PlantDoctor", "Received JSON: $cleanedJson")

            val report = reportAdapter.fromJson(cleanedJson)
                ?: throw IllegalStateException("Failed to parse the Gemini AI analysis result.")

            // Save image locally to app's secure files directory to persist history gracefully
            val localImagePath = saveImageToInternalStorage(scaledBitmap)

            // Save to database
            val entity = ScanHistoryEntity(
                imageUriPath = localImagePath,
                plantName = report.plantName,
                diseaseName = report.diseaseName,
                status = report.status,
                reportJson = cleanedJson
            )
            scanHistoryDao.insertScan(entity)

            report
        } catch (e: Exception) {
            Log.e("PlantDoctor", "Error during plant analysis", e)
            throw e
        }
    }

    suspend fun deleteScan(id: Int) = withContext(Dispatchers.IO) {
        scanHistoryDao.deleteScanById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        scanHistoryDao.clearAllHistory()
    }

    private fun scaleBitmapIfNeeded(source: Bitmap, maxDimension: Int): Bitmap {
        if (source.width <= maxDimension && source.height <= maxDimension) {
            return source
        }
        val aspectRatio = source.width.toFloat() / source.height.toFloat()
        val (width, height) = if (aspectRatio > 1) {
            Pair(maxDimension, (maxDimension / aspectRatio).toInt())
        } else {
            Pair((maxDimension * aspectRatio).toInt(), maxDimension)
        }
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    private fun cleanJsonString(raw: String): String {
        return raw.replace("```json", "")
            .replace("```", "")
            .trim()
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): String {
        val fileName = "scan_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            Log.e("PlantDoctor", "Error saving image locally", e)
            ""
        }
    }
}
