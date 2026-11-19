package com.scanpilot.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanpilot.app.billing.LocalBillingManager
import com.scanpilot.app.data.DocumentEntity
import com.scanpilot.app.data.FolderEntity
import com.scanpilot.app.data.SettingsStore
import com.scanpilot.app.repository.DocumentRepository
import com.scanpilot.app.repository.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State for short messages to the user (error or success) */
sealed interface UiMessage {
    data class Text(val text: String) : UiMessage
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository(application)
    private val settingsStore = SettingsStore(application)
    private val billing = LocalBillingManager(application)

    /** Show the rate prompt — only after 3 successful uses and only once. */
    val showRatePrompt: StateFlow<Boolean> =
        combine(settingsStore.successfulUses, settingsStore.ratePromptShown) { uses, shown ->
            uses >= 3 && !shown
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun dismissRatePrompt() {
        viewModelScope.launch { settingsStore.setRatePromptShown() }
    }

    val searchQuery = MutableStateFlow("")
    val sortOrder = MutableStateFlow(SortOrder.DATE)
    val selectedFolderId = MutableStateFlow<Long?>(null) // null means all folders
    val showFavoritesOnly = MutableStateFlow(false)

    /** Documents selected for merging (long press) */
    val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    /** Show the upgrade-to-Pro dialog (after tapping merge, or when hitting the scan cap). */
    val showProUpsell = MutableStateFlow(false)

    fun dismissProUpsell() { showProUpsell.value = false }

    /** Show the library as a grid or a list. */
    val isGridView = MutableStateFlow(false)

    fun toggleGridView() { isGridView.value = !isGridView.value }

    val folders: StateFlow<List<FolderEntity>> = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<DocumentEntity>> =
        combine(searchQuery, sortOrder, selectedFolderId, showFavoritesOnly) { query, sort, folder, favOnly ->
            QueryParams(query, sort, folder, favOnly)
        }.flatMapLatest { params ->
            repository.observeDocuments(params.query, params.sort)
        }.combine(selectedFolderId) { docs, folder -> docs to folder }
            .combine(showFavoritesOnly) { (docs, folder), favOnly ->
                docs.filter { doc ->
                    (folder == null || doc.folderId == folder) &&
                        (!favOnly || doc.isFavorite)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message

    fun clearMessage() { _message.value = null }

    fun toggleSelection(id: Long) {
        val current = selectedIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        selectedIds.value = current
    }

    fun clearSelection() { selectedIds.value = emptySet() }

    fun importPdf(uri: Uri) {
        viewModelScope.launch {
            repository.importPdf(uri)
                .onSuccess {
                    settingsStore.recordSuccessfulUse()
                    _message.value = UiMessage.Text("Document \u201C${it.fileName}\u201D was added.")
                }
                .onFailure { _message.value = UiMessage.Text(it.message ?: "Importing the document failed.") }
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            _message.value = UiMessage.Text("Document deleted.")
        }
    }

    fun renameDocument(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renameDocument(id, newName)
            _message.value = UiMessage.Text("Document renamed.")
        }
    }

    fun toggleFavorite(doc: DocumentEntity) {
        viewModelScope.launch { repository.toggleFavorite(doc) }
    }

    fun moveToFolder(id: Long, folderId: Long?) {
        viewModelScope.launch {
            repository.moveToFolder(id, folderId)
            _message.value = UiMessage.Text("Document moved.")
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val id = repository.createFolder(name)
            _message.value = if (id > 0) {
                UiMessage.Text("Folder \u201C$name\u201D was created.")
            } else {
                UiMessage.Text("Enter a folder name.")
            }
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            _message.value = UiMessage.Text("Folder deleted. Its documents moved back to \u201CAll\u201D.")
        }
    }

    fun mergeSelected() {
        val ids = selectedIds.value.toList()
        viewModelScope.launch {
            // Merging is a Pro feature
            if (!billing.isPro.first()) {
                showProUpsell.value = true
                _message.value = UiMessage.Text("Merging documents is a Pro feature. The free version includes up to 3 scans per day and unlimited reading and sharing.")
                return@launch
            }
            repository.mergeDocuments(ids, "Merged")
                .onSuccess {
                    selectedIds.value = emptySet()
                    _message.value = UiMessage.Text("Merged document created: \u201C${it.fileName}\u201D")
                }
                .onFailure {
                    _message.value = UiMessage.Text(it.message ?: "Merging failed.")
                }
        }
    }

    fun splitDocument(id: Long, fromPage: Int, toPage: Int) {
        viewModelScope.launch {
            repository.splitDocument(id, fromPage, toPage)
                .onSuccess {
                    settingsStore.recordSuccessfulUse()
                    _message.value = UiMessage.Text("Split document created: \u201C${it.fileName}\u201D")
                }
                .onFailure {
                    _message.value = UiMessage.Text(it.message ?: "Splitting failed.")
                }
        }
    }

    fun lockDocument(id: Long, password: String) {
        viewModelScope.launch {
            repository.lockDocument(id, password)
                .onSuccess {
                    settingsStore.recordSuccessfulUse()
                    _message.value = UiMessage.Text("Locked copy created: \u201C${it.fileName}\u201D \u2014 keep the password safe.")
                }
                .onFailure {
                    _message.value = UiMessage.Text(it.message ?: "Locking failed.")
                }
        }
    }

    fun exportPdfToImages(id: Long) {
        viewModelScope.launch {
            repository.exportImages(id)
                .onSuccess { files ->
                    val context = getApplication<Application>().applicationContext
                    val uris = files.map { file ->
                        androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    }
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "image/png"
                        putParcelableArrayListExtra(
                            android.content.Intent.EXTRA_STREAM,
                            ArrayList(uris)
                        )
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(
                        android.content.Intent.createChooser(intent, "Share images")
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    _message.value = UiMessage.Text("${files.size} images created.")
                }
                .onFailure {
                    _message.value = UiMessage.Text(it.message ?: "Converting to images failed.")
                }
        }
    }

    private data class QueryParams(
        val query: String,
        val sort: SortOrder,
        val folderId: Long?,
        val favOnly: Boolean
    )
}
