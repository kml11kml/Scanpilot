package com.scanpilot.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanpilot.app.billing.LocalBillingManager
import com.scanpilot.app.data.AiSettings
import com.scanpilot.app.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore(application)
    private val billing = LocalBillingManager(application)

    val settings: StateFlow<AiSettings> = store.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiSettings())

    val isPro: StateFlow<Boolean> = billing.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _savedMessage = MutableStateFlow(false)
    val savedMessage: StateFlow<Boolean> = _savedMessage

    private val _purchaseMessage = MutableStateFlow<String?>(null)
    val purchaseMessage: StateFlow<String?> = _purchaseMessage

    fun save(settings: AiSettings) {
        viewModelScope.launch {
            store.save(settings)
            _savedMessage.value = true
        }
    }

    fun clearSavedMessage() { _savedMessage.value = false }

    /** Buy the Pro version — currently a stub that gives an honest message (see BillingManager). */
    fun purchasePro() {
        viewModelScope.launch {
            billing.purchasePro()
                .onFailure { _purchaseMessage.value = it.message ?: "Purchase failed." }
        }
    }

    fun clearPurchaseMessage() { _purchaseMessage.value = null }
}
