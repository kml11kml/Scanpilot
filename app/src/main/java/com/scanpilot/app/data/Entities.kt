package com.scanpilot.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A PDF document in the library.
 * The PDF file is stored in the app's private storage (filePath), not
 * just as an external reference, so the document is not lost after the
 * original file is deleted or access to it is revoked.
 */
@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("folderId")]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val dateAdded: Long,
    val pageCount: Int,
    val folderId: Long? = null,
    val isFavorite: Boolean = false
)

/** A folder for organizing documents (e.g. "Notes", "Contracts", "Bills") */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)
