package com.scanpilot.app.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import com.scanpilot.app.data.AppDatabase
import com.scanpilot.app.data.DocumentEntity
import com.scanpilot.app.data.FolderEntity
import com.scanpilot.app.util.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** How the library list is sorted */
enum class SortOrder { DATE, NAME, SIZE }

/**
 * Document repository: all file and database work happens here.
 * Imported files are copied into the app's private storage so they stay available.
 */
class DocumentRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val documentDao = db.documentDao()
    private val folderDao = db.folderDao()

    private val documentsDir: File
        get() = File(appContext.filesDir, "documents").apply { if (!exists()) mkdirs() }

    companion object {
        /** Maximum imported file size — protects weak phones from crashing. */
        const val MAX_IMPORT_BYTES = 200L * 1024 * 1024
    }

    fun observeDocuments(query: String, sort: SortOrder): Flow<List<DocumentEntity>> {
        return when (sort) {
            SortOrder.DATE -> documentDao.observeByDate(query)
            SortOrder.NAME -> documentDao.observeByName(query)
            SortOrder.SIZE -> documentDao.observeBySize(query)
        }
    }

    fun observeFolders(): Flow<List<FolderEntity>> = folderDao.observeAll()

    suspend fun getDocument(id: Long): DocumentEntity? = documentDao.getById(id)

    fun fileOf(doc: DocumentEntity): File = File(doc.filePath)

    /** Imports a PDF from the phone's storage (via SAF). */
    suspend fun importPdf(uri: Uri): Result<DocumentEntity> = withContext(Dispatchers.IO) {
        try {
            val displayName = queryDisplayName(uri) ?: "Document-${System.currentTimeMillis()}.pdf"
            val safeName = displayName.takeIf { it.endsWith(".pdf", ignoreCase = true) }
                ?: "$displayName.pdf"
            val target = File(documentsDir, "${System.currentTimeMillis()}_$safeName")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            } ?: return@withContext Result.failure(RuntimeException("The file could not be read."))
            // Protects weak phones: very large files are not imported
            if (target.length() > MAX_IMPORT_BYTES) {
                target.delete()
                return@withContext Result.failure(
                    RuntimeException("This file is too large (over 200 MB) and will not open on ordinary phones.")
                )
            }
            val entity = DocumentEntity(
                fileName = safeName,
                filePath = target.absolutePath,
                sizeBytes = target.length(),
                dateAdded = System.currentTimeMillis(),
                pageCount = PdfUtils.getPageCount(target)
            )
            val id = documentDao.insert(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Result.failure(RuntimeException("Importing the document failed."))
        }
    }

    /** Creates a new PDF from scanned photos (with each photo's rotation applied). */
    suspend fun createPdfFromPhotos(
        bitmaps: List<Bitmap>,
        name: String,
        watermark: Boolean = false
    ): Result<DocumentEntity> = withContext(Dispatchers.IO) {
        try {
            val safeName = name.trim().takeIf { it.isNotEmpty() } ?: "Scan-${System.currentTimeMillis()}"
            val fileName = if (safeName.endsWith(".pdf", ignoreCase = true)) safeName else "$safeName.pdf"
            val target = File(documentsDir, "${System.currentTimeMillis()}_$fileName")
            val ok = PdfUtils.createPdfFromBitmaps(bitmaps, target, watermark)
            if (!ok) return@withContext Result.failure(RuntimeException("Creating the PDF failed."))
            val entity = DocumentEntity(
                fileName = fileName,
                filePath = target.absolutePath,
                sizeBytes = target.length(),
                dateAdded = System.currentTimeMillis(),
                pageCount = PdfUtils.getPageCount(target)
            )
            val id = documentDao.insert(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Result.failure(RuntimeException("Creating the PDF failed."))
        }
    }

    /** Merges several documents into a new PDF. */
    suspend fun mergeDocuments(ids: List<Long>, name: String): Result<DocumentEntity> =
        withContext(Dispatchers.IO) {
            try {
                val docs = documentDao.getByIds(ids)
                if (docs.size < 2) {
                    return@withContext Result.failure(RuntimeException("Select at least two documents to merge."))
                }
                val safeName = name.trim().takeIf { it.isNotEmpty() } ?: "Merged-${System.currentTimeMillis()}"
                val fileName = if (safeName.endsWith(".pdf", ignoreCase = true)) safeName else "$safeName.pdf"
                val target = File(documentsDir, "${System.currentTimeMillis()}_$fileName")
                val ok = PdfUtils.mergePdfs(appContext, docs.map { File(it.filePath) }, target)
                if (!ok) return@withContext Result.failure(RuntimeException("Merging failed."))
                val entity = DocumentEntity(
                    fileName = fileName,
                    filePath = target.absolutePath,
                    sizeBytes = target.length(),
                    dateAdded = System.currentTimeMillis(),
                    pageCount = PdfUtils.getPageCount(target)
                )
                val id = documentDao.insert(entity)
                Result.success(entity.copy(id = id))
            } catch (e: Exception) {
                Result.failure(RuntimeException("Merging failed."))
            }
        }

    suspend fun deleteDocument(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        File(doc.filePath).delete()
        documentDao.delete(doc)
    }

    /** Splits a document: pages from to to (from 1) go into a new document. */
    suspend fun splitDocument(id: Long, fromPage: Int, toPage: Int): Result<DocumentEntity> =
        withContext(Dispatchers.IO) {
            try {
                val doc = documentDao.getById(id)
                    ?: return@withContext Result.failure(RuntimeException("Document not found."))
                val baseName = doc.fileName.removeSuffix(".pdf")
                val fileName = "$baseName-pages-$fromPage-to-$toPage.pdf"
                val target = File(documentsDir, "${System.currentTimeMillis()}_$fileName")
                val ok = PdfUtils.splitPdf(File(doc.filePath), fromPage, toPage, target)
                if (!ok) return@withContext Result.failure(RuntimeException("Splitting failed."))
                val entity = DocumentEntity(
                    fileName = fileName,
                    filePath = target.absolutePath,
                    sizeBytes = target.length(),
                    dateAdded = System.currentTimeMillis(),
                    pageCount = PdfUtils.getPageCount(target),
                    folderId = doc.folderId
                )
                val newId = documentDao.insert(entity)
                Result.success(entity.copy(id = newId))
            } catch (e: Exception) {
                Result.failure(RuntimeException("Splitting failed."))
            }
        }

    /** Converts all pages of a document to PNG images in the output folder. */
    suspend fun exportImages(id: Long): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val doc = documentDao.getById(id)
                ?: return@withContext Result.failure(RuntimeException("Document not found."))
            val outDir = File(appContext.filesDir, "exports").apply { mkdirs() }
            val files = PdfUtils.exportPagesToImages(File(doc.filePath), outDir)
            if (files.isEmpty()) {
                Result.failure(RuntimeException("Converting to images failed."))
            } else {
                Result.success(files)
            }
        } catch (e: Exception) {
            Result.failure(RuntimeException("Converting to images failed."))
        }
    }

    /** Locks a document with a password — a new locked copy is created; the original is untouched. */
    suspend fun lockDocument(id: Long, password: String): Result<DocumentEntity> =
        withContext(Dispatchers.IO) {
            try {
                val doc = documentDao.getById(id)
                    ?: return@withContext Result.failure(RuntimeException("Document not found."))
                if (password.length < 4) {
                    return@withContext Result.failure(RuntimeException("The password must be at least 4 characters."))
                }
                val baseName = doc.fileName.removeSuffix(".pdf")
                val fileName = "$baseName-locked.pdf"
                val target = File(documentsDir, "${System.currentTimeMillis()}_$fileName")
                val ok = PdfUtils.lockPdf(File(doc.filePath), password, target)
                if (!ok) return@withContext Result.failure(RuntimeException("Locking failed."))
                val entity = DocumentEntity(
                    fileName = fileName,
                    filePath = target.absolutePath,
                    sizeBytes = target.length(),
                    dateAdded = System.currentTimeMillis(),
                    pageCount = PdfUtils.getPageCount(target),
                    folderId = doc.folderId
                )
                val newId = documentDao.insert(entity)
                Result.success(entity.copy(id = newId))
            } catch (e: Exception) {
                Result.failure(RuntimeException("Locking failed."))
            }
        }

    /**
     * Saves an annotated page: all pages are rendered, the target page is
     * flattened with its annotations, and a new document is created (max 50 pages).
     */
    suspend fun saveAnnotatedPage(
        id: Long,
        pageIndex: Int,
        strokes: List<com.scanpilot.app.util.AnnotationStroke>
    ): Result<DocumentEntity> = withContext(Dispatchers.IO) {
        try {
            val doc = documentDao.getById(id)
                ?: return@withContext Result.failure(RuntimeException("Document not found."))
            if (doc.pageCount > 50) {
                return@withContext Result.failure(
                    RuntimeException("This document is too large; annotations are supported for documents up to 50 pages.")
                )
            }
            val source = File(doc.filePath)
            val bitmaps = mutableListOf<android.graphics.Bitmap>()
            for (i in 0 until doc.pageCount) {
                val bitmap = PdfUtils.renderPage(source, i, 1240)
                    ?: return@withContext Result.failure(RuntimeException("Reading the page failed."))
                bitmaps.add(if (i == pageIndex) PdfUtils.drawStrokesOnBitmap(bitmap, strokes) else bitmap)
            }
            val baseName = doc.fileName.removeSuffix(".pdf")
            val fileName = "$baseName-annotated.pdf"
            val target = File(documentsDir, "${System.currentTimeMillis()}_$fileName")
            val ok = PdfUtils.createPdfFromBitmaps(bitmaps, target)
            bitmaps.forEach { it.recycle() }
            if (!ok) return@withContext Result.failure(RuntimeException("Saving the annotations failed."))
            val entity = DocumentEntity(
                fileName = fileName,
                filePath = target.absolutePath,
                sizeBytes = target.length(),
                dateAdded = System.currentTimeMillis(),
                pageCount = PdfUtils.getPageCount(target),
                folderId = doc.folderId
            )
            val newId = documentDao.insert(entity)
            Result.success(entity.copy(id = newId))
        } catch (e: Exception) {
            Result.failure(RuntimeException("Saving the annotations failed."))
        }
    }

    suspend fun renameDocument(id: Long, newName: String) {
        val name = newName.trim().takeIf { it.isNotEmpty() } ?: return
        val finalName = if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf"
        documentDao.rename(id, finalName)
    }

    suspend fun toggleFavorite(doc: DocumentEntity) {
        documentDao.setFavorite(doc.id, !doc.isFavorite)
    }

    suspend fun moveToFolder(id: Long, folderId: Long?) {
        documentDao.setFolder(id, folderId)
    }

    suspend fun createFolder(name: String): Long {
        val clean = name.trim()
        if (clean.isEmpty()) return -1
        return folderDao.insert(FolderEntity(name = clean, createdAt = System.currentTimeMillis()))
    }

    suspend fun deleteFolder(folder: FolderEntity) {
        folderDao.delete(folder)
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
