package com.scanpilot.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanpilot.app.billing.LocalBillingManager
import com.scanpilot.app.data.SettingsStore
import com.scanpilot.app.repository.DocumentRepository
import com.scanpilot.app.util.PdfUtils
import com.scanpilot.app.util.ScanFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One scanned photo with the user's chosen rotation */
data class ScanPhoto(
    val uri: Uri,
    val rotation: Int = 0
)

/** Free scan cap per day (Pro is unlimited). */
const val FREE_SCANS_PER_DAY = 3

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository(application)
    private val settingsStore = SettingsStore(application)
    private val billing = LocalBillingManager(application)

    private val _photos = MutableStateFlow<List<ScanPhoto>>(emptyList())
    val photos: StateFlow<List<ScanPhoto>> = _photos

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    /** Show the upgrade dialog when the 3 free scans per day are used up. */
    val showProUpsell = MutableStateFlow(false)

    fun dismissProUpsell() { showProUpsell.value = false }

    fun clearMessage() { _message.value = null }

    fun addPhoto(uri: Uri) {
        _photos.value = _photos.value + ScanPhoto(uri)
    }

    fun removePhoto(index: Int) {
        _photos.value = _photos.value.toMutableList().also { it.removeAt(index) }
    }

    fun movePhotoUp(index: Int) {
        if (index <= 0) return
        val list = _photos.value.toMutableList()
        val item = list.removeAt(index)
        list.add(index - 1, item)
        _photos.value = list
    }

    fun movePhotoDown(index: Int) {
        val list = _photos.value.toMutableList()
        if (index >= list.size - 1) return
        val item = list.removeAt(index)
        list.add(index + 1, item)
        _photos.value = list
    }

    fun rotatePhoto(index: Int) {
        val list = _photos.value.toMutableList()
        val photo = list[index]
        list[index] = photo.copy(rotation = (photo.rotation + 90) % 360)
        _photos.value = list
    }

    /** Creates a PDF from the photos and saves it to the library. On success, the photo list is cleared. */
    fun saveAsPdf(name: String, filter: ScanFilter, onSaved: () -> Unit) {
        val photos = _photos.value
        if (photos.isEmpty()) {
            _message.value = "Take at least one photo first."
            return
        }
        viewModelScope.launch {
            // Free-version cap: 3 scans per day; Pro is unlimited
            val isPro = billing.isPro.first()
            val scansToday = settingsStore.scansToday.first()
            if (!isPro && scansToday >= FREE_SCANS_PER_DAY) {
                showProUpsell.value = true
                _message.value = "You have used today's 3 free scans. You can scan again tomorrow or buy the Pro version."
                return@launch
            }
            _isSaving.value = true
            val bitmaps = withContext(Dispatchers.IO) {
                photos.mapNotNull { photo ->
                    val context = getApplication<Application>().applicationContext
                    PdfUtils.loadBitmapScaled(context, photo.uri)?.let { bitmap ->
                        val rotated = PdfUtils.rotateBitmap(bitmap, photo.rotation)
                        PdfUtils.applyFilter(rotated, filter)
                    }
                }
            }
            if (bitmaps.isEmpty()) {
                _isSaving.value = false
                _message.value = "Reading the photos failed."
                return@launch
            }
            // Free version: pages carry a "Created with ScanPilot" watermark at the bottom
            repository.createPdfFromPhotos(bitmaps, name, watermark = !isPro)
                .onSuccess {
                    bitmaps.forEach { it.recycle() }
                    settingsStore.recordScan()
                    settingsStore.recordSuccessfulUse()
                    _photos.value = emptyList()
                    _isSaving.value = false
                    _message.value = "Document \u201C${it.fileName}\u201D was saved to the library."
                    onSaved()
                }
                .onFailure {
                    bitmaps.forEach { it.recycle() }
                    _isSaving.value = false
                    _message.value = it.message ?: "Creating the PDF failed."
                }
        }
    }
}
