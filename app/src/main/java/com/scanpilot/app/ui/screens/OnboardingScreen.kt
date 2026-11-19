package com.scanpilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val PAGES = listOf(
    OnboardingPage(
        icon = Icons.Default.CameraAlt,
        title = "Scan documents in seconds",
        description = "Use your phone's camera to photograph a contract, lecture notes or a receipt and get a PDF right away."
    ),
    OnboardingPage(
        icon = Icons.Default.Folder,
        title = "All your documents, organised and offline",
        description = "Your documents stay on your own phone — no account, no internet, no VPN."
    ),
    OnboardingPage(
        icon = Icons.Default.Summarize,
        title = "Smart summaries, whenever you want",
        description = "If you like, get an AI summary of the document. Your file is only sent with your permission."
    )
)

/** First-run tutorial — 3 short pages, then straight to the library and the big scan button. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var pageIndex by remember { mutableStateOf(0) }
    val page = PAGES[pageIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                page.icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color(0xFF00695C)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            page.title,
            fontSize = 22.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            page.description,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Page dots
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PAGES.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == pageIndex) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pageIndex) Color(0xFF00695C) else Color(0xFFBDBDBD)
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (pageIndex < PAGES.size - 1) pageIndex++ else onFinish()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(if (pageIndex < PAGES.size - 1) "Next" else "Get started")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onFinish) {
            Text("Skip")
        }
    }
}
