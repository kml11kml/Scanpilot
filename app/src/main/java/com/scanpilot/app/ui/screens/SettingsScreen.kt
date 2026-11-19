package com.scanpilot.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.scanpilot.app.data.AiSettings
import com.scanpilot.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenSupport: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val savedMessage by viewModel.savedMessage.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val purchaseMessage by viewModel.purchaseMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var baseUrl by remember(settings) { mutableStateOf(settings.baseUrl) }
    var model by remember(settings) { mutableStateOf(settings.model) }
    var apiKey by remember(settings) { mutableStateOf(settings.apiKey) }
    var aiEnabled by remember(settings) { mutableStateOf(settings.aiEnabled) }

    LaunchedEffect(savedMessage) {
        if (savedMessage) {
            snackbarHostState.showSnackbar("Settings saved.")
            viewModel.clearSavedMessage()
        }
    }

    LaunchedEffect(purchaseMessage) {
        purchaseMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPurchaseMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("AI Summary (optional)")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Summary only works once you have entered your own API key. " +
                    "The key is stored only on your phone and is never sent to anyone else.",
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))

            RowSwitch(
                label = "Enable AI summary",
                checked = aiEnabled,
                onCheckedChange = { aiEnabled = it }
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Service address (Base URL)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Model") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your API key") },
                placeholder = { Text("Enter your key here — it is not in the app code") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.save(
                        AiSettings(
                            baseUrl = baseUrl,
                            model = model,
                            apiKey = apiKey,
                            aiEnabled = aiEnabled
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Save settings")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Pro version")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (isPro) {
                    "You have the Pro version: unlimited scans, merging and AI summary with no watermark."
                } else {
                    "Free version: unlimited reading, importing and sharing, 3 scans per day.\n" +
                        "Pro version: unlimited scans, document merging, AI summary and no watermark."
                },
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (!isPro) {
                Button(
                    onClick = { viewModel.purchasePro() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Buy Pro")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Payment is made through Google Play. The payment connection is not complete yet; " +
                        "The price is set in Google Play Console, not in this app.",
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onOpenSupport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Support and rating")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("About the app")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "ScanPilot version 1.0.0\n" +
                    "PDF reader and scanner, fully offline.\n" +
                    "Your files stay on your own phone; only when you tap 'AI Summary' " +
                    "is the document text sent to the service you chose above.",
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun RowSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
