package com.scanpilot.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream

/** Scan filters — applied to photos when saving a PDF. */
enum class ScanFilter {
    COLOR, GRAY, BLACK_WHITE
}

/**
 * PDF tools using built-in Android features:
 * - Viewing and thumbnails with PdfRenderer
 * - Creating PDFs from photos with PdfDocument
 * - Merging and splitting by re-rendering pages to images (the text layer is not preserved; explained in the README)
 * - Converting pages to PNG images
 * - Password locking with the open-source PDFBox-Android library (real PDF encryption)
 * - Text extraction only for the AI summary, with PDFBox-Android
 */
object PdfUtils {

    /** Number of pages in a PDF file; returns zero if the file is corrupt. */
    fun getPageCount(file: File): Int {
        if (!file.exists()) return 0
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer -> renderer.pageCount }
            }
        } catch (e: Exception) {
            0
        }
    }

    /** Renders one page to a bitmap at the given width (aspect ratio is preserved). */
    fun renderPage(file: File, pageIndex: Int, targetWidth: Int): Bitmap? {
        if (!file.exists()) return null
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
                    renderer.openPage(pageIndex).use { page ->
                        val scale = targetWidth.toFloat() / page.width
                        val width = targetWidth
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            // Page is too heavy (large PDF on a weak phone) — return null instead of crashing
            System.gc()
            null
        } catch (e: Exception) {
            null
        }
    }

    /** Thumbnail of the first page for the library list. */
    fun renderThumbnail(file: File): Bitmap? = renderPage(file, 0, 240)

    /** Rotates a bitmap by the given degrees (0, 90, 180 or 270). */
    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Applies a scan filter to a photo: color, grayscale, or black-and-white (high contrast). */
    fun applyFilter(bitmap: Bitmap, filter: ScanFilter): Bitmap {
        if (filter == ScanFilter.COLOR) return bitmap
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val grayMatrix = ColorMatrix().apply { setSaturation(0f) }
        if (filter == ScanFilter.GRAY) {
            paint.colorFilter = ColorMatrixColorFilter(grayMatrix)
        } else {
            // Black-and-white: grayscale + high contrast (not true binary; enough for document readability)
            val contrast = ColorMatrix(
                floatArrayOf(
                    2f, 0f, 0f, 0f, -140f,
                    0f, 2f, 0f, 0f, -140f,
                    0f, 0f, 2f, 0f, -140f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            val combined = ColorMatrix()
            combined.setConcat(contrast, grayMatrix)
            paint.colorFilter = ColorMatrixColorFilter(combined)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    /**
     * Creates a PDF from several bitmaps (e.g. scanned photos).
     * @param watermark if true, "Created with ScanPilot" is written at the
     * bottom of each page (free version). The Pro version saves without a watermark.
     */
    fun createPdfFromBitmaps(bitmaps: List<Bitmap>, outFile: File, watermark: Boolean = false): Boolean {
        if (bitmaps.isEmpty()) return false
        return try {
            val document = PdfDocument()
            for (bitmap in bitmaps) {
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                if (watermark) {
                    val paint = Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = (bitmap.width / 28f).coerceAtLeast(24f)
                        textAlign = Paint.Align.CENTER
                    }
                    page.canvas.drawText(
                        "Created with ScanPilot",
                        bitmap.width / 2f,
                        bitmap.height - 24f,
                        paint
                    )
                }
                document.finishPage(page)
            }
            FileOutputStream(outFile).use { document.writeTo(it) }
            document.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Merges several PDFs into one file.
     * Each page is re-rendered, so selectable text is not preserved in
     * the output file, but the pages look exactly like the originals.
     * This approach was chosen for an offline MVP with no dependencies.
     */
    fun mergePdfs(context: Context, sources: List<File>, outFile: File): Boolean {
        if (sources.isEmpty()) return false
        return try {
            val document = PdfDocument()
            var pageNumber = 1
            for (source in sources) {
                val count = getPageCount(source)
                for (i in 0 until count) {
                    val bitmap = renderPage(source, i, 1240) ?: continue
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pageNumber).create()
                    val page = document.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    document.finishPage(page)
                    pageNumber++
                    bitmap.recycle()
                }
            }
            FileOutputStream(outFile).use { document.writeTo(it) }
            document.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Splits a PDF: puts pages fromPage to toPage (numbered from 1) into a new file. */
    fun splitPdf(source: File, fromPage: Int, toPage: Int, outFile: File): Boolean {
        val count = getPageCount(source)
        if (count == 0) return false
        val from = fromPage.coerceIn(1, count)
        val to = toPage.coerceIn(from, count)
        return try {
            val document = PdfDocument()
            var pageNumber = 1
            for (i in (from - 1)..(to - 1)) {
                val bitmap = renderPage(source, i, 1240) ?: continue
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pageNumber).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(page)
                pageNumber++
                bitmap.recycle()
            }
            FileOutputStream(outFile).use { document.writeTo(it) }
            document.close()
            pageNumber > 1
        } catch (e: Exception) {
            false
        }
    }

    /** Converts all pages of a PDF to PNG image files in the given folder. */
    fun exportPagesToImages(source: File, outDir: File): List<File> {
        val result = mutableListOf<File>()
        val count = getPageCount(source)
        if (count == 0) return result
        outDir.mkdirs()
        val baseName = source.nameWithoutExtension
        for (i in 0 until count) {
            val bitmap = renderPage(source, i, 1080) ?: continue
            val outFile = File(outDir, "${baseName}_page_${i + 1}.png")
            try {
                FileOutputStream(outFile).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                result.add(outFile)
            } catch (e: Exception) {
                // This page is skipped; the rest continue
            } finally {
                bitmap.recycle()
            }
        }
        return result
    }

    /**
     * Locks a PDF with a password (real encryption with PDFBox-Android).
     * The output file cannot be opened without the password. The password
     * cannot be recovered — the user must keep it.
     */
    fun lockPdf(source: File, password: String, outFile: File): Boolean {
        if (!source.exists() || password.length < 4) return false
        return try {
            PDDocument.load(source).use { document ->
                val permission = AccessPermission()
                permission.setCanPrint(true)
                val policy = StandardProtectionPolicy(password, password, permission)
                policy.encryptionKeyLength = 128
                policy.setPreferAES(true)
                document.protect(policy)
                document.save(outFile)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Flattens annotations onto a page bitmap (for saving an annotated copy). */
    fun drawStrokesOnBitmap(pageBitmap: Bitmap, strokes: List<AnnotationStroke>): Bitmap {
        val result = pageBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        for (stroke in strokes) {
            val pts = stroke.points.map { p ->
                android.graphics.PointF(p.x * result.width, p.y * result.height)
            }
            if (pts.size < 2) continue
            when (stroke.tool) {
                AnnotationTool.HIGHLIGHT -> {
                    val minX = pts.minOf { it.x }
                    val maxX = pts.maxOf { it.x }
                    val minY = pts.minOf { it.y }
                    val maxY = pts.maxOf { it.y }
                    val paint = Paint().apply {
                        color = android.graphics.Color.YELLOW
                        alpha = 100
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(
                        minX, minY,
                        maxX.coerceAtLeast(minX + 20f),
                        maxY.coerceAtLeast(minY + 24f),
                        paint
                    )
                }
                AnnotationTool.UNDERLINE -> {
                    val y = pts.maxOf { it.y }
                    val paint = Paint().apply {
                        color = android.graphics.Color.RED
                        strokeWidth = 5f
                    }
                    canvas.drawLine(pts.minOf { it.x }, y, pts.maxOf { it.x }, y, paint)
                }
                AnnotationTool.STRIKE -> {
                    val y = (pts.minOf { it.y } + pts.maxOf { it.y }) / 2
                    val paint = Paint().apply {
                        color = android.graphics.Color.RED
                        strokeWidth = 5f
                    }
                    canvas.drawLine(pts.minOf { it.x }, y, pts.maxOf { it.x }, y, paint)
                }
                AnnotationTool.DRAW, AnnotationTool.SIGN -> {
                    val paint = Paint().apply {
                        color = stroke.colorArgb
                        strokeWidth = if (stroke.tool == AnnotationTool.SIGN) 5f else 9f
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        isAntiAlias = true
                    }
                    for (i in 0 until pts.size - 1) {
                        canvas.drawLine(pts[i].x, pts[i].y, pts[i + 1].x, pts[i + 1].y, paint)
                    }
                }
            }
        }
        return result
    }

    /**
     * Extracts PDF text for the AI summary.
     * If the PDF is scanned (images only, no text layer) an empty string
     * is returned and the UI shows an appropriate message. OCR will be
     * added in later versions.
     */
    fun extractText(file: File, maxChars: Int = 12000): String {
        if (!file.exists()) return ""
        return try {
            PDDocument.load(file).use { document ->
                val stripper = PDFTextStripper()
                stripper.startPage = 1
                stripper.endPage = minOf(document.numberOfPages, 20)
                stripper.getText(document).trim().take(maxChars)
            }
        } catch (e: Exception) {
            ""
        }
    }

    /** Reads a photo bitmap from a Uri, downscaled so phone memory does not fill up. */
    fun loadBitmapScaled(context: Context, uri: Uri, maxSize: Int = 2000): Bitmap? {
        return try {
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, options)
            }
            var sample = 1
            val largest = maxOf(options.outWidth, options.outHeight)
            while (largest / sample > maxSize) sample *= 2
            val decodeOptions = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } catch (e: Exception) {
            null
        }
    }
}
