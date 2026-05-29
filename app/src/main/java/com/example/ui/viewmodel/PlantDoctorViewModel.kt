package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ScanHistoryEntity
import com.example.data.model.PlantScanReport
import com.example.data.repository.PlantDoctorRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    object Idle : ScanUiState
    data class Loading(val message: String) : ScanUiState
    data class Success(val report: PlantScanReport) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class PlantDoctorViewModel(
    private val repository: PlantDoctorRepository
) : ViewModel() {

    private val _scanUiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanUiState: StateFlow<ScanUiState> = _scanUiState.asStateFlow()

    // Holds selected image for current scanning workspace
    private val _selectedImage = MutableStateFlow<Bitmap?>(null)
    val selectedImage: StateFlow<Bitmap?> = _selectedImage.asStateFlow()

    val historyState: StateFlow<List<ScanHistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // For viewing a historic report card in detail view modal
    private val _activeDetailReport = MutableStateFlow<PlantScanReport?>(null)
    val activeDetailReport: StateFlow<PlantScanReport?> = _activeDetailReport.asStateFlow()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val reportAdapter = moshi.adapter(PlantScanReport::class.java)

    fun selectImage(bitmap: Bitmap?) {
        _selectedImage.value = bitmap
        // Reset analysis state when a new image is chosen
        if (bitmap != null) {
            _scanUiState.value = ScanUiState.Idle
        }
    }

    fun startAnalysis() {
        val bitmap = _selectedImage.value ?: return
        
        viewModelScope.launch {
            _scanUiState.value = ScanUiState.Loading("Uploading image for analysis...")
            
            // Cycle visual simulation messages to provide dynamic premium feedback
            val job = launch {
                val progressMessages = listOf(
                    "Identifying plant genus and species...",
                    "Scanning foliage for anomalies & discoloration...",
                    "Analyzing leaf spot shapes and patterns...",
                    "Calculating diagnosis confidence...",
                    "Checking treatment databases for pesticide solutions...",
                    "Assembling organic alternatives & tips..."
                )
                for (msg in progressMessages) {
                    delay(2500)
                    if (_scanUiState.value is ScanUiState.Loading) {
                        _scanUiState.value = ScanUiState.Loading(msg)
                    } else {
                        break
                    }
                }
            }

            try {
                val report = repository.analyzePlantImage(bitmap)
                job.cancel() // Stop progress feedback loop
                _scanUiState.value = ScanUiState.Success(report)
            } catch (e: Exception) {
                job.cancel()
                _scanUiState.value = ScanUiState.Error(
                    e.localizedMessage ?: "An unexpected error occurred. Please check your internet connection."
                )
            }
        }
    }

    fun showHistoryDetail(entity: ScanHistoryEntity) {
        viewModelScope.launch {
            try {
                val report = reportAdapter.fromJson(entity.reportJson)
                _activeDetailReport.value = report
            } catch (e: Exception) {
                _activeDetailReport.value = null
            }
        }
    }

    fun dismissHistoryDetail() {
        _activeDetailReport.value = null
    }

    fun deleteHistoryId(id: Int) {
        viewModelScope.launch {
            repository.deleteScan(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun clearScanWorkspace() {
        _selectedImage.value = null
        _scanUiState.value = ScanUiState.Idle
    }
}

class PlantDoctorViewModelFactory(
    private val repository: PlantDoctorRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlantDoctorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlantDoctorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
