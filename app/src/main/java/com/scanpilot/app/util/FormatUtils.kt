package com.scanpilot.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Converts digits to Western digits (0123456789).
 *
 * NOTE: The function name `toWesternDigits` is kept unchanged for
 * compatibility with existing call sites. In this English build it no
 * longer converts to Persian digits — it returns Western digits
 * unchanged, i.e. it is effectively a no-op passthrough.
 */
fun toWesternDigits(text: String): String {
    return text
}

/** Formats a file size in a readable form (bytes / KB / MB) with Western digits. */
fun formatSize(bytes: Long): String {
    val raw = when {
        bytes < 1024 -> "$bytes bytes"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
        else -> "%.1f MB".format(bytes / (1024f * 1024f))
    }
    return toWesternDigits(raw)
}

/**
 * Formats a date using the Gregorian calendar with Western digits,
 * e.g. "19 Nov 2026".
 */
fun formatDate(timestamp: Long): String {
    val format = SimpleDateFormat("dd MMM yyyy", Locale.US)
    return toWesternDigits(format.format(Date(timestamp)))
}

/** Shares a PDF file with other apps (WhatsApp, Telegram, email, etc.). */
fun sharePdf(context: Context, file: File, fileName: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}
