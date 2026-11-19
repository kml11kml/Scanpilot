package com.scanpilot.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Support and privacy page.
 * TODO: Replace the support email with your real email.
 */
const val SUPPORT_EMAIL_PLACEHOLDER = "support@example.com"

fun openPlayStoreRating(context: android.content.Context) {
    val packageName = context.packageName
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        )
    } catch (e: Exception) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support") },
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
            Text("Contact support")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "If you have a problem or a suggestion, email us:",
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(SUPPORT_EMAIL_PLACEHOLDER)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$SUPPORT_EMAIL_PLACEHOLDER")
                        putExtra(Intent.EXTRA_SUBJECT, "ScanPilot support")
                    }
                    context.startActivity(Intent.createChooser(intent, "Send email"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Email support")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Rate the app")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "If ScanPilot was useful to you, please rate it on Google Play; " +
                    "your rating helps others find the app.",
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { openPlayStoreRating(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Rate it")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Privacy")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Your files stay on your own phone and are never sent to any server without your permission.\n" +
                    "Only when you tap 'AI Summary' is the text of that document sent to the service you chose in Settings" +
                    ".\n" +
                    "ScanPilot has no ads and does not track your behaviour.",
                color = Color.Gray
            )
        }
    }
}
