package com.scanpilot.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanpilot.app.billing.LocalBillingManager
import com.scanpilot.app.data.AiSettings
import com.scanpilot.app.data.DocumentEntity
import com.scanpilot.app.data.SettingsStore
import com.scanpilot.app.network.AiSummaryService
import com.scanpilot.app.repository.DocumentRepository
import com.scanpilot.app.util.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface SummaryState {
    data object Idle : SummaryState
    data object Loading : SummaryState
    data class Success(val text: String) : SummaryState
    data class Error(val message: String) : SummaryState
}

class ViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository(application)
    private val settingsStore = SettingsStore(application)
    private val billing = LocalBillingManager(application)
    private val aiService = AiSummaryService()

    private val _document = MutableStateFlow<DocumentEntity?>(null)
    val document: StateFlow<DocumentEntity?> = _document

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    private val _pageBitmap = MutableStateFlow<Bitmap?>(null)
    val pageBitmap: StateFlow<Bitmap?> = _pageBitmap

    private val _summaryState = MutableStateFlow<SummaryState>(SummaryState.Idle)
    val summaryState: StateFlow<SummaryState> = _summaryState

    fun loadDocument(id: Long) {
        viewModelScope.launch {
            val doc = repository.getDocument(id) ?: return@launch
            _document.value = doc
            _currentPage.value = 0
            renderPage(0)
        }
    }

    fun goToPage(page: Int) {
        val doc = _document.value ?: return
        val target = page.coerceIn(0, (doc.pageCount - 1).coerceAtLeast(0))
        if (target == _currentPage.value && _pageBitmap.value != null) return
        _currentPage.value = target
        renderPage(target)
    }

    fun nextPage() = goToPage(_currentPage.value + 1)
    fun previousPage() = goToPage(_currentPage.value - 1)

    private fun renderPage(page: Int) {
        val doc = _document.value ?: return
        viewModelScope.launch {
            _pageBitmap.value = withContext(Dispatchers.IO) {
                PdfUtils.renderPage(File(doc.filePath), page, 1080)
            }
        }
    }

    fun toggleFavorite() {
        val doc = _document.value ?: return
        viewModelScope.launch {
            repository.toggleFavorite(doc)
            _document.value = repository.getDocument(doc.id)
        }
    }

    /**
     * Saves the current page's annotations: the page is flattened with
     * its annotations and a new "...-annotated" document is created in
     * the library. The original document is untouched.
     */
    fun saveAnnotations(
        pageIndex: Int,
        strokes: List<com.scanpilot.app.util.AnnotationStroke>,
        onSaved: () -> Unit
    ) {
        val doc = _document.value ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.saveAnnotatedPage(doc.id, pageIndex, strokes)
            }
            result.onSuccess {
                _summaryState.value = SummaryState.Idle
                onSaved()
            }.onFailure {
                _summaryState.value = SummaryState.Error("Saving the annotations failed.")
            }
        }
    }

    /** AI summary — a Pro feature that only works when a key has been entered in Settings. */
    fun requestSummary() {
        val doc = _document.value ?: return
        viewModelScope.launch {
            _summaryState.value = SummaryState.Loading
            if (!billing.isPro.first()) {
                _summaryState.value = SummaryState.Error(
                    "The AI summary is a Pro feature. The free version includes reading, scanning and sharing."
                )
                return@launch
            }
            val settings: AiSettings = settingsStore.settings.first()
            if (!settings.aiEnabled) {
                _summaryState.value = SummaryState.Error(
                    "The AI summary is turned off. Turn it on in Settings and enter your API key."
                )
                return@launch
            }
            if (settings.apiKey.isBlank()) {
                _summaryState.value = SummaryState.Error(
                    "No API key entered. Enter your API key in Settings."
                )
                return@launch
            }
            val text = withContext(Dispatchers.IO) {
                PdfUtils.extractText(File(doc.filePath))
            }
            val result = aiService.summarize(text, settings)
            _summaryState.value = result.fold(
                onSuccess = { SummaryState.Success(it) },
                onFailure = { SummaryState.Error(it.message ?: "Summarizing failed.") }
            )
        }
    }

    fun clearSummary() { _summaryState.value = SummaryState.Idle }
}
