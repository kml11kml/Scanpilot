package com.scanpilot.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE (:query = '' OR fileName LIKE '%' || :query || '%') ORDER BY dateAdded DESC")
    fun observeByDate(query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE (:query = '' OR fileName LIKE '%' || :query || '%') ORDER BY fileName ASC")
    fun observeByName(query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE (:query = '' OR fileName LIKE '%' || :query || '%') ORDER BY sizeBytes DESC")
    fun observeBySize(query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doc: DocumentEntity): Long

    @Update
    suspend fun update(doc: DocumentEntity)

    @Delete
    suspend fun delete(doc: DocumentEntity)

    @Query("UPDATE documents SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("UPDATE documents SET folderId = :folderId WHERE id = :id")
    suspend fun setFolder(id: Long, folderId: Long?)

    @Query("UPDATE documents SET fileName = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)
}

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: Long): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity): Long

    @Update
    suspend fun update(folder: FolderEntity)

    @Delete
    suspend fun delete(folder: FolderEntity)
}
